package com.github.securityfilter;

import com.github.securityfilter.util.PlatformDependentUtil;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 创建requestId
 */
@Activate(group = {"consumer"}, order = 50)
public class DubboWebRequestIdCreateFilter extends WebRequestIdCreateFilter implements Filter {
    private final String[] skipInterfacePackets = {"org.apache.dubbo", "com.alibaba"};

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String interfaceName = invoker.getInterface().getName();
        for (String skipInterfacePacket : skipInterfacePackets) {
            if (interfaceName.startsWith(skipInterfacePacket)) {
                return invoker.invoke(invocation);
            }
        }
        Throwable throwable = null;
        dubboBefore(invoker, invocation);
        try {
            return invoker.invoke(invocation);
        } catch (Throwable e) {
            throwable = e;
            throw e;
        } finally {
            dubboAfter(invoker, invocation, throwable);
        }
    }

    public static void setDubboRequestId(String requestId) {
        if (requestId == null) {
            RpcContext.getContext().removeAttachment(ATTR_REQUEST_ID);
            PlatformDependentUtil.mdcRemove(ATTR_REQUEST_ID);
        } else {
            RpcContext.getContext().setAttachment(ATTR_REQUEST_ID, requestId);
            PlatformDependentUtil.mdcPut(ATTR_REQUEST_ID, requestId);
        }
    }

    public void setDubboRequestId0(String requestId) {
        setDubboRequestId(requestId);
    }

    @Override
    public void httpBefore(HttpServletRequest request, HttpServletResponse response) {
        String requestId = createAndSetRequestId(request, response);
        setDubboRequestId0(requestId);
    }

    @Override
    public void httpAfter(HttpServletRequest request, HttpServletResponse servletResponse) {
        removeRequestId(request, servletResponse);
        setDubboRequestId0(null);
    }

    protected void dubboBefore(Invoker<?> invoker, Invocation invocation) {
        String requestId = getRequestId(null, true);
        setDubboRequestId0(requestId);
    }

    protected void dubboAfter(Invoker<?> invoker, Invocation invocation, Throwable throwable) {
        setDubboRequestId0(null);
    }
}
