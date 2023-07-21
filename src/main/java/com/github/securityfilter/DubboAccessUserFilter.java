package com.github.securityfilter;

import com.github.securityfilter.util.AccessUserUtil;
import com.github.securityfilter.util.DubboAccessUserUtil;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.protocol.dubbo.DecodeableRpcInvocation;

import java.util.Map;

@Activate(
        group = {"consumer"},
        order = 51
)
public class DubboAccessUserFilter implements Filter, Filter.Listener {
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

    protected void dubboBefore(Invoker<?> invoker, Invocation invocation) {
        Map<String, Object> apacheAccessUser = DubboAccessUserUtil.getApacheAccessUser();
        Object accessUser = apacheAccessUser == null ? AccessUserUtil.getAccessUser() : DubboAccessUserUtil.getApacheAccessUser();
        DubboAccessUserUtil.setApacheAccessUser(accessUser);

        // 保存现场
        store(invoker, invocation, apacheAccessUser);
    }

    protected void dubboAfter(Invoker<?> invoker, Invocation invocation, boolean client, Throwable throwable) {
        DubboAccessUserUtil.removeApacheAccessUser();
        // 还原现场
        restore(invoker, invocation, client, throwable);
    }

    /**
     * 保存现场
     */
    protected void store(Invoker<?> invoker, Invocation invocation, Map<String, Object> apacheAccessUser) {
        invocation.put(INVOCATION_ATTRIBUTE_KEY, apacheAccessUser);
    }

    /**
     * 还原现场
     *
     * @param invoker
     * @param invocation
     * @param client
     * @param throwable
     */
    protected void restore(Invoker<?> invoker, Invocation invocation, boolean client, Throwable throwable) {
        Object apacheAccessUser = invocation.get(INVOCATION_ATTRIBUTE_KEY);
        if (apacheAccessUser != null) {
            DubboAccessUserUtil.setApacheAccessUser(apacheAccessUser);
        }
    }

}
