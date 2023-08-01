package com.github.securityfilter;

import com.github.securityfilter.util.AccessUserUtil;
import com.github.securityfilter.util.DubboAccessUserUtil;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

import java.util.Map;

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

    protected void dubboBefore(Invoker<?> invoker, Invocation invocation) {
        Map<String, Object> apacheAccessUser = DubboAccessUserUtil.getApacheAccessUser();
        if (apacheAccessUser == null) {
            Object accessUser = AccessUserUtil.getAccessUser();
            if (accessUser != null) {
                DubboAccessUserUtil.setApacheAccessUser(accessUser);
                apacheAccessUser = DubboAccessUserUtil.getApacheAccessUser();
            }
        }

        // 保存现场
        store(invoker, invocation, apacheAccessUser);
    }

    protected void dubboAfter(Invoker<?> invoker, Invocation invocation, Throwable throwable) {
        DubboAccessUserUtil.removeApacheAccessUser();
        // 还原现场
        restore(invoker, invocation, throwable);
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
     * @param throwable
     */
    protected void restore(Invoker<?> invoker, Invocation invocation, Throwable throwable) {
        Object apacheAccessUser = invocation.get(INVOCATION_ATTRIBUTE_KEY);
        if (apacheAccessUser != null) {
            DubboAccessUserUtil.setApacheAccessUser(apacheAccessUser);
        }
    }

}
