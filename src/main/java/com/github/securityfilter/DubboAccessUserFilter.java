package com.github.securityfilter;

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
        AccessUserContext accessUserContext = newAccessUserContext(consumerSide);
        setAccessUserContext(invocation, accessUserContext);
    }

    protected void dubboAfter(Invoker<?> invoker, Invocation invocation, Throwable throwable, boolean consumerSide) {
        AccessUserContext accessUserContext = getAccessUserContext(invocation);
        if (accessUserContext != null) {
            accessUserContext.close();
        }
    }

    protected AccessUserContext newAccessUserContext(boolean consumerSide) {
        AccessUserContext result;
        Object accessUser = AccessUserUtil.getCurrentThreadAccessUser();
        if (accessUser != null) {
            result = new CurrentThreadAccessUserContext(accessUser, consumerSide);
        } else {
            if (PlatformDependentUtil.EXIST_HTTP_SERVLET) {
                accessUser = WebSecurityAccessFilter.getCurrentAccessUserIfExist();
            }
            if (accessUser != null) {
                result = new HttpServletAccessUserContext(accessUser, consumerSide);
            } else {
                accessUser = DubboAccessUserUtil.getApacheAccessUser();
                if (accessUser != null) {
                    result = new ApacheDubboAccessUserContext(accessUser);
                } else {
                    result = NullAccessUserContext.INSTANCE;
                }
            }
        }
        return result;
    }

    public static void setAccessUserContext(Invocation invocation, AccessUserContext accessUserContext) {
        invocation.put(INVOCATION_ATTRIBUTE_KEY, accessUserContext);
    }

    public static AccessUserContext getAccessUserContext(Invocation invocation) {
        return (AccessUserContext) invocation.get(INVOCATION_ATTRIBUTE_KEY);
    }

    interface AccessUserContext {
        void close();
    }

    static class CurrentThreadAccessUserContext implements AccessUserContext {
        private final Thread thread = Thread.currentThread();
        private final Object accessUser;
        private final boolean consumerSide;

        public CurrentThreadAccessUserContext(Object accessUser, boolean consumerSide) {
            this.accessUser = accessUser;
            this.consumerSide = consumerSide;
            if (consumerSide) {
                DubboAccessUserUtil.setApacheAccessUser(accessUser);
            }
        }

        @Override
        public void close() {
            if (thread != Thread.currentThread()) {
                throw new IllegalStateException("thread");
            }
            if (consumerSide) {
                DubboAccessUserUtil.removeApacheAccessUser();
            }
        }
    }

    static class ApacheDubboAccessUserContext implements AccessUserContext {
        private final Thread thread = Thread.currentThread();
        private final Object accessUser;

        public ApacheDubboAccessUserContext(Object accessUser) {
            this.accessUser = accessUser;
            AccessUserUtil.setCurrentThreadAccessUser(accessUser);
        }

        @Override
        public void close() {
            if (thread != Thread.currentThread()) {
                throw new IllegalStateException("thread");
            }
            AccessUserUtil.removeCurrentThreadAccessUser();
        }
    }

    static class HttpServletAccessUserContext implements AccessUserContext {
        private final Object accessUser;
        public final boolean consumerSide;

        public HttpServletAccessUserContext(Object accessUser, boolean consumerSide) {
            this.accessUser = accessUser;
            this.consumerSide = consumerSide;
            if (consumerSide) {
                DubboAccessUserUtil.setApacheAccessUser(accessUser);
            }
        }

        @Override
        public void close() {
            if (consumerSide) {
                DubboAccessUserUtil.removeApacheAccessUser();
            }
        }
    }

    static class NullAccessUserContext implements AccessUserContext {
        public static final NullAccessUserContext INSTANCE = new NullAccessUserContext();

        @Override
        public void close() {

        }
    }

}
