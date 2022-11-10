package com.github.securityfilter;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

import javax.servlet.http.HttpServletRequest;

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
        String requestId = getRequestId(null, true);
        RpcContext.getContext().setAttachment(ATTR_REQUEST_ID, requestId);
        return invoker.invoke(invocation);
    }

    @Override
    public void createRequestIdAfter(HttpServletRequest request, String requestId) {
        RpcContext.getContext().setAttachment(ATTR_REQUEST_ID, requestId);
    }

    @Override
    public void removeRequestIdAfter(HttpServletRequest request, String requestId) {
        RpcContext.getContext().removeAttachment(ATTR_REQUEST_ID);
    }
}
