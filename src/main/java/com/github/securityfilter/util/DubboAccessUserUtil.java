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
            Class<?> clazz = Class.forName("org.apache.dubbo.rpc.RpcContext");
            clazz.getDeclaredMethod("getObjectAttachment", String.class);
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
        Map<String, Object> result = null;
        for (String attrName : attrNameList) {
            if (!isUserAttr(attrName)) {
                continue;
            }
            Object value;
            if (SUPPORT_GET_OBJECT_ATTACHMENT) {
                value = RpcContext.getContext().getObjectAttachment(attrName);
            } else {
                value = RpcContext.getContext().getAttachment(attrName);
            }
            if (result == null) {
                result = new LinkedHashMap<>(6);
            }
            result.put(attrName, value);
        }
        return result == null || result.isEmpty() ? null : result;
    }

    public static Map<String, String> getAlibabaAccessUser() {
        Map<String, String> result = null;
        for (String attrName : com.alibaba.dubbo.rpc.RpcContext.getContext().getAttachments().keySet()) {
            if (!isUserAttr(attrName)) {
                continue;
            }
            if (result == null) {
                result = new LinkedHashMap<>(6);
            }
            result.put(attrName, com.alibaba.dubbo.rpc.RpcContext.getContext().getAttachment(attrName));
        }
        return result == null || result.isEmpty() ? null : result;
    }

    public static boolean existAlibabaAccessUser() {
        for (String attrName : com.alibaba.dubbo.rpc.RpcContext.getContext().getAttachments().keySet()) {
            if (isUserAttr(attrName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean existApacheAccessUser() {
        Set<String> attrNameList;
        if (SUPPORT_GET_OBJECT_ATTACHMENT) {
            attrNameList = RpcContext.getContext().getObjectAttachments().keySet();
        } else {
            attrNameList = RpcContext.getContext().getAttachments().keySet();
        }
        for (String attrName : attrNameList) {
            if (isUserAttr(attrName)) {
                return true;
            }
        }
        return false;
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
            if (!(key instanceof String)) {
                continue;
            }
            String name = wrapUserAttrName((String) key);
            if (value == null) {
                RpcContext.getContext().removeAttachment(name);
            } else if (isBasicType(value)) {
                if (SUPPORT_GET_OBJECT_ATTACHMENT) {
                    RpcContext.getContext().setObjectAttachment(name, value);
                } else {
                    RpcContext.getContext().setAttachment(name, value.toString());
                }
            }
        }
    }

    public static void setAlibabaAccessUser(Object accessUser) {
        Map<String, Object> beanHandler = BeanMap.toMap(accessUser);
        for (Map.Entry<?, ?> entry : beanHandler.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (!(key instanceof String)) {
                continue;
            }
            String name = wrapUserAttrName((String) key);
            if (value == null) {
                com.alibaba.dubbo.rpc.RpcContext.getContext().removeAttachment(name);
            } else if (isBasicType(value)) {
                com.alibaba.dubbo.rpc.RpcContext.getContext().setAttachment(name, value.toString());
            }
        }
    }

    public static void setApacheAccessUserValue(String attrName, Object value) {
        String name = wrapUserAttrName(attrName);
        if (value == null) {
            RpcContext.getContext().removeAttachment(name);
        } else if (SUPPORT_GET_OBJECT_ATTACHMENT) {
            RpcContext.getContext().setObjectAttachment(name, value);
        } else {
            RpcContext.getContext().setAttachment(name, value.toString());
        }
    }

    public static void setAlibabaAccessUserValue(String attrName, Object value) {
        String name = wrapUserAttrName(attrName);
        if (value == null) {
            com.alibaba.dubbo.rpc.RpcContext.getContext().removeAttachment(name);
        } else {
            com.alibaba.dubbo.rpc.RpcContext.getContext().setAttachment(name, value.toString());
        }
    }

    public static boolean isBasicType(Object value) {
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
