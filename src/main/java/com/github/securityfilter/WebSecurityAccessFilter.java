package com.github.securityfilter;

import com.github.securityfilter.util.BeanMap;
import com.github.securityfilter.util.SpringUtil;
import com.github.securityfilter.util.Util;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * 验证用户身份 (web端)
 * 拦截接口, 放入用户信息
 *
 * @author wangzihao
 */
public class WebSecurityAccessFilter<USER_ID, ACCESS_USER> implements Filter {
    public static final String REQUEST_ATTR_NAME = "user";
    public static final String DEFAULT_ACCESS_TOKEN_PARAMETER_NAME = "access_token";
    public static final Object NULL = new Object();
    /**
     * 跨线程传递当前RPC请求的用户
     */
    private static final ThreadLocal<Supplier<Object>> ACCESS_USER_THREAD_LOCAL = new ThreadLocal<>();
    private static final ThreadLocal<HttpServletRequest> REQUEST_THREAD_LOCAL = new ThreadLocal<>();
    private static final Constructor JACKSON_OBJECT_MAPPER_CONSTRUCTOR;
    private static final Method JACKSON_WRITE_VALUE_AS_BYTES_METHOD;
    private static WebSecurityAccessFilter INSTANCE;

    static {
        Constructor<?> jacksonObjectMapperConstructor;
        Method writeValueAsBytesMethod;
        try {
            Class<?> objectMapperClass = Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            jacksonObjectMapperConstructor = objectMapperClass.getConstructor();
            writeValueAsBytesMethod = objectMapperClass.getMethod("writeValueAsBytes", Object.class);
        } catch (Exception e) {
            jacksonObjectMapperConstructor = null;
            writeValueAsBytesMethod = null;
        }
        JACKSON_OBJECT_MAPPER_CONSTRUCTOR = jacksonObjectMapperConstructor;
        JACKSON_WRITE_VALUE_AS_BYTES_METHOD = writeValueAsBytesMethod;
    }

    private final Set<String> accessTokenParameterNames = new LinkedHashSet<>();
    private Object jacksonObjectMapper;

    public WebSecurityAccessFilter() {
        this(Collections.singletonList(DEFAULT_ACCESS_TOKEN_PARAMETER_NAME));
    }

    public WebSecurityAccessFilter(Collection<String> accessTokenKeys) {
        this.accessTokenParameterNames.addAll(accessTokenKeys);
        INSTANCE = this;
    }

    public static <ACCESS_USER> ACCESS_USER getCurrentAccessUserIfNew(HttpServletRequest request) {
        if (request == null) {
            request = getCurrentRequest();
        }
        Object user = WebSecurityAccessFilter.getCurrentAccessUser(request);
        if (user == null && INSTANCE != null) {
            INSTANCE.initAccessUser(request);
        }
        return WebSecurityAccessFilter.getCurrentAccessUser(request);
    }

    public static <ACCESS_USER> ACCESS_USER getCurrentAccessUserIfNew() {
        return WebSecurityAccessFilter.getCurrentAccessUserIfNew(null);
    }

    public static <ACCESS_USER> ACCESS_USER getCurrentAccessUser(HttpServletRequest request) {
        ACCESS_USER accessUser = null;
        if (request == null) {
            request = getCurrentRequest();
        }
        if (request != null) {
            accessUser = (ACCESS_USER) request.getAttribute(REQUEST_ATTR_NAME);
        }
        if (accessUser == null || accessUser == NULL) {
            Supplier<Object> accessUserSupplier = ACCESS_USER_THREAD_LOCAL.get();
            if (accessUserSupplier != null) {
                accessUser = (ACCESS_USER) accessUserSupplier.get();
            }
        }
        return accessUser == NULL ? null : accessUser;
    }

    public static <ACCESS_USER> ACCESS_USER getCurrentAccessUser() {
        return getCurrentAccessUser(null);
    }

    public static <T> void setCurrentUser(T accessUser) {
        if (accessUser == null) {
            ACCESS_USER_THREAD_LOCAL.remove();
        } else {
            ACCESS_USER_THREAD_LOCAL.set(() -> accessUser);
        }
    }

