package com.github.securityfilter;

import com.github.securityfilter.util.SnowflakeIdWorker;
import org.slf4j.MDC;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * 创建requestId
 *
 * @author hao
 */
public class WebRequestIdCreateFilter implements Filter {
    public static final String ATTR_REQUEST_ID =
            System.getProperty("WebRequestIdCreateFilter.ATTR_REQUEST_ID", "requestId");
    private static final SnowflakeIdWorker ID_WORKER = new SnowflakeIdWorker();
    private static final Supplier<String> REQUEST_ID_SUPPLIER = () -> String.valueOf(ID_WORKER.nextId());

    public static String getRequestId(ServletRequest request, boolean create) {
        if (request == null) {
            request = WebSecurityAccessFilter.getCurrentRequest();
            if (request == null) {
                if (create) {
                    return REQUEST_ID_SUPPLIER.get();
                } else {
                    return null;
                }
            }
        }
        String requestId;
        try {
            requestId = request.getParameter("_" + ATTR_REQUEST_ID);
            if (requestId == null) {
                requestId = (String) request.getAttribute(ATTR_REQUEST_ID);
            }
        } catch (Exception e) {
            requestId = null;
        }

        if (requestId == null) {
            requestId = MDC.get(ATTR_REQUEST_ID);
            if (requestId != null) {
                requestId = requestId.substring(ATTR_REQUEST_ID.length() + 1);
            }
        }
        if (requestId == null) {
            if (create) {
                requestId = REQUEST_ID_SUPPLIER.get();
                setRequestId(request, requestId);
            }
        }
        return requestId;
    }

    public static String getRequestId(ServletRequest request) {
        return getRequestId(request, false);
    }

    public static void setRequestId(ServletRequest request, String requestId) {
        if (requestId == null) {
            if (request != null) {
                try {
                    request.removeAttribute(ATTR_REQUEST_ID);
                } catch (Exception e) {
                }
            }
            MDC.remove(ATTR_REQUEST_ID);
        } else {
            if (request != null) {
                try {
                    request.setAttribute(ATTR_REQUEST_ID, requestId);
                } catch (Exception e) {
                }
            }
            MDC.put(ATTR_REQUEST_ID, ATTR_REQUEST_ID + ":" + requestId);
        }
    }

    public void httpBefore(HttpServletRequest request, HttpServletResponse response) {
        createAndSetRequestId(request, response);
    }

    public void httpAfter(HttpServletRequest request, HttpServletResponse response) {
        removeRequestId(request, response);
    }

    public String createAndSetRequestId(HttpServletRequest request, HttpServletResponse response) {
        return getRequestId(request, true);
    }

    public void removeRequestId(HttpServletRequest request, HttpServletResponse response) {
        setRequestId(request, null);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        httpBefore(request, response);
        try {
            chain.doFilter(request, servletResponse);
        } finally {
            httpAfter(request, response);
        }
    }
}
