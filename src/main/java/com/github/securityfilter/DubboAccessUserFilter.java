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
    private final String[] skipInterfacePackets = {"org.apache.dubbo", "com.alibaba"};

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String interfaceName = invoker.getInterface().getName();
        for (String skipInterfacePacket : skipInterfacePackets) {
            if (interfaceName.startsWith(skipInterfacePacket)) {
                return invoker.invoke(invocation);
            }
        }
        DubboAccessUserUtil.setApacheAccessUser(AccessUserUtil.getAccessUser());
        return invoker.invoke(invocation);
    }

}
