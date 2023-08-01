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
            if (requestId != null) {
                requestId = requestId.substring(ATTR_REQUEST_ID.length() + 1);
            }
        }
        if (requestId == null && create) {
            requestId = REQUEST_ID_SUPPLIER.get();
            setRequestId(requestId);
        }
        return requestId;
    }

    public static void setRequestId(String requestId) {
        if (requestId == null) {
            RpcContext.getContext().removeAttachment(ATTR_REQUEST_ID);
            PlatformDependentUtil.mdcRemove(ATTR_REQUEST_ID);
        } else {
            RpcContext.getContext().setAttachment(ATTR_REQUEST_ID, requestId);
            PlatformDependentUtil.mdcPut(ATTR_REQUEST_ID, ATTR_REQUEST_ID + ":" + requestId);
        }
    }

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
        String requestId = getRequestId(true, invocation);
        setRequestId(requestId);
    }

    protected void dubboAfter(Invoker<?> invoker, Invocation invocation, Throwable throwable) {
        setRequestId(null);
    }
}
