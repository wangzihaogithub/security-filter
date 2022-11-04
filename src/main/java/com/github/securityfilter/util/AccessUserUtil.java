package com.github.securityfilter.util;

import com.github.securityfilter.WebSecurityAccessFilter;

import java.util.Map;
import java.util.Objects;

public class AccessUserUtil {

    public static Object getAccessUser() {
        Object value = null;
        if (Util.EXIST_HTTP_SERVLET) {
            value = WebSecurityAccessFilter.getCurrentAccessUserIfCreate();
        }
        if (Util.EXIST_DUBBO_APACHE && value == null) {
            value = DubboAccessUserUtil.getApacheAccessUser();
        }
        if (Util.EXIST_DUBBO_ALIBABA && value == null) {
            value = DubboAccessUserUtil.getAlibabaAccessUser();
        }
        return value;
    }

    public static Object getAccessUserValue(String attrName) {
        Object value = null;
        if (Util.EXIST_HTTP_SERVLET) {
            value = getWebAccessUserValue(attrName);
        }
        if (Util.EXIST_DUBBO_APACHE && value == null) {
            value = DubboAccessUserUtil.getApacheAccessUserValue(attrName);
        }
        if (Util.EXIST_DUBBO_ALIBABA && value == null) {
            value = DubboAccessUserUtil.getAlibabaAccessUserValue(attrName);
        }
        return value;
    }

    public static void setAccessUser(Object accessUser) {
        if (Util.EXIST_HTTP_SERVLET) {
            WebSecurityAccessFilter.setCurrentUser(accessUser);
        }
        if (Util.EXIST_DUBBO_APACHE) {
            if (accessUser == null) {
                DubboAccessUserUtil.removeApacheAccessUser();
            } else {
                DubboAccessUserUtil.setApacheAccessUser(accessUser);
            }
        } else if (Util.EXIST_DUBBO_ALIBABA) {
            if (accessUser == null) {
                DubboAccessUserUtil.removeAlibabaAccessUser();
            } else {
                DubboAccessUserUtil.setAlibabaAccessUser(accessUser);
            }
        }
    }

    public static String getWebAccessUserValue(String attrName) {
        if (!Util.EXIST_HTTP_SERVLET) {
            return null;
        }
        String value;
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
            value = Objects.toString(accessUserGetterMap.get(attrName), null);
        }
        return value;
    }

    public static void removeAccessUser() {
        setAccessUser(null);
    }

}
