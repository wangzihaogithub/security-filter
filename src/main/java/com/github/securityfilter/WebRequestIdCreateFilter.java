package com.github.securityfilter;

import com.github.securityfilter.util.SnowflakeIdWorker;
import org.slf4j.MDC;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
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

    public void createRequestIdAfter(HttpServletRequest request, String requestId) {

    }

    public void removeRequestIdAfter(HttpServletRequest request, String requestId) {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String requestId = getRequestId(request, true);
        createRequestIdAfter(request, requestId);
        try {
            chain.doFilter(request, servletResponse);
        } finally {
            setRequestId(request, null);
            removeRequestIdAfter(request, requestId);
        }
    }
}
