package com.github.securityfilter;

import com.github.securityfilter.util.AccessUserSnapshot;
import com.github.securityfilter.util.AccessUserUtil;
import com.github.securityfilter.util.DubboAccessUserUtil;
import com.github.securityfilter.util.PlatformDependentUtil;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

@Activate(
        group = {"consumer", "provider"},
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
        AutoCloseable accessUserContext = newAccessUserContext(consumerSide);
        if (accessUserContext != null) {
            setAccessUserContext(invocation, accessUserContext);
        }
    }

    protected void dubboAfter(Invoker<?> invoker, Invocation invocation, Throwable throwable, boolean consumerSide) {
        AutoCloseable accessUserContext = getAccessUserContext(invocation);
        if (accessUserContext != null) {
            try {
                accessUserContext.close();
            } catch (Exception e) {
                PlatformDependentUtil.sneakyThrows(e);
            }
        }
    }

    protected AutoCloseable newAccessUserContext(boolean consumerSide) {
        AutoCloseable result;
        Object runOnRootAccessUser = AccessUserUtil.getRootAccessUser(false);
        Object accessUser = AccessUserUtil.getCurrentThreadAccessUser();
        if (accessUser != null) {
            // 线程上下文（Web，Dubbo（Server，Client），定时器，任务线程） 放DubboRpcContext
            result = consumerSide ? setAttachment(accessUser, runOnRootAccessUser) : null;
        } else {
            if (PlatformDependentUtil.EXIST_HTTP_SERVLET) {
                accessUser = WebSecurityAccessFilter.getCurrentAccessUserIfExist();
            }
            if (accessUser != null) {
                // Web层请求 放DubboRpcContext
                result = consumerSide ? setAttachment(accessUser, runOnRootAccessUser) : null;
            } else {
                accessUser = DubboAccessUserUtil.getApacheAccessUser();
                if (accessUser != null) {
                    // 服务端接收 放 线程上下文
                    result = setCurrentThread(accessUser, runOnRootAccessUser);
                } else {
                    result = null;
                }
            }
        }
        return result;
    }

    public static void setAccessUserContext(Invocation invocation, AutoCloseable accessUserContext) {
        invocation.put(INVOCATION_ATTRIBUTE_KEY, accessUserContext);
    }

    public static AutoCloseable getAccessUserContext(Invocation invocation) {
        return (AutoCloseable) invocation.get(INVOCATION_ATTRIBUTE_KEY);
    }

    private static final AutoCloseable REMOVE_ATTACHMENT = () -> {
        DubboAccessUserUtil.removeApacheAccessUser();
        DubboAccessUserUtil.removeApacheRootAccessUser();
    };

    public static AutoCloseable setAttachment(Object accessUser, Object runOnRootAccessUser) {
        DubboAccessUserUtil.setApacheAccessUser(accessUser);
        if (AccessUserUtil.isExistRoot(runOnRootAccessUser) && AccessUserUtil.isNotNull(accessUser)) {
            DubboAccessUserUtil.setApacheRootAccessUser(runOnRootAccessUser);
        }
        return REMOVE_ATTACHMENT;
    }

    public static AutoCloseable setCurrentThread(Object accessUser, Object runOnRootAccessUser) {
        AccessUserSnapshot closeable = new AccessUserSnapshot.CurrentThreadLocal(null, runOnRootAccessUser, false, AccessUserSnapshot.TypeEnum.push);
        closeable.setAccessUser(accessUser, false);
        return closeable;
    }

}