    public static String[] getAccessTokens(HttpServletRequest request, Collection<String> accessTokenParameterNames) {
        Supplier<Object> supplier = ACCESS_USER_THREAD_LOCAL.get();
        if (supplier != null) {
            Object accessUser = supplier.get();
            if (accessUser != null && accessUser != NULL) {
                Map accessUserGetterMap = BeanMap.toMap(accessUser);
                for (String accessTokenParameterName : accessTokenParameterNames) {
                    Object accessToken = accessUserGetterMap.get(accessTokenParameterName);
                    if (accessToken != null) {
                        return new String[]{accessToken.toString()};
                    }
                }
                Object accessToken = accessUserGetterMap.get(DEFAULT_ACCESS_TOKEN_PARAMETER_NAME);
                if (accessToken != null) {
                    return new String[]{accessToken.toString()};
                }
            }
        }
        if (request == null) {
            request = getCurrentRequest();
            if (request == null) {
                return new String[0];
            }
        }
        Set<String> result = new LinkedHashSet<>();
        for (String parameterName : accessTokenParameterNames) {
            String accessToken = (String) request.getAttribute(parameterName);
            if (accessToken != null && !accessToken.isEmpty()) {
                result.add(accessToken);
            }

            accessToken = request.getParameter(parameterName);
            if (accessToken != null && !accessToken.isEmpty()) {
                result.add(accessToken);
            }

            String headerName = toHeaderName(parameterName);
            accessToken = request.getHeader(headerName);
            if (accessToken != null && !accessToken.isEmpty()) {
                result.add(accessToken);
            }

            accessToken = getCookieValue(request.getCookies(), parameterName);
            if (accessToken != null && !accessToken.isEmpty()) {
                result.add(accessToken);
            }
        }
        return result.toArray(new String[0]);
    }

    public static <ACCESS_USER> void runOnCurrentUser(ACCESS_USER accessUser, Runnable runnable) {
        Object old = getCurrentAccessUser();
        try {
            setCurrentUser(accessUser);
            runnable.run();
        } finally {
            setCurrentUser(old);
        }
    }

    public static <ACCESS_USER, RESULT> RESULT runOnCurrentUser(ACCESS_USER accessUser, Callable<RESULT> callable) throws Exception {
        Object old = getCurrentAccessUser();
        try {
            setCurrentUser(accessUser);
            return callable.call();
        } finally {
            setCurrentUser(old);
        }
    }

