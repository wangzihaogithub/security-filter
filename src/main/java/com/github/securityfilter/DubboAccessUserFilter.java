package com.github.securityfilter;

import com.github.securityfilter.util.AccessUserSnapshot;
import com.github.securityfilter.util.AccessUserUtil;
import com.github.securityfilter.util.DubboAccessUserUtil;
import com.github.securityfilter.util.PlatformDependentUtil;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

import java.util.Map;

@Activate(
        group = {"consumer", "provider"},
        order = 51
)
public class DubboAccessUserFilter implements Filter {
    private static final String INVOCATION_ATTRIBUTE_KEY = System.getProperty("DubboAccessUserFilter.INVOCATION_ATTRIBUTE_KEY", "accessUser");
    private static final AutoCloseable REMOVE_ATTACHMENT = () -> {
        DubboAccessUserUtil.removeApacheAccessUser();
        DubboAccessUserUtil.removeApacheRootAccessUser();
    };
    private final String[] skipInterfacePackets = {"org.apache.dubbo", "com.alibaba"};

    public static void setAccessUserContext(Invocation invocation, AutoCloseable accessUserContext) {
        invocation.put(INVOCATION_ATTRIBUTE_KEY, accessUserContext);
    }

    public static AutoCloseable getAccessUserContext(Invocation invocation) {
        return (AutoCloseable) invocation.get(INVOCATION_ATTRIBUTE_KEY);
    }

    public static AutoCloseable setAttachment(Object accessUser, Object runOnRootAccessUser) {
        DubboAccessUserUtil.setApacheAccessUser(accessUser);
        if (AccessUserUtil.isExistRoot(runOnRootAccessUser) && AccessUserUtil.isNotNull(accessUser)) {
            DubboAccessUserUtil.setApacheRootAccessUser(runOnRootAccessUser);
        }
        return REMOVE_ATTACHMENT;
    }

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

        boolean consumerSide = context.isConsumerSide();
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
        Object runOnRootAccessUser = AccessUserUtil.getRootAccessUser(false, false);
        Object threadAccessUser = AccessUserUtil.getCurrentThreadAccessUser();
        Object webAccessUser;
        Map<String, Object> dubboAccessUser;
        if (threadAccessUser != null) {
            // 线程上下文（Web，Dubbo（Server，Client），定时器，任务线程） 放DubboRpcContext
            result = consumerSide ? setAttachment(threadAccessUser, runOnRootAccessUser) : null;
        } else if ((webAccessUser = PlatformDependentUtil.EXIST_HTTP_SERVLET ? WebSecurityAccessFilter.getCurrentAccessUserIfExist() : null) != null) {
            // Web层请求 放DubboRpcContext
            result = consumerSide ? setAttachment(webAccessUser, runOnRootAccessUser) : null;
        } else if ((dubboAccessUser = DubboAccessUserUtil.getApacheAccessUser()) != null) {
            // 服务端接收 放 线程上下文
            result = new DubboCurrentThreadLocal(dubboAccessUser, runOnRootAccessUser);
        } else {
            result = null;
        }
        return result;
    }

    public static class DubboCurrentThreadLocal extends AccessUserSnapshot.CurrentThreadLocal {
        private final Map<String, Object> dubboAccessUser;
        private final String dubboRequestId = RpcContext.getContext().getAttachment(ATTR_REQUEST_ID);

        public DubboCurrentThreadLocal(Map<String, Object> accessUser, Object rootAccessUser) {
            super(null, rootAccessUser, false, TypeEnum.fork);
            this.dubboAccessUser = accessUser;
            setAccessUser(accessUser, false);
            if (dubboRequestId != null) {
                setRequestId(dubboRequestId);
            }
        }

        public String getDubboRequestId() {
            return dubboRequestId;
        }

        public Map<String, Object> getDubboAccessUser() {
            return dubboAccessUser;
        }
    }

}
