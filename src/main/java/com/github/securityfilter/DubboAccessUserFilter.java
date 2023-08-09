package com.github.securityfilter;

import com.github.securityfilter.util.AccessUserUtil;
import com.github.securityfilter.util.DubboAccessUserUtil;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

@Activate(
        group = {"consumer"},
        order = 51
)
public class DubboAccessUserFilter implements Filter {
    private static final String INVOCATION_ATTRIBUTE_KEY = System.getProperty("DubboAccessUserFilter.INVOCATION_ATTRIBUTE_KEY", "accessUser");

    private final String[] skipInterfacePackets = {"org.apache.dubbo", "com.alibaba"};

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String interfaceName = invoker.getInterface().getName();
        for (String skipInterfacePacket : skipInterfacePackets) {
            if (interfaceName.startsWith(skipInterfacePacket)) {
                return invoker.invoke(invocation);
            }
        }

        boolean consumerSide = RpcContext.getContext().isConsumerSide();
        Throwable throwable = null;
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

    protected void dubboBefore(Invoker<?> invoker, Invocation invocation, boolean consumerSide) {
        Object accessUser = AccessUserUtil.getAccessUser();
        if (accessUser == null) {
            return;
        }
        if (consumerSide) {
            DubboAccessUserUtil.setApacheAccessUser(accessUser);
        } else {
            AccessUserUtil.setCurrentThreadAccessUser(accessUser);
        }
    }

    protected void dubboAfter(Invoker<?> invoker, Invocation invocation, Throwable throwable, boolean consumerSide) {
        if (consumerSide) {
            DubboAccessUserUtil.removeApacheAccessUser();
        } else {
            AccessUserUtil.removeCurrentThreadAccessUser();
        }
    }

}
