package com.github.securityfilter.util;

import com.github.securityfilter.WebSecurityAccessFilter;

import java.util.Map;
import java.util.concurrent.Callable;

public class AccessUserUtil {

    public static boolean existAccessUser() {
        boolean exist = existWebAccessUser();
        if (!exist) {
            exist = existDubboAccessUser();
        }
        return exist;
    }

    public static boolean existWebAccessUser() {
        if (PlatformDependentUtil.EXIST_HTTP_SERVLET) {
            return null != WebSecurityAccessFilter.getCurrentAccessUserIfExist();
        } else {
            return false;
        }
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

    public static Object getAccessUser() {
        Object value = null;
        if (PlatformDependentUtil.EXIST_HTTP_SERVLET) {
            value = WebSecurityAccessFilter.getCurrentAccessUserIfCreate();
        }
        if (PlatformDependentUtil.EXIST_DUBBO_APACHE && value == null) {
            value = DubboAccessUserUtil.getApacheAccessUser();
        }
        if (PlatformDependentUtil.EXIST_DUBBO_ALIBABA && value == null) {
            value = DubboAccessUserUtil.getAlibabaAccessUser();
        }
        return value;
    }

    public static Object getAccessUserIfExist() {
        Object value = null;
        if (PlatformDependentUtil.EXIST_HTTP_SERVLET) {
            value = WebSecurityAccessFilter.getCurrentAccessUserIfExist();
        }
        if (PlatformDependentUtil.EXIST_DUBBO_APACHE && value == null) {
            value = DubboAccessUserUtil.getApacheAccessUser();
        }
        if (PlatformDependentUtil.EXIST_DUBBO_ALIBABA && value == null) {
            value = DubboAccessUserUtil.getAlibabaAccessUser();
        }
        return value;
    }

    public static Object getAccessUserValue(String attrName) {
        Object value = null;
        if (PlatformDependentUtil.EXIST_HTTP_SERVLET) {
            value = getWebAccessUserValue(attrName);
        }
        if (PlatformDependentUtil.EXIST_DUBBO_APACHE && value == null) {
            value = DubboAccessUserUtil.getApacheAccessUserValue(attrName);
        }
        if (PlatformDependentUtil.EXIST_DUBBO_ALIBABA && value == null) {
            value = DubboAccessUserUtil.getAlibabaAccessUserValue(attrName);
        }
        return value;
    }

    public static <T> T getAccessUserValue(String attrName, Class<T> type) {
        Object value = getAccessUserValue(attrName);
        return TypeUtil.cast(value, type);
    }

    public static boolean setAccessUser(Object accessUser) {
        boolean b = false;
        if (PlatformDependentUtil.EXIST_HTTP_SERVLET) {
            WebSecurityAccessFilter.setCurrentUser(accessUser);
            b = true;
        }
        if (PlatformDependentUtil.EXIST_DUBBO_APACHE) {
            if (accessUser == null) {
                DubboAccessUserUtil.removeApacheAccessUser();
            } else {
                DubboAccessUserUtil.setApacheAccessUser(accessUser);
            }
            b = true;
        } else if (PlatformDependentUtil.EXIST_DUBBO_ALIBABA) {
            if (accessUser == null) {
                DubboAccessUserUtil.removeAlibabaAccessUser();
            } else {
                DubboAccessUserUtil.setAlibabaAccessUser(accessUser);
            }
            b = true;
        }
        return b;
    }

    public static <T> T getWebAccessUserValue(String attrName, Class<T> type) {
        Object value = getWebAccessUserValue(attrName);
        return TypeUtil.cast(value, type);
    }

    public static boolean setWebAccessUserValue(String attrName, Object value) {
        if (PlatformDependentUtil.EXIST_HTTP_SERVLET) {
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
        boolean b = false;
        if (PlatformDependentUtil.EXIST_HTTP_SERVLET) {
            b = WebSecurityAccessFilter.setCurrentAccessUserValue(attrName, value);
        }
        if (PlatformDependentUtil.EXIST_DUBBO_APACHE) {
            DubboAccessUserUtil.setApacheAccessUserValue(attrName, value);
            b = true;
        } else if (PlatformDependentUtil.EXIST_DUBBO_ALIBABA) {
            DubboAccessUserUtil.setAlibabaAccessUserValue(attrName, value);
            b = true;
        }
        return b;
    }

    public static Object getWebAccessUserValue(String attrName) {
        if (!PlatformDependentUtil.EXIST_HTTP_SERVLET) {
            return null;
        }
        Object value;
        Object accessUser = WebSecurityAccessFilter.getCurrentAccessUserIfCreate();
        if (accessUser == null) {
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

    public static <T> T runOnAccessUser(Object accessUser, Callable<T> runnable) {
        Object oldAccessUser = getAccessUserIfExist();
        try {
            setAccessUser(accessUser);
            return runnable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            removeAccessUser();
            if (oldAccessUser != null) {
                setAccessUser(oldAccessUser);
            }
        }
    }

    public static void runOnAccessUser(Object accessUser, Runnable runnable) {
        Object oldAccessUser = getAccessUserIfExist();
        try {
            setAccessUser(accessUser);
            runnable.run();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            removeAccessUser();
            if (oldAccessUser != null) {
                setAccessUser(oldAccessUser);
            }
        }
    }

    @FunctionalInterface
    public interface Runnable {
        void run() throws Throwable;
    }

}
