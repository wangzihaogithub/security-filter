package com.github.securityfilter;

import com.github.securityfilter.util.AccessUserUtil;
import com.github.securityfilter.util.DubboAccessUserUtil;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.protocol.dubbo.DecodeableRpcInvocation;

@Activate(
        group = {"consumer"},
        order = 51
)
public class DubboAccessUserFilter implements Filter, Filter.Listener {
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

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        dubboAfter(invoker, invocation, true, null);
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        dubboAfter(invoker, invocation, true, t);
    }

    public void dubboBefore(Invoker<?> invoker, Invocation invocation) {
        DubboAccessUserUtil.setApacheAccessUser(AccessUserUtil.getAccessUser());
    }

    public void dubboAfter(Invoker<?> invoker, Invocation invocation, boolean client, Throwable throwable) {
        DubboAccessUserUtil.removeApacheAccessUser();
    }
}
