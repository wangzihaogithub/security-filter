package com.github.securityfilter.util;

import com.github.securityfilter.WebSecurityAccessFilter;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class AccessUserUtil {
    public static boolean MERGE_USER = "true".equalsIgnoreCase(System.getProperty("AccessUserUtil.MERGE_USER", "false"));
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

    public static Object mergeCurrentAccessUser(Object accessUser) {
        Object accessUserIfExist = getAccessUserIfExistNull();
        if (isNull(accessUser)) {
            return isNull(accessUserIfExist) ? null : accessUserIfExist;
        }
        if (isNull(accessUserIfExist)) {
            return accessUser;
        }
        Map accessUserIfExistMap = BeanMap.toMap(accessUserIfExist);
        Map accessUserMap = BeanMap.toMap(accessUser);

        Map<String, Object> merge = new HashMap<>(accessUserIfExistMap);
        merge.putAll(accessUserMap);
        return merge;
    }

    public static Object mergeCurrentAccessUser(Object... accessUserList) {
        Object accessUserIfExist = getAccessUserIfExistNull();
        if (accessUserList == null || accessUserList.length == 0) {
            return isNull(accessUserIfExist) ? null : accessUserIfExist;
        }
        if (isNull(accessUserIfExist) && accessUserList.length == 1) {
            return accessUserList[0];
        }
        Map<String, Object> merge = isNull(accessUserIfExist) ? new HashMap<>() : new HashMap<>(BeanMap.toMap(accessUserIfExist));
        for (Object accessUser : accessUserList) {
            if (isNull(accessUser)) {
                continue;
            }
            Map accessUserMap = BeanMap.toMap(accessUser);
            merge.putAll(accessUserMap);
        }
        return merge;
    }

    public static Map<String, Object> mergeCurrentAccessUserMap(Object... accessUserList) {
        Object accessUserIfExist = getAccessUserIfExistNull();
        Map<String, Object> merge = isNull(accessUserIfExist) ? new HashMap<>() : new HashMap<>(BeanMap.toMap(accessUserIfExist));
        if (accessUserList != null && accessUserList.length > 0) {
            for (Object accessUser : accessUserList) {
                if (isNull(accessUser)) {
                    continue;
                }
                Map accessUserMap = BeanMap.toMap(accessUser);
                merge.putAll(accessUserMap);
            }
        }
        return merge;
    }

    public static Object mergeAccessUser(Object... accessUserList) {
        if (accessUserList == null || accessUserList.length == 0) {
            return null;
        }
        Object accessUser0 = accessUserList[0];
        if (isNull(accessUser0) && accessUserList.length == 1) {
            return accessUser0;
        }
        int count = 0;
        for (Object accessUser : accessUserList) {
            if (isNotNull(accessUser)) {
                count++;
            }
            if (count > 1) {
                break;
            }
        }
        if (count == 1) {
            for (Object accessUser : accessUserList) {
                if (isNotNull(accessUser)) {
                    return accessUser;
                }
            }
        }
        Map<String, Object> merge = new HashMap<>();
        for (Object accessUser : accessUserList) {
            if (isNull(accessUser)) {
                continue;
            }
            Map accessUserMap = BeanMap.toMap(accessUser);
            merge.putAll(accessUserMap);
        }
        return merge;
    }

    public static Object mergeAccessUserMap(Object... accessUserList) {
        if (accessUserList == null || accessUserList.length == 0) {
            return new HashMap<>(1);
        }
        Map<String, Object> merge = new HashMap<>();
        for (Object accessUser : accessUserList) {
            if (isNull(accessUser)) {
                continue;
            }
            Map accessUserMap = BeanMap.toMap(accessUser);
            merge.putAll(accessUserMap);
        }
        return merge;
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
        boolean setterSuccess = PlatformDependentUtil.EXIST_HTTP_SERVLET
                && WebSecurityAccessFilter.isInLifecycle()
                && WebSecurityAccessFilter.setCurrentAccessUserValue(attrName, value);
        if (setDubboAccessUserValue(attrName, value)) {
            setterSuccess = true;
        }
        Object accessUser = getCurrentThreadAccessUser();
        if (isNotNull(accessUser) && TypeUtil.invokeSetter(accessUser, attrName, value)) {
            setterSuccess = true;
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

    public static <T> T runOnAccessUser(Object accessUser, Callable0<T> runnable) {
        return runOnAccessUser(accessUser, runnable, MERGE_USER);
    }

    public static <T> T runOnAccessUser(Object accessUser, Callable0<T> runnable, boolean mergeCurrentUser) {
        if (accessUser == null) {
            accessUser = NULL;
        }
        Object oldAccessUser = getAccessUserIfExist();
        try {
            setAccessUser(mergeCurrentUser ? mergeAccessUser(oldAccessUser, accessUser) : accessUser);
            return runnable.call();
        } catch (Throwable e) {
            PlatformDependentUtil.sneakyThrows(e);
            return null;
        } finally {
            setAccessUser(oldAccessUser);
        }
    }

    public static void runOnAccessUser(Object accessUser, Runnable runnable) {
        runOnAccessUser(accessUser, runnable, MERGE_USER);
    }

    public static void runOnAccessUser(Object accessUser, Runnable runnable, boolean mergeCurrentUser) {
        if (accessUser == null) {
            accessUser = NULL;
        }
        Object oldAccessUser = getAccessUserIfExistNull();
        try {
            setAccessUser(mergeCurrentUser ? mergeAccessUser(oldAccessUser, accessUser) : accessUser);
            runnable.run();
        } catch (Throwable e) {
            PlatformDependentUtil.sneakyThrows(e);
        } finally {
            setAccessUser(oldAccessUser);
        }
    }

    public static <T> T runOnAttribute(String attrKey, Object attrValue, Callable0<T> runnable) {
        Object old = getAccessUserValue(attrKey);
        try {
            if (setAccessUserValue(attrKey, attrValue)) {
                return runnable.call();
            } else {
                throw new IllegalStateException("runOnAttribute setAccessUserValue(" + attrKey + ") fail!");
            }
        } catch (Throwable e) {
            PlatformDependentUtil.sneakyThrows(e);
            return null;
        } finally {
            setAccessUserValue(attrKey, old);
        }
    }

    public static void runOnAttribute(String attrKey, Object attrValue, Runnable runnable) {
        Object old = getAccessUserValue(attrKey);
        try {
            if (setAccessUserValue(attrKey, attrValue)) {
                runnable.run();
            } else {
                throw new IllegalStateException("runOnAttribute setAccessUserValue(" + attrKey + ") fail!");
            }
        } catch (Throwable e) {
            PlatformDependentUtil.sneakyThrows(e);
        } finally {
            setAccessUserValue(attrKey, old);
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
    public interface Callable0<V> {
        V call() throws Throwable;
    }

    @FunctionalInterface
    public interface Runnable {
        void run() throws Throwable;
    }

}
