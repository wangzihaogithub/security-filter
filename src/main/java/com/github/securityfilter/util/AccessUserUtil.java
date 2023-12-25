package com.github.securityfilter.util;

import com.github.securityfilter.WebSecurityAccessFilter;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static com.github.securityfilter.util.PlatformDependentUtil.ACCESS_USER_THREAD_LOCAL;
import static com.github.securityfilter.util.TypeUtil.initialCapacity;

public class AccessUserUtil {
    public static boolean MERGE_USER = "true".equalsIgnoreCase(System.getProperty("AccessUserUtil.MERGE_USER", "false"));
    public static final Object NULL = new Object();

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
            return isNull(accessUser) ? null : accessUser;
        }
        Map accessUserIfExistMap = BeanMap.toMap(accessUserIfExist);
        Map accessUserMap = BeanMap.toMap(accessUser);
        Map<String, Object> merge = new HashMap<>(accessUserIfExistMap);
        merge.putAll(accessUserMap);
        return merge.isEmpty() ? null : merge;
    }

    public static Object mergeCurrentAccessUser(Object... accessUserList) {
        Object accessUserIfExist = getAccessUserIfExistNull();
        if (accessUserList == null || accessUserList.length == 0) {
            return isNull(accessUserIfExist) ? null : accessUserIfExist;
        }
        if (isNull(accessUserIfExist) && accessUserList.length == 1) {
            Object accessUser0 = accessUserList[0];
            return isNull(accessUser0) ? null : accessUser0;
        }
        Map<String, Object> merge = isNull(accessUserIfExist) ? new HashMap<>() : new HashMap<>(BeanMap.toMap(accessUserIfExist));
        for (Object accessUser : accessUserList) {
            if (isNull(accessUser)) {
                continue;
            }
            Map accessUserMap = BeanMap.toMap(accessUser);
            merge.putAll(accessUserMap);
        }
        return merge.isEmpty() ? null : merge;
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
        return merge.isEmpty() ? null : merge;
    }

    public static Object mergeAccessUser(Object... accessUserList) {
        if (accessUserList == null || accessUserList.length == 0) {
            return null;
        }
        Object accessUser0 = accessUserList[0];
        if (isNull(accessUser0) && accessUserList.length == 1) {
            return null;
        }
        int count = 0;
        Object notNullAccessUser = null;
        for (Object accessUser : accessUserList) {
            if (isNotNull(accessUser)) {
                count++;
                notNullAccessUser = accessUser;
            }
            if (count > 1) {
                break;
            }
        }
        if (count == 0) {
            return null;
        } else if (count == 1) {
            return notNullAccessUser;
        }
        Map<String, Object> merge = new HashMap<>(8);
        for (Object accessUser : accessUserList) {
            if (isNull(accessUser)) {
                continue;
            }
            Map accessUserMap = BeanMap.toMap(accessUser);
            merge.putAll(accessUserMap);
        }
        return merge.isEmpty() ? null : merge;
    }

    public static Object mergeAccessUser(Object accessUser1, Object accessUser2) {
        if (isNull(accessUser1) && isNull(accessUser2)) {
            return null;
        }
        if (isNull(accessUser1)) {
            return accessUser2;
        }
        if (isNull(accessUser2)) {
            return accessUser1;
        }
        Map accessUser1Map = BeanMap.toMap(accessUser1);
        Map accessUser2Map = BeanMap.toMap(accessUser2);

        Map<String, Object> merge = new HashMap<>(initialCapacity(accessUser1Map.size()));
        merge.putAll(accessUser1Map);
        merge.putAll(accessUser2Map);
        return merge.isEmpty() ? null : merge;
    }

    public static Map<String, Object> mergeAccessUserMap(Object... accessUserList) {
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

    public static boolean existCurrentThreadAccessUser() {
        return isNotNull(getCurrentThreadAccessUser());
    }

    public static boolean isCurrentThreadAccessUser() {
        return ACCESS_USER_THREAD_LOCAL.get() != null;
    }

    public static boolean existWebAccessUser() {
        return PlatformDependentUtil.EXIST_HTTP_SERVLET
                && isNotNull(WebSecurityAccessFilter.getCurrentAccessUserIfExist());
    }

    public static boolean existDubboAccessUser() {
        if (PlatformDependentUtil.EXIST_DUBBO_APACHE && DubboAccessUserUtil.isApacheAccessUser()) {
            return DubboAccessUserUtil.existApacheAccessUser();
        } else if (PlatformDependentUtil.EXIST_DUBBO_ALIBABA && DubboAccessUserUtil.isAlibabaAccessUser()) {
            return DubboAccessUserUtil.existAlibabaAccessUser();
        } else {
            return false;
        }
    }

    public static Map<String, Object> getAccessUserMapIfExist() {
        Object accessUser = getAccessUserIfExist();
        return isNull(accessUser) ? new LinkedHashMap<>(1) : new LinkedHashMap<>(BeanMap.toMap(accessUser));
    }

    public static Map<String, Object> getAccessUserMap() {
        Object accessUser = getAccessUser();
        return isNull(accessUser) ? new LinkedHashMap<>(1) : new LinkedHashMap<>(BeanMap.toMap(accessUser));
    }

    public static <T> T getAccessUserValue(String attrName, Class<T> type) {
        Object value = getAccessUserValue(attrName);
        return TypeUtil.cast(value, type);
    }

    public static <ACCESS_USER> ACCESS_USER getCurrentThreadAccessUser() {
        Supplier<Object> supplier = ACCESS_USER_THREAD_LOCAL.get();
        return supplier != null ? (ACCESS_USER) supplier.get() : null;
    }

    public static void removeCurrentThreadAccessUser() {
        ACCESS_USER_THREAD_LOCAL.remove();
    }

    public static void setCurrentThreadAccessUser(Object accessUser) {
        if (accessUser == null) {
            ACCESS_USER_THREAD_LOCAL.remove();
        } else {
            ACCESS_USER_THREAD_LOCAL.set(() -> accessUser);
        }
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
        if (PlatformDependentUtil.EXIST_DUBBO_APACHE && DubboAccessUserUtil.isApacheAccessUser()) {
            DubboAccessUserUtil.setApacheAccessUserValue(attrName, value);
            return true;
        } else if (PlatformDependentUtil.EXIST_DUBBO_ALIBABA && DubboAccessUserUtil.isAlibabaAccessUser()) {
            DubboAccessUserUtil.setAlibabaAccessUserValue(attrName, value);
            return true;
        } else {
            return false;
        }
    }

    private static Object getAttrValue(Object accessUser, String attrName) {
        if (isNull(accessUser)) {
            return null;
        }
        Map accessUserGetterMap;
        if (accessUser instanceof Map) {
            accessUserGetterMap = (Map) accessUser;
        } else {
            accessUserGetterMap = new BeanMap(accessUser);
        }
        return accessUserGetterMap.get(attrName);
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
        return getWebAccessUserValue(attrName, true);
    }

    public static Object getWebAccessUserValue(String attrName, boolean create) {
        if (!PlatformDependentUtil.EXIST_HTTP_SERVLET) {
            return null;
        }
        Object value;
        Object accessUser = create ? WebSecurityAccessFilter.getCurrentAccessUserIfCreate() : WebSecurityAccessFilter.getCurrentAccessUserIfExist();
        ;
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
        setAccessUser(null);
    }

    public static void clearAccessUser() {
        removeCurrentThreadAccessUser();
        if (PlatformDependentUtil.EXIST_HTTP_SERVLET) {
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

    public static void runOnAccessUser(Object accessUser, Runnable runnable) {
        runOnAccessUser(accessUser, runnable, MERGE_USER);
    }

    public static <T> T runOnAccessUser(Object accessUser, Callable0<T> runnable, boolean mergeCurrentUser) {
        try (AccessUserCloseable closeable = getAccessUserCloseableIfExistNull()) {
            closeable.setAccessUser(accessUser, mergeCurrentUser);
            return runnable.call();
        } catch (Throwable e) {
            PlatformDependentUtil.sneakyThrows(e);
            return null;
        }
    }

    public static void runOnNull(AccessUserUtil.Runnable runnable) {
        runOnAccessUser(null, runnable, false);
    }

    public static <T> T runOnNull(AccessUserUtil.Callable0<T> runnable) {
        return runOnAccessUser(null, runnable, false);
    }

    public static void runOnAccessUser(Object accessUser, Runnable runnable, boolean mergeCurrentUser) {
        try (AccessUserCloseable closeable = getAccessUserCloseableIfExistNull()) {
            closeable.setAccessUser(accessUser, mergeCurrentUser);
            runnable.run();
        } catch (Throwable e) {
            PlatformDependentUtil.sneakyThrows(e);
        }
    }

    public static <T> T runOnAttribute(String attrKey, Object attrValue, Callable0<T> runnable) {
        return runOnAccessUser(Collections.singletonMap(attrKey, attrValue), runnable, true);
    }

    public static void runOnAttribute(String attrKey, Object attrValue, Runnable runnable) {
        runOnAccessUser(Collections.singletonMap(attrKey, attrValue), runnable, true);
    }

    public static <T> T runOnSetAttribute(String attrKey, Object attrValue, Callable0<T> runnable) {
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

    public static void runOnSetAttribute(String attrKey, Object attrValue, Runnable runnable) {
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

    public static boolean existAccessUser() {
        Supplier<Object> supplier = ACCESS_USER_THREAD_LOCAL.get();
        if (supplier != null) {
            // thread
            return isNotNull(supplier.get());
        } else {
            if (PlatformDependentUtil.EXIST_HTTP_SERVLET && WebSecurityAccessFilter.isInLifecycle()) {
                // web
                return isNotNull(WebSecurityAccessFilter.getCurrentAccessUserIfExist());
            } else {
                // dubbo
                if (PlatformDependentUtil.EXIST_DUBBO_APACHE && DubboAccessUserUtil.isApacheAccessUser()) {
                    return isNotNull(DubboAccessUserUtil.getApacheAccessUser());
                } else if (PlatformDependentUtil.EXIST_DUBBO_ALIBABA && DubboAccessUserUtil.isAlibabaAccessUser()) {
                    return isNotNull(DubboAccessUserUtil.getAlibabaAccessUser());
                } else {
                    return false;
                }
            }
        }
    }

    /**
     * @return 会将 {@link #NULL}对象，转为null
     */
    public static Object getAccessUser() {
        Object user = getAccessUserNull(true);
        return isNull(user) ? null : user;
    }

    public static Object getAccessUserIfExist() {
        Object user = getAccessUserNull(false);
        return isNull(user) ? null : user;
    }

    public static Object getAccessUserIfExistNull() {
        return getAccessUserNull(false);
    }

    /**
     * @return 会返回 {@link #NULL}
     */
    public static AccessUserCloseable getAccessUserCloseableIfExistNull() {
        return getAccessUserCloseable(true);
    }

    /**
     * @return 会将 {@link #NULL}对象，转为null
     */
    public static AccessUserCloseable getAccessUserCloseable() {
        return getAccessUserCloseable(false);
    }

    private static AccessUserCloseable getAccessUserCloseable(boolean nullToObject) {
        AccessUserCloseable value;
        Supplier<Object> supplier = ACCESS_USER_THREAD_LOCAL.get();
        if (supplier != null) {
            // thread
            value = new AccessUserCloseable.CurrentThreadCloseable(supplier.get(), nullToObject);
        } else if (PlatformDependentUtil.EXIST_HTTP_SERVLET && WebSecurityAccessFilter.isInLifecycle()) {
            // web
            value = new AccessUserCloseable.WebCloseable(WebSecurityAccessFilter.getCurrentAccessUserIfExist(), nullToObject);
        } else if (PlatformDependentUtil.EXIST_DUBBO_APACHE && DubboAccessUserUtil.isApacheAccessUser()) {
            // dubbo apache
            value = new AccessUserCloseable.DubboApacheCloseable(DubboAccessUserUtil.getApacheAccessUser(), nullToObject);
        } else if (PlatformDependentUtil.EXIST_DUBBO_ALIBABA && DubboAccessUserUtil.isAlibabaAccessUser()) {
            // dubbo alibaba
            value = new AccessUserCloseable.DubboAlibabaCloseable(DubboAccessUserUtil.getAlibabaAccessUser(), nullToObject);
        } else {
            // NULL
            value = new AccessUserCloseable.NullCloseable();
        }
        return value;
    }

    public static Object getAccessUserValue(String attrName) {
        AccessUserCloseable currentCloseable = AccessUserCloseable.current();
        Object value;
        if (currentCloseable != null) {
            value = currentCloseable.getCurrentAccessUserValue(attrName);
        } else {
            Supplier<Object> supplier = ACCESS_USER_THREAD_LOCAL.get();
            if (supplier != null) {
                // thread
                value = getAttrValue(supplier.get(), attrName);
            } else {
                if (PlatformDependentUtil.EXIST_HTTP_SERVLET && WebSecurityAccessFilter.isInLifecycle()) {
                    // web
                    value = getWebAccessUserValue(attrName);
                } else {
                    // dubbo
                    if (PlatformDependentUtil.EXIST_DUBBO_APACHE && DubboAccessUserUtil.isApacheAccessUser()) {
                        value = DubboAccessUserUtil.getApacheAccessUserValue(attrName);
                    } else if (PlatformDependentUtil.EXIST_DUBBO_ALIBABA && DubboAccessUserUtil.isAlibabaAccessUser()) {
                        value = DubboAccessUserUtil.getAlibabaAccessUserValue(attrName);
                    } else {
                        value = null;
                    }
                }
            }
        }
        return value;
    }

    public static Object getAccessUserNull(boolean create) {
        Object value;
        AccessUserCloseable currentCloseable = AccessUserCloseable.current();
        if (currentCloseable != null) {
            value = currentCloseable.getCurrentAccessUser();
        } else {
            Supplier<Object> supplier = ACCESS_USER_THREAD_LOCAL.get();
            if (supplier != null) {
                // thread
                value = supplier.get();
            } else {
                if (PlatformDependentUtil.EXIST_HTTP_SERVLET && WebSecurityAccessFilter.isInLifecycle()) {
                    // web
                    value = WebSecurityAccessFilter.getCurrentAccessUserIfExist();
                    if (NULL == value) {
                        return NULL;
                    }
                    if (create && null == value) {
                        // web create
                        value = WebSecurityAccessFilter.createAccessUser();
                    }
                } else {
                    // dubbo
                    if (PlatformDependentUtil.EXIST_DUBBO_APACHE && DubboAccessUserUtil.isApacheAccessUser()) {
                        value = DubboAccessUserUtil.getApacheAccessUser();
                    } else if (PlatformDependentUtil.EXIST_DUBBO_ALIBABA && DubboAccessUserUtil.isAlibabaAccessUser()) {
                        value = DubboAccessUserUtil.getAlibabaAccessUser();
                    } else {
                        value = null;
                    }
                }
            }
        }
        return value;
    }

    public static boolean setAccessUser(Object accessUser) {
        if (isCurrentThreadAccessUser()) {
            // thread
            if (accessUser == null) {
                removeCurrentThreadAccessUser();
            } else {
                setCurrentThreadAccessUser(accessUser);
            }
            return true;
        } else {
            if (PlatformDependentUtil.EXIST_HTTP_SERVLET && WebSecurityAccessFilter.isInLifecycle()) {
                // web
                if (accessUser == null) {
                    WebSecurityAccessFilter.removeCurrentUser();
                } else {
                    WebSecurityAccessFilter.setCurrentUser(accessUser);
                }
                return true;
            } else {
                // dubbo
                if (PlatformDependentUtil.EXIST_DUBBO_APACHE && DubboAccessUserUtil.isApacheAccessUser()) {
                    if (accessUser == null) {
                        DubboAccessUserUtil.removeApacheAccessUser();
                    } else {
                        DubboAccessUserUtil.setApacheAccessUser(accessUser);
                    }
                    return true;
                } else if (PlatformDependentUtil.EXIST_DUBBO_ALIBABA && DubboAccessUserUtil.isAlibabaAccessUser()) {
                    if (accessUser == null) {
                        DubboAccessUserUtil.removeAlibabaAccessUser();
                    } else {
                        DubboAccessUserUtil.setAlibabaAccessUser(accessUser);
                    }
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    public static boolean setAccessUserValue(String attrName, Object value) {
        boolean setterSuccess;
        if (isCurrentThreadAccessUser()) {
            // thread
            Object accessUser = getCurrentThreadAccessUser();
            if (isNull(accessUser)) {
                Map<String, Object> map = new LinkedHashMap<>(1);
                map.put(attrName, value);
                setCurrentThreadAccessUser(map);
                setterSuccess = true;
            } else {
                setterSuccess = TypeUtil.invokeSetter(accessUser, attrName, value);
            }
        } else {
            if (PlatformDependentUtil.EXIST_HTTP_SERVLET && WebSecurityAccessFilter.isInLifecycle()) {
                // web
                setterSuccess = WebSecurityAccessFilter.setCurrentAccessUserValue(attrName, value);
            } else {
                if (PlatformDependentUtil.EXIST_DUBBO_APACHE && DubboAccessUserUtil.isApacheAccessUser()) {
                    // dubbo apache
                    DubboAccessUserUtil.setApacheAccessUserValue(attrName, value);
                    setterSuccess = true;
                } else if (PlatformDependentUtil.EXIST_DUBBO_ALIBABA && DubboAccessUserUtil.isAlibabaAccessUser()) {
                    // dubbo alibaba
                    DubboAccessUserUtil.setAlibabaAccessUserValue(attrName, value);
                    setterSuccess = true;
                } else {
                    // null
                    setterSuccess = false;
                }
            }
        }
        return setterSuccess;
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
