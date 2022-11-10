package com.github.securityfilter.util;

import org.apache.dubbo.rpc.RpcContext;

import java.time.temporal.TemporalAccessor;
import java.util.*;

public class DubboAccessUserUtil {
    public static final String ATTR_PREFIX = System.getProperty("DubboAccessUserUtil.ATTR_PREFIX", "_user") + ".";
    private static final boolean SUPPORT_GET_OBJECT_ATTACHMENT;

    static {
        boolean supportGetObjectAttachment;
        try {
            RpcContext.getContext().getObjectAttachment("");
            supportGetObjectAttachment = true;
        } catch (Throwable e) {
            supportGetObjectAttachment = false;
        }
        SUPPORT_GET_OBJECT_ATTACHMENT = supportGetObjectAttachment;
    }

    private static boolean isUserAttr(String attrName) {
        return attrName != null && attrName.startsWith(ATTR_PREFIX);
    }

    private static String wrapUserAttrName(String attrName) {
        if (isUserAttr(attrName)) {
            return attrName;
        } else {
            return ATTR_PREFIX + attrName;
        }
    }

    public static Object getApacheAccessUserValue(String name) {
        if (SUPPORT_GET_OBJECT_ATTACHMENT) {
            return RpcContext.getContext().getObjectAttachment(wrapUserAttrName(name));
        } else {
            return RpcContext.getContext().getAttachment(wrapUserAttrName(name));
        }
    }

    public static String getAlibabaAccessUserValue(String name) {
        return com.alibaba.dubbo.rpc.RpcContext.getContext().getAttachment(wrapUserAttrName(name));
    }

    public static Map<String, Object> getApacheAccessUser() {
        Set<String> attrNameList;
        if (SUPPORT_GET_OBJECT_ATTACHMENT) {
            attrNameList = RpcContext.getContext().getObjectAttachments().keySet();
        } else {
            attrNameList = RpcContext.getContext().getAttachments().keySet();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (String attrName : new ArrayList<>(attrNameList)) {
            if (isUserAttr(attrName)) {
                Object value;
                if (SUPPORT_GET_OBJECT_ATTACHMENT) {
                    value = RpcContext.getContext().getObjectAttachment(attrName);
                } else {
                    value = RpcContext.getContext().getAttachment(attrName);
                }
                result.put(attrName, value);
            }
        }
        return result.isEmpty() ? null : result;
    }

    public static Map<String, String> getAlibabaAccessUser() {
        Map<String, String> result = new LinkedHashMap<>();
        for (String attrName : new ArrayList<>(com.alibaba.dubbo.rpc.RpcContext.getContext().getAttachments().keySet())) {
            if (isUserAttr(attrName)) {
                result.put(attrName, com.alibaba.dubbo.rpc.RpcContext.getContext().getAttachment(attrName));
            }
        }
        return result.isEmpty() ? null : result;
    }

    public static void removeApacheAccessUser() {
        Set<String> attrNameList;
        if (SUPPORT_GET_OBJECT_ATTACHMENT) {
            attrNameList = RpcContext.getContext().getObjectAttachments().keySet();
        } else {
            attrNameList = RpcContext.getContext().getAttachments().keySet();
        }
        for (String attrName : new ArrayList<>(attrNameList)) {
            if (isUserAttr(attrName)) {
                RpcContext.getContext().removeAttachment(attrName);
            }
        }
    }

    public static void removeAlibabaAccessUser() {
        for (String attr : new ArrayList<>(com.alibaba.dubbo.rpc.RpcContext.getContext().getAttachments().keySet())) {
            if (isUserAttr(attr)) {
                com.alibaba.dubbo.rpc.RpcContext.getContext().removeAttachment(attr);
            }
        }
    }

    public static void setApacheAccessUser(Object accessUser) {
        Map<String, Object> beanHandler = BeanMap.toMap(accessUser);
        for (Map.Entry<?, ?> entry : beanHandler.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (key instanceof String && isBaseType(value)) {
                RpcContext.getContext().setAttachment(wrapUserAttrName((String) key), value);
            }
        }
    }

    public static void setAlibabaAccessUser(Object accessUser) {
        Map<String, Object> beanHandler = BeanMap.toMap(accessUser);
        for (Map.Entry<?, ?> entry : beanHandler.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (key instanceof String && isBaseType(value)) {
                com.alibaba.dubbo.rpc.RpcContext.getContext().setAttachment(wrapUserAttrName((String) key), Objects.toString(value, null));
            }
        }
    }

    public static void setApacheAccessUserValue(String attrName, Object value) {
        RpcContext.getContext().setAttachment(wrapUserAttrName(attrName), value);
    }

    public static void setAlibabaAccessUserValue(String attrName, Object value) {
        com.alibaba.dubbo.rpc.RpcContext.getContext().setAttachment(wrapUserAttrName(attrName), Objects.toString(value, null));
    }

    public static boolean isBaseType(Object value) {
        if (value == null) {
            return false;
        }
        return value.getClass().isPrimitive()
                || value instanceof Number
                || value instanceof CharSequence
                || value instanceof Date
                || value instanceof TemporalAccessor
                || value instanceof Enum;
    }
}
