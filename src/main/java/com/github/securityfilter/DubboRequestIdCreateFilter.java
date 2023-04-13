package com.github.securityfilter;

import com.github.securityfilter.util.SnowflakeIdWorker;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.protocol.dubbo.DecodeableRpcInvocation;
import org.slf4j.MDC;

import java.util.function.Supplier;

/**
 * 创建requestId
 */
@Activate(group = {"consumer", "provider"}, order = Integer.MIN_VALUE + 100)
public class DubboRequestIdCreateFilter implements Filter, Filter.Listener {
    private static final SnowflakeIdWorker ID_WORKER = new SnowflakeIdWorker();
    private static final Supplier<String> REQUEST_ID_SUPPLIER = () -> String.valueOf(ID_WORKER.nextId());
    public static final String ATTR_REQUEST_ID =
            System.getProperty("DubboRequestIdCreateFilter.ATTR_REQUEST_ID", "requestId");
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
            requestId = MDC.get(ATTR_REQUEST_ID);
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
            MDC.remove(ATTR_REQUEST_ID);
        } else {
            RpcContext.getContext().setAttachment(ATTR_REQUEST_ID, requestId);
            MDC.put(ATTR_REQUEST_ID, ATTR_REQUEST_ID + ":" + requestId);
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
        // 客户端调用结束
        dubboAfter(invoker, invocation, true, null);
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        // 客户端调用结束
        dubboAfter(invoker, invocation, true, t);
    }

    public void dubboBefore(Invoker<?> invoker, Invocation invocation) {
        String requestId = getRequestId(true, invocation);
        setRequestId(requestId);
    }

    public void dubboAfter(Invoker<?> invoker, Invocation invocation, boolean client, Throwable throwable) {
        setRequestId(null);
    }
}