    private static String getCookieValue(Cookie[] cookies, String name) {
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (Objects.equals(name, cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static String toHeaderName(String name) {
        return name.replace("_", "-");
    }

    public static HttpServletRequest getCurrentRequest() {
        HttpServletRequest request = REQUEST_THREAD_LOCAL.get();
        if (request == null && Util.EXIST_SPRING_WEB) {
            request = getCurrentRequestSpring();
        }
        return request;
    }

    public static HttpServletRequest getCurrentRequestSpring() {
        return SpringUtil.getCurrentRequest();
    }

    public Set<String> getAccessTokenParameterNames() {
        return accessTokenParameterNames;
    }

    protected boolean isAccessSuccess(ACCESS_USER accessUser) {
        return true;
    }

    protected USER_ID selectUserId(HttpServletRequest request, String accessToken) {
        return null;
    }

    protected ACCESS_USER selectUser(HttpServletRequest request, USER_ID userId, String accessToken) {
        return null;
    }

    protected void onAccessSuccess(HttpServletRequest request, HttpServletResponse response, FilterChain chain, ACCESS_USER accessUser) throws IOException, ServletException {
        chain.doFilter(request, response);
    }

    protected void onAccessFail(HttpServletRequest request, HttpServletResponse response, FilterChain chain, ACCESS_USER accessUser) throws IOException, ServletException {
        for (String accessTokenParameterName : accessTokenParameterNames) {
            response.addHeader("Set-Cookie", accessTokenParameterName + "=; Max-Age=0; Path=/");
        }
        if (accessUser == null) {
            writeToBody(response, "{\"message\":\"用户未登录\",\"success\":false,\"code\":401,\"status\":2}");
        } else {
            writeToBody(response, "{\"message\":\"账号禁止登录\",\"success\":false,\"code\":401,\"status\":3}");
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        REQUEST_THREAD_LOCAL.set(request);
        try {
            // 用户不存在
            ACCESS_USER accessUser = initAccessUser(request);
            if (accessUser == null) {
                onAccessFail(request, response, chain, null);
            } else {
                try {
                    if (isAccessSuccess(accessUser)) {
                        onAccessSuccess(request, response, chain, accessUser);
                    } else {
                        onAccessFail(request, response, chain, accessUser);
                    }
                } finally {
                    setCurrentUser(null);
                }
            }
        } finally {
            REQUEST_THREAD_LOCAL.remove();
        }
    }

    protected String[] getAccessTokens(HttpServletRequest request) {
        return getAccessTokens(request, accessTokenParameterNames);
    }

    protected ACCESS_USER initAccessUser(HttpServletRequest request) {
        Supplier<Object> supplier = ACCESS_USER_THREAD_LOCAL.get();
        ACCESS_USER accessUser = null;
        if (supplier != null) {
            accessUser = (ACCESS_USER) supplier.get();
        }
        if (accessUser == NULL) {
            return null;
        }
        if (accessUser != null) {
            return accessUser;
        }
        if (request == null) {
            request = getCurrentRequest();
        }
        if (request == null) {
            return null;
        }
        accessUser = (ACCESS_USER) request.getAttribute(REQUEST_ATTR_NAME);
        if (accessUser == NULL) {
            return null;
        }
        if (accessUser != null) {
            return accessUser;
        }
        String[] accessTokens = getAccessTokens(request);
        setCurrentUser(NULL);
        try {
            for (String accessToken : accessTokens) {
                if (accessToken == null || accessToken.isEmpty()) {
                    continue;
                }

                USER_ID userId = selectUserId(request, accessToken);
                if (userId == null) {
                    continue;
                }

                accessUser = selectUser(request, userId, accessToken);
                if (accessUser == null) {
                    continue;
                }
                request.setAttribute(REQUEST_ATTR_NAME, accessUser);
                break;
            }
        } finally {
            setCurrentUser(null);
        }
        return accessUser;
    }

    protected void writeToBody(HttpServletResponse response, Object data) throws IOException {
        response.setHeader("content-type", "application/json;charset=UTF-8");
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            byte[] bytes = toByte(data);
            outputStream.write(bytes);
            outputStream.flush();
        }
    }

    public byte[] toByte(Object body) {
        if (body instanceof byte[]) {
            return (byte[]) body;
        } else if (body instanceof String) {
            return ((String) body).getBytes(StandardCharsets.UTF_8);
        } else if (body == null) {
            return "{}".getBytes(StandardCharsets.UTF_8);
        } else {
            return toJsonBytes(body);
        }
    }

    public <T> T getJacksonObjectMapper() {
        return (T) jacksonObjectMapper;
    }

    public void setJacksonObjectMapper(Object jacksonObjectMapper) {
        this.jacksonObjectMapper = jacksonObjectMapper;
    }

    public byte[] toJsonBytes(Object body) {
        if (JACKSON_WRITE_VALUE_AS_BYTES_METHOD != null && jacksonObjectMapper == null) {
            try {
                jacksonObjectMapper = JACKSON_OBJECT_MAPPER_CONSTRUCTOR.newInstance();
            } catch (Exception ignored) {
            }
        }

        byte[] bytes = null;
        if (jacksonObjectMapper != null && JACKSON_WRITE_VALUE_AS_BYTES_METHOD != null) {
            try {
                bytes = (byte[]) JACKSON_WRITE_VALUE_AS_BYTES_METHOD.invoke(jacksonObjectMapper, body);
            } catch (Exception ignored) {
            }
        }
        if (bytes == null) {
            throw new IllegalStateException("no support json serialization. need user impl method toJsonBytes(body)");
        }
        return bytes;
    }

}