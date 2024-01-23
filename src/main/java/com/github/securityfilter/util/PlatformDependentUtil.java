package com.github.securityfilter.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class PlatformDependentUtil {
    /**
     * 跨线程传递当前RPC请求的用户
     */
    static final ThreadLocal<Supplier<Object>> ACCESS_USER_THREAD_LOCAL = new ThreadLocal<>();
    static final ThreadLocal<Map<String, Object>> MDC_THREAD_LOCAL = ThreadLocal.withInitial(() -> new HashMap<>(2));
    static final ThreadLocal<LinkedList<AccessUserSnapshot>> SNAPSHOT_THREAD_LOCAL = ThreadLocal.withInitial(LinkedList::new);

    public static final String ATTR_REQUEST_ID =
            System.getProperty("MDC.ATTR_REQUEST_ID", "requestId");
    public static final boolean EXIST_SPRING_WEB;
    public static final boolean EXIST_HTTP_SERVLET;
    public static final boolean EXIST_DUBBO_APACHE;
    public static final boolean EXIST_DUBBO_ALIBABA;
    public static final boolean EXIST_MDC;

    public static final Constructor JACKSON_OBJECT_MAPPER_CONSTRUCTOR;
    public static final Method JACKSON_WRITE_VALUE_AS_BYTES_METHOD;
    public static final Method FASTJSON_TO_JSON_STRING_METHOD;
    public static final Method MDC_GET_METHOD;
    public static final Method MDC_PUT_METHOD;
    public static final Method MDC_REMOVE_METHOD;


    static {
        Method mdcGetMethod;
        Method mdcPutMethod;
        Method mdcRemoveMethod;
        try {
            Class<?> mdc = Class.forName("org.slf4j.MDC");
            mdcGetMethod = mdc.getDeclaredMethod("get", String.class);
            mdcPutMethod = mdc.getDeclaredMethod("put", String.class, String.class);
            mdcRemoveMethod = mdc.getDeclaredMethod("remove", String.class);
        } catch (Throwable e) {
            mdcGetMethod = null;
            mdcPutMethod = null;
            mdcRemoveMethod = null;
        }
        MDC_GET_METHOD = mdcGetMethod;
        MDC_PUT_METHOD = mdcPutMethod;
        MDC_REMOVE_METHOD = mdcRemoveMethod;
        EXIST_MDC = mdcGetMethod != null;

        boolean existSpringWeb;
        try {
            Class.forName("org.springframework.web.context.request.RequestContextHolder");
            existSpringWeb = true;
        } catch (Throwable e) {
            existSpringWeb = false;
        }
        EXIST_SPRING_WEB = existSpringWeb;

        boolean existHttpServlet;
        try {
            Class.forName("javax.servlet.http.HttpServletRequest");
            existHttpServlet = true;
        } catch (Throwable e) {
            existHttpServlet = false;
        }
        EXIST_HTTP_SERVLET = existHttpServlet;

        boolean existDubboAlibaba;
        try {
            Class.forName("com.alibaba.dubbo.rpc.Filter");
            existDubboAlibaba = true;
        } catch (Throwable e) {
            existDubboAlibaba = false;
        }
        EXIST_DUBBO_ALIBABA = existDubboAlibaba;

        boolean existDubboApache;
        try {
            Class.forName("org.apache.dubbo.rpc.Filter");
            existDubboApache = true;
        } catch (Throwable e) {
            existDubboApache = false;
        }
        EXIST_DUBBO_APACHE = existDubboApache;

        Constructor<?> jacksonObjectMapperConstructor;
        Method writeValueAsBytesMethod;
        try {
            Class<?> objectMapperClass = Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            jacksonObjectMapperConstructor = objectMapperClass.getConstructor();
            writeValueAsBytesMethod = objectMapperClass.getMethod("writeValueAsBytes", Object.class);
        } catch (Throwable e) {
            jacksonObjectMapperConstructor = null;
            writeValueAsBytesMethod = null;
        }
        JACKSON_OBJECT_MAPPER_CONSTRUCTOR = jacksonObjectMapperConstructor;
        JACKSON_WRITE_VALUE_AS_BYTES_METHOD = writeValueAsBytesMethod;

        Method toJSONStringMethod;
        try {
            Class<?> fastjsonClass = Class.forName("com.alibaba.fastjson2.JSON");
            toJSONStringMethod = fastjsonClass.getDeclaredMethod("toJSONString", Object.class);
        } catch (Throwable e) {
            try {
                Class<?> fastjsonClass = Class.forName("com.alibaba.fastjson.JSON");
                toJSONStringMethod = fastjsonClass.getDeclaredMethod("toJSONString", Object.class);
            } catch (Throwable e1) {
                toJSONStringMethod = null;
            }
        }
        FASTJSON_TO_JSON_STRING_METHOD = toJSONStringMethod;
    }

    public static <E extends Throwable> void sneakyThrows(Throwable t) throws E {
        throw (E) t;
    }

    public static String mdcGet(String key) {
        if (MDC_GET_METHOD != null) {
            try {
                Object value = MDC_GET_METHOD.invoke(null, key);
                if (value == null) {
                    value = MDC_THREAD_LOCAL.get().get(key);
                }
                return value != null ? value.toString() : null;
            } catch (IllegalAccessException | InvocationTargetException e) {
                sneakyThrows(e);
            }
        }
        return null;
    }

    public static void mdcClose(String key, String value) {
        if (value == null) {
            mdcRemove(key);
        } else {
            mdcPut(key, value);
        }
    }

    public static void mdcPut(String key, String value) {
        MDC_THREAD_LOCAL.get().put(key, value);
        if (MDC_PUT_METHOD != null) {
            try {
                MDC_PUT_METHOD.invoke(null, key, value);
            } catch (IllegalAccessException | InvocationTargetException e) {
                sneakyThrows(e);
            }
        }
    }

    public static void mdcRemove(String key) {
        Map<String, Object> map = MDC_THREAD_LOCAL.get();
        if (!map.isEmpty()) {
            map.remove(key);
            if (map.isEmpty()) {
                MDC_THREAD_LOCAL.remove();
            }
        }

        if (MDC_REMOVE_METHOD != null) {
            try {
                MDC_REMOVE_METHOD.invoke(null, key);
            } catch (IllegalAccessException | InvocationTargetException e) {
                sneakyThrows(e);
            }
        }
    }

    public static <R> R runOnMDC(String mdcName, String mdcValue, Callable<R> runnable) {
        String oldMdcValue = mdcGet(mdcName);
        try {
            mdcPut(mdcName, mdcValue);
            return runnable.call();
        } catch (Throwable e) {
            sneakyThrows(e);
            return null;
        } finally {
            mdcRemove(mdcName);
            if (oldMdcValue != null) {
                mdcPut(mdcName, oldMdcValue);
            }
        }
    }

    public static void runOnMDC(String mdcName, String mdcValue, AccessUserUtil.Runnable runnable) {
        String oldMdcValue = mdcGet(mdcName);
        try {
            mdcPut(mdcName, mdcValue);
            runnable.run();
        } catch (Throwable e) {
            sneakyThrows(e);
        } finally {
            mdcRemove(mdcName);
            if (oldMdcValue != null) {
                mdcPut(mdcName, oldMdcValue);
            }
        }
    }
}
