package com.github.securityfilter;

import com.github.securityfilter.util.*;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
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
    public static final String REQUEST_ATTR_NAME =
            System.getProperty("WebSecurityAccessFilter.REQUEST_ATTR_NAME", "user");
    public static final String DEFAULT_ACCESS_TOKEN_PARAMETER_NAME =
            System.getProperty("WebSecurityAccessFilter.DEFAULT_ACCESS_TOKEN_PARAMETER_NAME", "access_token");
    public static final String REQUEST_ATTR_ONCE_FILTER_NAME =
            System.getProperty("WebSecurityAccessFilter.REQUEST_ATTR_ONCE_FILTER_NAME", "com.github.securityfilter.WebSecurityAccessFilter#REQUEST_ATTR_ONCE_FILTER");
    /**
     * 防止嵌套调用
     */
    public static final Object NULL = AccessUserUtil.NULL;
    private static final ThreadLocal<HttpServletRequest> REQUEST_THREAD_LOCAL = new ThreadLocal<>();
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static WebSecurityAccessFilter INSTANCE;
    /**
     * 跨线程传递当前RPC请求的用户
     */
    private static final ThreadLocal<Supplier<Object>> ACCESS_USER_THREAD_LOCAL = new ThreadLocal<>();

    private final Set<String> accessTokenParameterNames = new LinkedHashSet<>(3);
    private final Set<String> excludeUriPatterns = new LinkedHashSet<>(3);
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private Object jacksonObjectMapper;

    public WebSecurityAccessFilter() {
        this(Collections.singletonList(DEFAULT_ACCESS_TOKEN_PARAMETER_NAME));
    }

    public WebSecurityAccessFilter(Collection<String> accessTokenKeys) {
        this.accessTokenParameterNames.addAll(accessTokenKeys);
        this.pathMatcher.setCachePatterns(true);
        INSTANCE = this;
    }

    public static String getAccessTokenParameterName() {
        WebSecurityAccessFilter<?, ?> instance = INSTANCE;
        if (instance != null) {
            Set<String> accessTokenParameterNames = instance.getAccessTokenParameterNames();
            if (!accessTokenParameterNames.isEmpty()) {
                return accessTokenParameterNames.iterator().next();
            }
        }
        return DEFAULT_ACCESS_TOKEN_PARAMETER_NAME;
    }

    public static Set<String> getAccessTokenParameterNameSet() {
        WebSecurityAccessFilter<?, ?> instance = INSTANCE;
        if (instance != null) {
            return instance.getAccessTokenParameterNames();
        } else {
            return Collections.singleton(DEFAULT_ACCESS_TOKEN_PARAMETER_NAME);
        }
    }

    public static String[] getAccessTokens() {
        WebSecurityAccessFilter instance = INSTANCE;
        String[] accessTokens;
        if (instance != null) {
            accessTokens = instance.getAccessTokens(null);
        } else {
            accessTokens = getAccessTokens(null, Collections.emptyList());
        }
        return accessTokens;
    }

    public static String getAccessToken() {
        String[] accessTokens = getAccessTokens();
        return accessTokens == null || accessTokens.length == 0 ? null : accessTokens[0];
    }

    public static boolean isInLifecycle() {
        // 考虑DispatcherType
        // REQUEST_THREAD_LOCAL仅在当前 DispatcherType是自己时有值
        // SpringServletRequest在所有情况下有值
        return getCurrentRequest() != null;
    }

    public static <ACCESS_USER> ACCESS_USER getCurrentAccessUserIfCreate(HttpServletRequest request, WebSecurityAccessFilter instance) {
        if (request == null) {
            request = getCurrentRequest();
        }
        ACCESS_USER user = getCurrentAccessUserIfExist(request);
        if (user == NULL) {
            return null;
        } else if (user == null && instance != null) {
            user = createAccessUser(request, instance, true);
            return user;
        } else {
            return user;
        }
    }

    public static <ACCESS_USER> ACCESS_USER createAccessUser() {
        return createAccessUser(null, INSTANCE, true);
    }

    public static <ACCESS_USER> ACCESS_USER createAccessUser(HttpServletRequest request, WebSecurityAccessFilter instance, boolean cache) {
        if (instance == null) {
            return null;
        }
        if (request == null) {
            request = getCurrentRequest();
        } else {
            try {
                //验证 spring-ThreadLocal请求
                request.getMethod();
            } catch (Exception e) {
                return null;
            }
        }
        if (request == null) {
            return null;
        }
        Object accessUser;
        if (PlatformDependentUtil.EXIST_DUBBO_APACHE
                && DubboAccessUserUtil.isApacheNestingRequest()) {
            HttpServletRequest finalRequest = request;
            accessUser = DubboAccessUserUtil.apacheNestingRequest(() -> instance.initAccessUser(finalRequest));
        } else {
            accessUser = instance.initAccessUser(request);
        }
        if (cache) {
            request.setAttribute(REQUEST_ATTR_NAME, accessUser == null ? NULL : accessUser);
        }
        return (ACCESS_USER) accessUser;
    }

    public static <ACCESS_USER> ACCESS_USER getCurrentAccessUserIfCreate(HttpServletRequest request) {
        return getCurrentAccessUserIfCreate(request, INSTANCE);
    }

    public static <ACCESS_USER> ACCESS_USER getCurrentAccessUserIfCreate() {
        return getCurrentAccessUserIfCreate(null, INSTANCE);
    }

    public static <ACCESS_USER> ACCESS_USER getCurrentAccessUser(HttpServletRequest request) {
        ACCESS_USER accessUser = getCurrentAccessUserIfExist(request);
        return accessUser == NULL ? null : accessUser;
    }

    public static <ACCESS_USER> ACCESS_USER getCurrentAccessUserIfExist() {
        return getCurrentAccessUserIfExist(null);
    }

    public static <ACCESS_USER> ACCESS_USER getCurrentAccessUserExist(HttpServletRequest request) {
        return getCurrentAccessUserIfExist(request);
    }

    public static <ACCESS_USER> ACCESS_USER getCurrentAccessUserIfExist(HttpServletRequest request) {
        Object accessUser = null;
        if (request == null) {
            request = getCurrentRequest();
        }
        if (request != null) {
            accessUser = request.getAttribute(REQUEST_ATTR_NAME);
        }
        if (accessUser == null) {
            Supplier<Object> threadAccessUserSupplier = ACCESS_USER_THREAD_LOCAL.get();
            accessUser = threadAccessUserSupplier != null ? threadAccessUserSupplier.get() : null;
        }
        return (ACCESS_USER) accessUser;
    }

    public static <ACCESS_USER> ACCESS_USER getCurrentAccessUser() {
        return getCurrentAccessUser(null);
    }

    public static void removeCurrentUser() {
        HttpServletRequest request = getCurrentRequest();
        removeCurrentUser(request);
    }

    public static void removeCurrentUser(HttpServletRequest request) {
        ACCESS_USER_THREAD_LOCAL.remove();
        if (request != null) {
            request.removeAttribute(REQUEST_ATTR_NAME);
        }
    }

    public static <T> void setCurrentUser(HttpServletRequest request, T accessUser) {
        if (accessUser == null) {
            accessUser = (T) NULL;
        }
        T finalAccessUser = accessUser;
        ACCESS_USER_THREAD_LOCAL.set(() -> finalAccessUser);
        if (request != null) {
            request.setAttribute(REQUEST_ATTR_NAME, accessUser);
        }
    }

    public static <T> void setCurrentUserRequestAttribute(HttpServletRequest request, T accessUser) {
        if (accessUser == null) {
            accessUser = (T) NULL;
        }
        if (request != null) {
            request.setAttribute(REQUEST_ATTR_NAME, accessUser);
        }
    }

    public static <T> void setCurrentUser(T accessUser) {
        HttpServletRequest request = getCurrentRequest();
        setCurrentUser(request, accessUser);
    }

    public static String[] getAccessTokens(HttpServletRequest request, Collection<String> accessTokenParameterNames) {
        Supplier<Object> accessUserSupplier = ACCESS_USER_THREAD_LOCAL.get();
        Object accessUser = accessUserSupplier != null ? accessUserSupplier.get() : null;
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
        if (request == null) {
            request = getCurrentRequest();
            if (request == null) {
                return new String[0];
            }
        }
        Set<String> result = new LinkedHashSet<>(2);
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

    public static <ACCESS_USER, RESULT> RESULT runOnCurrentUser(ACCESS_USER accessUser, Callable<RESULT> callable) {
        Object old = getCurrentAccessUser();
        try {
            setCurrentUser(accessUser);
            return callable.call();
        } catch (Exception e) {
            PlatformDependentUtil.sneakyThrows(e);
            return null;
        } finally {
            setCurrentUser(old);
        }
    }

    public static boolean setCurrentAccessUserValue(String attrName, Object value) {
        Object accessUserIfExist = getCurrentAccessUserIfExist(null);
        Map<String, Object> map = new LinkedHashMap<>(2);
        if (accessUserIfExist == null || accessUserIfExist == NULL) {
            map.put(attrName, value);
            setCurrentUser(map);
            return true;
        } else {
            return TypeUtil.invokeSetter(accessUserIfExist, attrName, value);
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
        if (request == null && PlatformDependentUtil.EXIST_SPRING_WEB) {
            request = getCurrentRequestSpring();
        }
        return request;
    }

    public static HttpServletRequest getCurrentRequestSpring() {
        return SpringUtil.getCurrentRequest();
    }

    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        Set<String> excludeUriPatterns = getExcludeUriPatterns();
        if (excludeUriPatterns == null || excludeUriPatterns.isEmpty()) {
            return false;
        }

        String contextPath = request.getContextPath();
        if (contextPath == null) {
            contextPath = "";
        }
        if ("/".equals(contextPath)) {
            contextPath = "";
        }
        String requestUri = Objects.toString(request.getRequestURI(), "");
        for (String excludeUriPattern : excludeUriPatterns) {
            String pattern;
            if (excludeUriPattern.startsWith("/")) {
                pattern = excludeUriPattern;
            } else {
                pattern = contextPath + "/" + excludeUriPattern;
            }
            if (pathMatcher.match(pattern, requestUri, "*")) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getExcludeUriPatterns() {
        return excludeUriPatterns;
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

    protected void onAccessFail(HttpServletRequest request, HttpServletResponse response, FilterChain chain, ACCESS_USER accessUser) throws IOException {
        for (String accessTokenParameterName : accessTokenParameterNames) {
            response.addHeader("Set-Cookie", accessTokenParameterName + "=; Max-Age=0; Path=/");
        }
        if (accessUser == null) {
            writeToBody(response, "{\"message\":\"用户未登录\",\"success\":false,\"code\":401,\"status\":2}");
        } else {
            writeToBody(response, "{\"message\":\"账号禁止登录\",\"success\":false,\"code\":401,\"status\":3}");
        }
    }

    protected void onNotFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        chain.doFilter(request, response);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        Object once = servletRequest.getAttribute(REQUEST_ATTR_ONCE_FILTER_NAME);
        if (once == null) {
            servletRequest.setAttribute(REQUEST_ATTR_ONCE_FILTER_NAME, Boolean.TRUE);

            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            try {
                REQUEST_THREAD_LOCAL.set(request);
                removeCurrentUser(request);
                if (shouldNotFilter(request)) {
                    onNotFilter(request, response, chain);
                } else {
                    // 用户不存在
                    ACCESS_USER accessUser = initAccessUser(request);
                    if (accessUser == null) {
                        onAccessFail(request, response, chain, null);
                    } else {
                        request.setAttribute(REQUEST_ATTR_NAME, accessUser);
                        if (isAccessSuccess(accessUser)) {
                            onAccessSuccess(request, response, chain, accessUser);
                        } else {
                            onAccessFail(request, response, chain, accessUser);
                        }
                    }
                }
            } finally {
                ACCESS_USER_THREAD_LOCAL.remove();
                REQUEST_THREAD_LOCAL.remove();
            }
        } else {
            chain.doFilter(servletRequest, servletResponse);
        }
    }

    protected String[] getAccessTokens(HttpServletRequest request) {
        return getAccessTokens(request, accessTokenParameterNames);
    }

    protected ACCESS_USER initAccessUser(HttpServletRequest request) {
        if (request == null) {
            request = getCurrentRequest();
        }
        if (request == null) {
            return null;
        }
        ACCESS_USER accessUser = (ACCESS_USER) request.getAttribute(REQUEST_ATTR_NAME);
        if (accessUser == NULL) {
            return null;
        }
        if (accessUser != null) {
            return accessUser;
        }
        String[] accessTokens = getAccessTokens(request);
        setCurrentUser(request, NULL);
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
                if (accessUser != null) {
                    break;
                }
            }
        } finally {
            removeCurrentUser(request);
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
            return ((String) body).getBytes(UTF_8);
        } else if (body == null) {
            return "{}".getBytes(UTF_8);
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
        if (PlatformDependentUtil.JACKSON_WRITE_VALUE_AS_BYTES_METHOD != null && jacksonObjectMapper == null) {
            try {
                jacksonObjectMapper = PlatformDependentUtil.JACKSON_OBJECT_MAPPER_CONSTRUCTOR.newInstance();
            } catch (Exception ignored) {
            }
        }

        byte[] bytes = null;
        if (jacksonObjectMapper != null && PlatformDependentUtil.JACKSON_WRITE_VALUE_AS_BYTES_METHOD != null) {
            try {
                bytes = (byte[]) PlatformDependentUtil.JACKSON_WRITE_VALUE_AS_BYTES_METHOD.invoke(jacksonObjectMapper, body);
            } catch (Exception ignored) {
            }
        }

        if (bytes == null && PlatformDependentUtil.FASTJSON_TO_JSON_STRING_METHOD != null) {
            try {
                Object jsonString = PlatformDependentUtil.FASTJSON_TO_JSON_STRING_METHOD.invoke(null, body);
                if (jsonString instanceof String) {
                    bytes = ((String) jsonString).getBytes(UTF_8);
                }
            } catch (Exception ignored) {
            }
        }

        if (bytes == null) {
            throw new IllegalStateException("no support json serialization. need user impl method toJsonBytes(body)");
        }
        return bytes;
    }

}