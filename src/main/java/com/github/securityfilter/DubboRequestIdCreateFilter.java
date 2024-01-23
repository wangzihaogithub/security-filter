package com.github.securityfilter;

import com.github.securityfilter.util.PlatformDependentUtil;
import com.github.securityfilter.util.SnowflakeIdWorker;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

import java.util.function.Supplier;

/**
 * 创建requestId
 */
@Activate(group = {"consumer", "provider"}, order = Integer.MIN_VALUE + 100)
public class DubboRequestIdCreateFilter implements Filter {
    private static final SnowflakeIdWorker ID_WORKER = new SnowflakeIdWorker();
    private static final Supplier<String> REQUEST_ID_SUPPLIER = () -> String.valueOf(ID_WORKER.nextId());
    public static final String ATTR_REQUEST_ID = PlatformDependentUtil.ATTR_REQUEST_ID;
    private final String[] skipInterfacePackets = {"org.apache.dubbo", "com.alibaba"};

    public static String getRequestId(boolean create) {
        return getRequestId(create, null);
    }

    public static String getRequestId(boolean create, Invocation invocation) {
        String requestId = null;
        if (invocation != null) {
            requestId = invocation.getAttachment(ATTR_REQUEST_ID);
        }
        if (requestId == null) {
            requestId = RpcContext.getContext().getAttachment(ATTR_REQUEST_ID);
        }
        if (requestId == null) {
            requestId = PlatformDependentUtil.mdcGet(ATTR_REQUEST_ID);
        }
        if (requestId == null && create) {
            requestId = REQUEST_ID_SUPPLIER.get();
            setRequestId(requestId);
            PlatformDependentUtil.mdcGet(ATTR_REQUEST_ID);
        }
        return requestId;
    }

    public static void setRequestId(String requestId) {
        PlatformDependentUtil.mdcClose(ATTR_REQUEST_ID, requestId);
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

    protected void dubboBefore(Invoker<?> invoker, Invocation invocation, boolean consumerSide) {
        String requestId = getRequestId(true, invocation);
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
