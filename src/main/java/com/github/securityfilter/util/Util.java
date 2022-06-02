package com.github.securityfilter.util;

public class Util {
    public static final boolean EXIST_SPRING_WEB;

    static {
        boolean existSpringWeb;
        try {
            Class.forName("org.springframework.web.context.request.RequestContextHolder");
            existSpringWeb = true;
        } catch (Throwable e) {
            existSpringWeb = false;
        }
        EXIST_SPRING_WEB = existSpringWeb;
    }
}
