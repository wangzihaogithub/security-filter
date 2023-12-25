package com.github.securityfilter.util;

import org.apache.dubbo.rpc.RpcContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class DubboAccessUserUtil {
    public static final String ATTR_PREFIX = System.getProperty("DubboAccessUserUtil.ATTR_PREFIX", "_user") + ".";
    private static final boolean SUPPORT_GET_OBJECT_ATTACHMENT;
    private static final boolean SUPPORT_APACHE_2X_RESTORE_CONTEXT;
    private static final Method APACHE_RESTORE_2X_CONTEXT_METHOD;
    private static final Method APACHE_RESTORE_3X_METHOD;
    private static final Method APACHE_STORE_SERVICE_CONTEXT_3X_METHOD;
    private static final boolean SUPPORT_APACHE_3X_RESTORE_SERVICE_CONTEXT;

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

        boolean supportApacheRestoreContext;
        Method restoreContextMethod;
        try {
            // dubbo 2.x
            Class<?> clazz = Class.forName("org.apache.dubbo.rpc.RpcContext");
            Method method = clazz.getDeclaredMethod("restoreContext", RpcContext.class);
            supportApacheRestoreContext = Modifier.isStatic(method.getModifiers())
                    && Modifier.isPublic(method.getModifiers());
            if (supportApacheRestoreContext) {
                restoreContextMethod = method;
            } else {
                restoreContextMethod = null;
            }
        } catch (Throwable e) {
            supportApacheRestoreContext = false;
            restoreContextMethod = null;
        }
        APACHE_RESTORE_2X_CONTEXT_METHOD = restoreContextMethod;
        SUPPORT_APACHE_2X_RESTORE_CONTEXT = supportApacheRestoreContext;

        boolean supportApacheRestoreServiceContext;
        Method restoreMethod;
        Method storeServiceContextMethod;
        try {
            // dubbo3.x
            Class<?> clazz = Class.forName("org.apache.dubbo.rpc.RpcContext.RestoreServiceContext");
            restoreMethod = clazz.getDeclaredMethod("restore");
            restoreMethod.setAccessible(true);

            storeServiceContextMethod = org.apache.dubbo.rpc.RpcContext.class.getDeclaredMethod("storeServiceContext");
            storeServiceContextMethod.setAccessible(true);
            supportApacheRestoreServiceContext = true;
        } catch (Throwable e) {
            supportApacheRestoreServiceContext = false;
            storeServiceContextMethod = null;
            restoreMethod = null;
        }
        APACHE_RESTORE_3X_METHOD = restoreMethod;
        APACHE_STORE_SERVICE_CONTEXT_3X_METHOD = storeServiceContextMethod;
        SUPPORT_APACHE_3X_RESTORE_SERVICE_CONTEXT = supportApacheRestoreServiceContext;
    }

    private static boolean isUserAttr(String attrName) {
        return attrName != null && attrName.startsWith(ATTR_PREFIX);
    }

    private static String unwrapUserAttrName(String attrName) {
        if (isUserAttr(attrName)) {
            return attrName.substring(ATTR_PREFIX.length());
        } else {
            return attrName;
        }
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

    public <T> CompletableFuture<T> getCompletableFuture() {
        return new AccessUserCompletableFuture<>(RpcContext.getContext().getCompletableFuture());
    }

    public static String getAlibabaAccessUserValue(String name) {
        return com.alibaba.dubbo.rpc.RpcContext.getContext().getAttachment(wrapUserAttrName(name));
    }

    public static Map<String, Object> getApacheAccessUser() {
        RpcContext context = RpcContext.getContext();
        Set<String> attrNameList;
        if (SUPPORT_GET_OBJECT_ATTACHMENT) {
            attrNameList = context.getObjectAttachments().keySet();
        } else {
            attrNameList = context.getAttachments().keySet();
        }
        Map<String, Object> result = null;
        for (String attrName : attrNameList) {
            if (!isUserAttr(attrName)) {
                continue;
            }
            Object value;
            if (SUPPORT_GET_OBJECT_ATTACHMENT) {
                value = context.getObjectAttachment(attrName);
            } else {
                value = context.getAttachment(attrName);
            }
            if (result == null) {
                result = new LinkedHashMap<>(6);
            }
            result.put(unwrapUserAttrName(attrName), value);
        }
        return result == null || result.isEmpty() ? null : result;
    }

    public static Map<String, String> getAlibabaAccessUser() {
        com.alibaba.dubbo.rpc.RpcContext context = com.alibaba.dubbo.rpc.RpcContext.getContext();
        Map<String, String> result = null;
        for (String attrName : context.getAttachments().keySet()) {
            if (!isUserAttr(attrName)) {
                continue;
            }
            if (result == null) {
                result = new LinkedHashMap<>(6);
            }
            result.put(unwrapUserAttrName(attrName), context.getAttachment(attrName));
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
        RpcContext context = RpcContext.getContext();
        Set<String> attrNameList;
        if (SUPPORT_GET_OBJECT_ATTACHMENT) {
            attrNameList = context.getObjectAttachments().keySet();
        } else {
            attrNameList = context.getAttachments().keySet();
        }
        for (String attrName : new ArrayList<>(attrNameList)) {
            if (isUserAttr(attrName)) {
                context.removeAttachment(attrName);
            }
        }
    }

    public static void removeAlibabaAccessUser() {
        com.alibaba.dubbo.rpc.RpcContext context = com.alibaba.dubbo.rpc.RpcContext.getContext();
        for (String attr : new ArrayList<>(context.getAttachments().keySet())) {
            if (isUserAttr(attr)) {
                context.removeAttachment(attr);
            }
        }
    }

    public static boolean isApacheAccessUser() {
        RpcContext context = RpcContext.getContext();
        return context != null && context.getMethodName() != null;
    }

    public static boolean isAlibabaAccessUser() {
        com.alibaba.dubbo.rpc.RpcContext context = com.alibaba.dubbo.rpc.RpcContext.getContext();
        try {
            return context != null && context.getMethodName() != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static void setApacheAccessUser(Object accessUser) {
        removeApacheAccessUser();
        if (AccessUserUtil.isNotNull(accessUser)) {
            Map<String, Object> beanHandler = BeanMap.toMap(accessUser);
            RpcContext context = RpcContext.getContext();
            for (Map.Entry<?, ?> entry : beanHandler.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (!(key instanceof String)) {
                    continue;
                }
                String name = wrapUserAttrName((String) key);
                if (value == null) {
                    context.removeAttachment(name);
                } else if (isBasicType(value)) {
                    if (SUPPORT_GET_OBJECT_ATTACHMENT) {
                        context.setObjectAttachment(name, value);
                    } else {
                        context.setAttachment(name, value.toString());
                    }
                }
            }
        }
    }

    public static void setAlibabaAccessUser(Object accessUser) {
        removeAlibabaAccessUser();
        if (AccessUserUtil.isNotNull(accessUser)) {
            Map<String, Object> beanHandler = BeanMap.toMap(accessUser);
            com.alibaba.dubbo.rpc.RpcContext context = com.alibaba.dubbo.rpc.RpcContext.getContext();
            for (Map.Entry<?, ?> entry : beanHandler.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (!(key instanceof String)) {
                    continue;
                }
                String name = wrapUserAttrName((String) key);
                if (value == null) {
                    context.removeAttachment(name);
                } else if (isBasicType(value)) {
                    context.setAttachment(name, value.toString());
                }
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

    public static boolean isApacheDubboConsumerSide() {
        return RpcContext.getContext().isConsumerSide();
    }

    public static boolean isAlibabaDubboConsumerSide() {
        return com.alibaba.dubbo.rpc.RpcContext.getContext().isConsumerSide();
    }

    public static boolean isApacheNestingRequest() {
        return RpcContext.getContext().getUrl() != null;
    }

    public static <T> T apacheNestingRequest(Supplier<T> request) {
        if (SUPPORT_APACHE_3X_RESTORE_SERVICE_CONTEXT) {
            boolean nesting = RpcContext.getContext().getUrl() != null;
            Object restoreServiceContext;
            if (nesting) {
                try {
                    restoreServiceContext = APACHE_STORE_SERVICE_CONTEXT_3X_METHOD.invoke(null);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    PlatformDependentUtil.sneakyThrows(e);
                    return null;
                }
            } else {
                restoreServiceContext = null;
            }
            try {
                return request.get();
            } finally {
                if (restoreServiceContext != null) {
                    try {
                        APACHE_RESTORE_3X_METHOD.invoke(restoreServiceContext);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        PlatformDependentUtil.sneakyThrows(e);
                    }
                }
            }
        } else if (SUPPORT_APACHE_2X_RESTORE_CONTEXT) {
            RpcContext oldContext = RpcContext.getContext();
            boolean nesting = oldContext.getUrl() != null;
            RpcContext snapshotContext;
            if (nesting) {
                snapshotContext = new RpcContext() {

                };
                snapshotContext.setRequest(oldContext.getRequest());
                snapshotContext.setResponse(oldContext.getResponse());
                snapshotContext.setFuture(oldContext.getCompletableFuture());
                snapshotContext.setInvokers(oldContext.getInvokers());
                snapshotContext.setInvoker(oldContext.getInvoker());
                snapshotContext.setInvocation(oldContext.getInvocation());
                snapshotContext.setUrls(oldContext.getUrls());
                snapshotContext.setUrl(oldContext.getUrl());
                snapshotContext.setMethodName(oldContext.getMethodName());
                snapshotContext.setParameterTypes(oldContext.getParameterTypes());
                snapshotContext.setArguments(oldContext.getArguments());
                snapshotContext.setLocalAddress(oldContext.getLocalAddress());
                snapshotContext.setRemoteAddress(oldContext.getRemoteAddress());
                snapshotContext.setRemoteApplicationName(oldContext.getRemoteApplicationName());
                snapshotContext.setObjectAttachments(oldContext.getObjectAttachments());
                snapshotContext.get().putAll(oldContext.get());
                snapshotContext.setConsumerUrl(oldContext.getConsumerUrl());
            } else {
                snapshotContext = null;
            }
            try {
                return request.get();
            } finally {
                if (snapshotContext != null) {
                    try {
                        APACHE_RESTORE_2X_CONTEXT_METHOD.invoke(null, snapshotContext);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        PlatformDependentUtil.sneakyThrows(e);
                    }
                }
            }
        } else {
            return request.get();
        }
    }

    public static boolean isBasicType(Object value) {
        if (value == null || value instanceof Class) {
            return false;
        }
        return value.getClass().isPrimitive()
                || value instanceof Number
                || value instanceof String
                || value instanceof Date
                || value instanceof TemporalAccessor
                || value instanceof Enum;
    }
}
