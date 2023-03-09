package com.github.securityfilter.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class PlatformDependentUtil {
    public static final boolean EXIST_SPRING_WEB;
    public static final boolean EXIST_HTTP_SERVLET;
    public static final boolean EXIST_DUBBO_APACHE;
    public static final boolean EXIST_DUBBO_ALIBABA;

    public static final Constructor JACKSON_OBJECT_MAPPER_CONSTRUCTOR;
    public static final Method JACKSON_WRITE_VALUE_AS_BYTES_METHOD;
    public static final Method FASTJSON_TO_JSON_STRING_METHOD;

    static {
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

}
