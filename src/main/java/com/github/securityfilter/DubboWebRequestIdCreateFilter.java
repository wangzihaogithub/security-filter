package com.github.securityfilter;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.protocol.dubbo.DecodeableRpcInvocation;
import org.slf4j.MDC;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 创建requestId
 */
@Activate(group = {"consumer"}, order = 50)
public class DubboWebRequestIdCreateFilter extends WebRequestIdCreateFilter implements Filter, Filter.Listener {
    private final String[] skipInterfacePackets = {"org.apache.dubbo", "com.alibaba"};

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String interfaceName = invoker.getInterface().getName();
        for (String skipInterfacePacket : skipInterfacePackets) {
            if (interfaceName.startsWith(skipInterfacePacket)) {
                return invoker.invoke(invocation);
            }
        }
        dubboBefore(invoker, invocation);
        Throwable throwable = null;
        try {
            return invoker.invoke(invocation);
        } catch (Throwable t) {
            throwable = t;
            throw t;
        } finally {
            if (invocation instanceof DecodeableRpcInvocation) {
                // 服务端调用流程结束, 清空数据
                dubboAfter(invoker, invocation, false, throwable);
            } else {
                dubboAfter(invoker, invocation, true, throwable);
            }
        }
    }

    public static void setDubboRequestId(String requestId) {
        if (requestId == null) {
            RpcContext.getContext().removeAttachment(ATTR_REQUEST_ID);
            MDC.remove(ATTR_REQUEST_ID);
        } else {
            RpcContext.getContext().setAttachment(ATTR_REQUEST_ID, requestId);
            MDC.put(ATTR_REQUEST_ID, ATTR_REQUEST_ID + ":" + requestId);
        }
    }

    public void setDubboRequestId0(String requestId) {
        setDubboRequestId(requestId);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        // 客户端调用结束
        dubboAfter(invoker, invocation, true, null);
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        // 客户端调用结束
        dubboAfter(invoker, invocation, true, t);
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

    public void dubboBefore(Invoker<?> invoker, Invocation invocation) {
        String requestId = getRequestId(null, true);
        setDubboRequestId0(requestId);
    }

    public void dubboAfter(Invoker<?> invoker, Invocation invocation, boolean client, Throwable throwable) {
        setDubboRequestId0(null);
    }
}
