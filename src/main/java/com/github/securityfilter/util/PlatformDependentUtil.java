package com.github.securityfilter.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class PlatformDependentUtil {
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
                return (String) MDC_GET_METHOD.invoke(null, key);
            } catch (IllegalAccessException | InvocationTargetException e) {
                sneakyThrows(e);
            }
        }
        return null;
    }

    public static void mdcPut(String key, String value) {
        if (MDC_PUT_METHOD != null) {
            try {
                MDC_PUT_METHOD.invoke(null, key, value);
            } catch (IllegalAccessException | InvocationTargetException e) {
                sneakyThrows(e);
            }
        }
    }

    public static void mdcRemove(String key) {
        if (MDC_REMOVE_METHOD != null) {
            try {
                MDC_REMOVE_METHOD.invoke(null, key);
            } catch (IllegalAccessException | InvocationTargetException e) {
                sneakyThrows(e);
            }
        }
    }

    public static <R> R runOnMDC(String mdcName, String mdcValue, Callable<R> runnable) {
        String oldMdcValue = PlatformDependentUtil.mdcGet(mdcName);
        try {
            PlatformDependentUtil.mdcPut(mdcName, mdcValue);
            return runnable.call();
        } catch (Throwable e) {
            PlatformDependentUtil.sneakyThrows(e);
            return null;
        } finally {
            PlatformDependentUtil.mdcRemove(mdcName);
            if (oldMdcValue != null) {
                PlatformDependentUtil.mdcPut(mdcName, oldMdcValue);
            }
        }
    }

    public static void runOnMDC(String mdcName, String mdcValue, AccessUserUtil.Runnable runnable) {
        String oldMdcValue = PlatformDependentUtil.mdcGet(mdcName);
        try {
            PlatformDependentUtil.mdcPut(mdcName, mdcValue);
            runnable.run();
        } catch (Throwable e) {
            PlatformDependentUtil.sneakyThrows(e);
        } finally {
            PlatformDependentUtil.mdcRemove(mdcName);
            if (oldMdcValue != null) {
                PlatformDependentUtil.mdcPut(mdcName, oldMdcValue);
            }
        }
    }
}
