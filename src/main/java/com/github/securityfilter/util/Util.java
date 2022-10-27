package com.github.securityfilter.util;

public class Util {
    public static final boolean EXIST_SPRING_WEB;
    public static final boolean EXIST_HTTP_SERVLET;
    public static final boolean EXIST_DUBBO_APACHE;
    public static final boolean EXIST_DUBBO_ALIBABA;

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
    }
}
