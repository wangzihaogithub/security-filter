package com.github.securityfilter.util;

import com.github.securityfilter.WebSecurityAccessFilter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class AccessUserUtil {
    public static final Object NULL = new Object();
    /**
     * 跨线程传递当前RPC请求的用户
     */
    private static final ThreadLocal<Supplier<Object>> ACCESS_USER_THREAD_LOCAL = new ThreadLocal<>();

    public static boolean isNotNull(Object accessUser) {
        return !isNull(accessUser);
    }

    public static boolean isNull(Object accessUser) {
        return accessUser == null || accessUser == NULL;
    }

    public static boolean existAccessUser() {
        return existCurrentThreadAccessUser() || existWebAccessUser() || existDubboAccessUser();
    }

    public static boolean existCurrentThreadAccessUser() {
        return isNotNull(getCurrentThreadAccessUser());
    }

    public static boolean existWebAccessUser() {
        return PlatformDependentUtil.EXIST_HTTP_SERVLET
                && isNotNull(WebSecurityAccessFilter.getCurrentAccessUserIfExist());
    }

    public static boolean existDubboAccessUser() {
        if (PlatformDependentUtil.EXIST_DUBBO_APACHE
                && DubboAccessUserUtil.existApacheAccessUser()) {
            return true;
        } else if (PlatformDependentUtil.EXIST_DUBBO_ALIBABA
                && DubboAccessUserUtil.existAlibabaAccessUser()) {
            return true;
        } else {
            return false;
        }
    }

    public static Map<String, Object> getAccessUserMapIfExist() {
        Object accessUser = getAccessUserIfExist();
        return isNull(accessUser) ? Collections.emptyMap() : new LinkedHashMap<>(BeanMap.toMap(accessUser));
    }

    public static Map<String, Object> getAccessUserMap() {
        Object accessUser = getAccessUser();
        return isNull(accessUser) ? Collections.emptyMap() : new LinkedHashMap<>(BeanMap.toMap(accessUser));
    }

    public static Object getAccessUser() {
        Object value = getCurrentThreadAccessUser();
        if (value == null && PlatformDependentUtil.EXIST_HTTP_SERVLET) {
            value = WebSecurityAccessFilter.getCurrentAccessUserIfExist();
            if (NULL == value) {
                return null;
            }
            if (null == value) {
                value = WebSecurityAccessFilter.createAccessUser();
            }
        }
        if (value == null && PlatformDependentUtil.EXIST_DUBBO_APACHE) {
            value = DubboAccessUserUtil.getApacheAccessUser();
        }
        if (value == null && PlatformDependentUtil.EXIST_DUBBO_ALIBABA) {
            value = DubboAccessUserUtil.getAlibabaAccessUser();
        }
        return value == NULL ? null : value;
    }

    public static Object getAccessUserIfExist() {
        Object value = getAccessUserIfExistNull();
        return value == NULL ? null : value;
    }

    public static Object getAccessUserIfExistNull() {
        Object value = getCurrentThreadAccessUser();
        if (value == null && PlatformDependentUtil.EXIST_HTTP_SERVLET) {
            value = WebSecurityAccessFilter.getCurrentAccessUserIfExist();
        }
        if (value == null && PlatformDependentUtil.EXIST_DUBBO_APACHE) {
            value = DubboAccessUserUtil.getApacheAccessUser();
        }
        if (value == null && PlatformDependentUtil.EXIST_DUBBO_ALIBABA) {
            value = DubboAccessUserUtil.getAlibabaAccessUser();
        }
        return value;
    }

    public static Object getAccessUserValue(String attrName) {
        Object value = getCurrentThreadAccessUserValue(attrName);
        if (value == null && PlatformDependentUtil.EXIST_HTTP_SERVLET) {
            value = getWebAccessUserValue(attrName);
        }
        if (value == null && PlatformDependentUtil.EXIST_DUBBO_APACHE) {
            value = DubboAccessUserUtil.getApacheAccessUserValue(attrName);
        }
        if (value == null && PlatformDependentUtil.EXIST_DUBBO_ALIBABA) {
            value = DubboAccessUserUtil.getAlibabaAccessUserValue(attrName);
        }
        return value;
    }

    public static <T> T getAccessUserValue(String attrName, Class<T> type) {
        Object value = getAccessUserValue(attrName);
        return TypeUtil.cast(value, type);
    }

    public static void setAccessUser(Object accessUser) {
        removeAccessUser();
        if (isNotNull(accessUser)) {
            if (PlatformDependentUtil.EXIST_HTTP_SERVLET && WebSecurityAccessFilter.isInLifecycle()) {
                WebSecurityAccessFilter.setCurrentUser(accessUser);
            } else {
                setCurrentThreadAccessUser(accessUser);
            }
        }
    }

    public static <ACCESS_USER> ACCESS_USER getCurrentThreadAccessUser() {
        Supplier<Object> supplier = ACCESS_USER_THREAD_LOCAL.get();
        return supplier != null ? (ACCESS_USER) supplier.get() : null;
    }

    public static void removeCurrentThreadAccessUser() {
        ACCESS_USER_THREAD_LOCAL.remove();
    }

    public static void setCurrentThreadAccessUser(Object accessUser) {
        ACCESS_USER_THREAD_LOCAL.set(() -> accessUser);
    }

    public static void setCurrentThreadAccessUserSupplier(Supplier accessUserSupplier) {
        ACCESS_USER_THREAD_LOCAL.set(accessUserSupplier);
    }

    public static <T> T getWebAccessUserValue(String attrName, Class<T> type) {
        Object value = getWebAccessUserValue(attrName);
        return TypeUtil.cast(value, type);
    }

    public static boolean setWebAccessUserValue(String attrName, Object value) {
        if (PlatformDependentUtil.EXIST_HTTP_SERVLET && WebSecurityAccessFilter.isInLifecycle()) {
            return WebSecurityAccessFilter.setCurrentAccessUserValue(attrName, value);
        } else {
            return false;
        }
    }

    public static boolean setDubboAccessUserValue(String attrName, Object value) {
        if (PlatformDependentUtil.EXIST_DUBBO_APACHE) {
            DubboAccessUserUtil.setApacheAccessUserValue(attrName, value);
            return true;
        } else if (PlatformDependentUtil.EXIST_DUBBO_ALIBABA) {
            DubboAccessUserUtil.setAlibabaAccessUserValue(attrName, value);
            return true;
        } else {
            return false;
        }
    }

    public static boolean setAccessUserValue(String attrName, Object value) {
        boolean setterSuccess;
        if (PlatformDependentUtil.EXIST_HTTP_SERVLET && WebSecurityAccessFilter.isInLifecycle()) {
            setterSuccess = WebSecurityAccessFilter.setCurrentAccessUserValue(attrName, value);
        } else {
            Object accessUser = getCurrentThreadAccessUser();
            setterSuccess = TypeUtil.invokeSetter(accessUser, attrName, value);
        }
        if (!setterSuccess) {
            setterSuccess = setDubboAccessUserValue(attrName, value);
        }
        return setterSuccess;
    }

    public static Object getCurrentThreadAccessUserValue(String attrName) {
        Object accessUser = getCurrentThreadAccessUser();
        Object value;
        if (isNull(accessUser)) {
            value = null;
        } else {
            Map accessUserGetterMap;
            if (accessUser instanceof Map) {
                accessUserGetterMap = (Map) accessUser;
            } else {
                accessUserGetterMap = new BeanMap(accessUser);
            }
            value = accessUserGetterMap.get(attrName);
        }
        return value;
    }

    public static Object getWebAccessUserValue(String attrName) {
        if (!PlatformDependentUtil.EXIST_HTTP_SERVLET) {
            return null;
        }
        Object value;
        Object accessUser = WebSecurityAccessFilter.getCurrentAccessUserIfCreate();
        if (isNull(accessUser)) {
            value = null;
        } else {
            Map accessUserGetterMap;
            if (accessUser instanceof Map) {
                accessUserGetterMap = (Map) accessUser;
            } else {
                accessUserGetterMap = new BeanMap(accessUser);
            }
            value = accessUserGetterMap.get(attrName);
        }
        return value;
    }

    public static void removeAccessUser() {
        removeCurrentThreadAccessUser();
        if (PlatformDependentUtil.EXIST_HTTP_SERVLET && WebSecurityAccessFilter.isInLifecycle()) {
            WebSecurityAccessFilter.removeCurrentUser();
        }
        if (PlatformDependentUtil.EXIST_DUBBO_APACHE) {
            DubboAccessUserUtil.removeApacheAccessUser();
        }
        if (PlatformDependentUtil.EXIST_DUBBO_ALIBABA) {
            DubboAccessUserUtil.removeAlibabaAccessUser();
        }
    }

    public static <T> T runOnAccessUser(Object accessUser, Callable<T> runnable) {
        if (accessUser == null) {
            accessUser = NULL;
        }
        Object oldAccessUser = getAccessUserIfExist();
        try {
            setAccessUser(accessUser);
            return runnable.call();
        } catch (Exception e) {
            PlatformDependentUtil.sneakyThrows(e);
            return null;
        } finally {
            setAccessUser(oldAccessUser);
        }
    }

    public static void runOnAccessUser(Object accessUser, Runnable runnable) {
        if (accessUser == null) {
            accessUser = NULL;
        }
        Object oldAccessUser = getAccessUserIfExistNull();
        try {
            setAccessUser(accessUser);
            runnable.run();
        } catch (Throwable e) {
            PlatformDependentUtil.sneakyThrows(e);
        } finally {
            setAccessUser(oldAccessUser);
        }
    }

    public static <T> AccessUserCompletableFuture<T> completableFuture() {
        return new AccessUserCompletableFuture<>();
    }

    public static <T> AccessUserCompletableFuture<T> completableFuture(CompletableFuture<T> future) {
        return new AccessUserCompletableFuture<>(future);
    }

    public static <T> AccessUserCompletableFuture<T> completableFuture(Object accessUser) {
        return new AccessUserCompletableFuture<>(accessUser);
    }

    public static <T> AccessUserCompletableFuture<T> completableFuture(Object accessUser, CompletableFuture<T> future) {
        return new AccessUserCompletableFuture<>(accessUser, future);
    }


    /**
     * 频繁切换用户时,可以用这个方便一些
     *
     * @see AccessUserUtil#open()
     * <pre>
     *       public static void main(String[] args) {
     *         AccessUserUtil.setAccessUser("abc");
     *         try (AccessUserTransaction transaction = AccessUserUtil.openTransaction()) {
     *             System.out.println("abc = " + AccessUserUtil.getAccessUser());
     *             transaction.begin(1);
     *             System.out.println("1 = " + AccessUserUtil.getAccessUser());
     *             transaction.begin(2);
     *             System.out.println("2 = " + AccessUserUtil.getAccessUser());
     *             transaction.end();
     *             System.out.println("1 = " + AccessUserUtil.getAccessUser());
     *             transaction.end();
     *
     *             System.out.println("abc = " + AccessUserUtil.getAccessUser());
     *
     *             transaction.begin(2);
     *             System.out.println("2 = " + AccessUserUtil.getAccessUser());
     *         }
     *         System.out.println("abc = " + AccessUserUtil.getAccessUser());
     *     }
     * </pre>
     */
    public static AccessUserTransaction open() {
        return new AccessUserTransaction();
    }

    @FunctionalInterface
    public interface Runnable {
        void run() throws Throwable;
    }

}
