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
        RpcContext context = RpcContext.getContext();
        if (context.getMethodName() == null) {
            return invoker.invoke(invocation);
        }
        String interfaceName = invoker.getInterface().getName();
        for (String skipInterfacePacket : skipInterfacePackets) {
            if (interfaceName.startsWith(skipInterfacePacket)) {
                return invoker.invoke(invocation);
            }
        }
        Throwable throwable = null;
        boolean consumerSide = context.isConsumerSide();
        dubboBefore(invoker, invocation, consumerSide);
        try {
            return invoker.invoke(invocation);
        } catch (Throwable e) {
            throwable = e;
            throw e;
        } finally {
            dubboAfter(invoker, invocation, throwable, consumerSide);
        }
    }

    public static void setDubboRequestId(String requestId) {
        if (requestId == null) {
            RpcContext.getContext().removeAttachment(ATTR_REQUEST_ID);
        } else {
            RpcContext.getContext().setAttachment(ATTR_REQUEST_ID, requestId);
        }
    }

    public void setDubboRequestId0(String requestId) {
        PlatformDependentUtil.mdcClose(ATTR_REQUEST_ID, requestId);
    }

    @Override
    public void httpBefore(HttpServletRequest request, HttpServletResponse response) {
        String requestId = createAndSetRequestId(request, response);
        setDubboRequestId0(requestId);
    }

    @Override
    public void httpAfter(HttpServletRequest request, HttpServletResponse servletResponse) {
        removeRequestId(request, servletResponse);
    }

    protected void dubboBefore(Invoker<?> invoker, Invocation invocation, boolean consumerSide) {
        String requestId = getRequestId(null, true);
        RpcContext.getContext().setAttachment(ATTR_REQUEST_ID, requestId);
        if (!consumerSide) {
            PlatformDependentUtil.mdcPut(ATTR_REQUEST_ID, requestId);
        }
    }

    protected void dubboAfter(Invoker<?> invoker, Invocation invocation, Throwable throwable, boolean consumerSide) {
        RpcContext.getContext().removeAttachment(ATTR_REQUEST_ID);
        if (!consumerSide) {
            PlatformDependentUtil.mdcRemove(ATTR_REQUEST_ID);
        }
    }
}
