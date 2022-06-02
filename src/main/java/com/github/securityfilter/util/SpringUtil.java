package com.github.securityfilter.util;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public class SpringUtil {

    public static HttpServletRequest getCurrentRequest() {
        HttpServletRequest request;
        try {
            RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
            if (requestAttributes instanceof ServletRequestAttributes) {
                request = ((ServletRequestAttributes) requestAttributes).getRequest();
            } else {
                request = null;
            }
        } catch (Exception e) {
            request = null;
        }

        //验证请求
        try {
            if (request != null) {
                request.getMethod();
            }
        } catch (Exception e) {
            request = null;
        }
        return request;
    }

}
