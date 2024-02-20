package com.github.securityfilter.util;

import com.github.securityfilter.WebSecurityAccessFilter;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.github.securityfilter.util.PlatformDependentUtil.ACCESS_USER_THREAD_LOCAL;
import static com.github.securityfilter.util.PlatformDependentUtil.SNAPSHOT_THREAD_LOCAL;

public interface AccessUserSnapshot extends AutoCloseable {

    enum TypeEnum {
        push,
        root,
        fork
    }

    static List<AccessUserSnapshot> list() {
        return Collections.unmodifiableList(SNAPSHOT_THREAD_LOCAL.get());
    }

    static boolean exist() {
        return !SNAPSHOT_THREAD_LOCAL.get().isEmpty();
    }

    static AccessUserSnapshot current() {
        LinkedList<AccessUserSnapshot> list = SNAPSHOT_THREAD_LOCAL.get();
        return list.isEmpty() ? null : list.getFirst();
    }

    static AccessUserSnapshot root() {
        LinkedList<AccessUserSnapshot> list = SNAPSHOT_THREAD_LOCAL.get();
        return list.isEmpty() ? null : list.getLast();
    }

    static AccessUserSnapshot get(int index) {
        LinkedList<AccessUserSnapshot> list = SNAPSHOT_THREAD_LOCAL.get();
        return list.isEmpty() || index < 0 || index >= list.size() ? null : list.get(index);
    }


    boolean isClose();

    boolean isNullToObject();

    Thread getThread();

    String getRequestId();

    String getCurrentRequestId();

    TypeEnum getTypeEnum();

    Object getAccessUser();

    Object getRootAccessUser();

    Object getCurrentAccessUser();

    Object getCurrentAccessUserValue(String attrName);

    void setAccessUser(Object accessUser, boolean mergeAccessUser);

    Map<String, String> getMdcMap();

    Map<String, String> getForkMdcMap();

    Map<String, String> getCurrentMdcMap();

    AccessUserSnapshot fork();

    Object getForkAccessUser();

    void setRequestId(String requestId);

    void setMdcMap(Map<String,String> mdcMap);

    @Override
    void close();

    static AccessUserSnapshot open() {
        return open(false, AccessUserSnapshot.TypeEnum.push, AccessUserUtil.getRootAccessUser(false, false));
    }

    static AccessUserSnapshot open(AccessUserSnapshot.TypeEnum typeEnum, Object rootAccessUser) {
        return open(false, typeEnum, rootAccessUser);
    }

    static AccessUserSnapshot open(boolean nullToObject, AccessUserSnapshot.TypeEnum typeEnum, Object rootAccessUser) {
        AccessUserSnapshot value;
        Supplier<Object> supplier = ACCESS_USER_THREAD_LOCAL.get();
        if (supplier != null) {
            // thread
            value = new AccessUserSnapshot.CurrentThreadLocal(supplier.get(), rootAccessUser, nullToObject, typeEnum);
        } else if (PlatformDependentUtil.EXIST_HTTP_SERVLET && WebSecurityAccessFilter.isInLifecycle()) {
            // web
            value = new AccessUserSnapshot.WebHttpServlet(WebSecurityAccessFilter.getCurrentAccessUserIfExist(), rootAccessUser, nullToObject, typeEnum);
        } else if (PlatformDependentUtil.EXIST_DUBBO_APACHE && DubboAccessUserUtil.isApacheAccessUser()) {
            // dubbo apache
            value = new AccessUserSnapshot.DubboApache(DubboAccessUserUtil.getApacheAccessUser(), rootAccessUser, nullToObject, typeEnum);
        } else if (PlatformDependentUtil.EXIST_DUBBO_ALIBABA && DubboAccessUserUtil.isAlibabaAccessUser()) {
            // dubbo alibaba
            value = new AccessUserSnapshot.DubboAlibaba(DubboAccessUserUtil.getAlibabaAccessUser(), rootAccessUser, nullToObject, typeEnum);
        } else {
            // NULL
            value = new AccessUserSnapshot.Null(rootAccessUser, nullToObject, typeEnum);
        }
        return value;
    }

    class CurrentThreadLocal extends AccessUserUtil.AbstractAccessUserSnapshot {
        public CurrentThreadLocal(Object accessUser, Object rootAccessUser, boolean nullToObject, TypeEnum typeEnum) {
            super(accessUser, rootAccessUser, nullToObject, typeEnum);
        }

        @Override
        protected Object getCurrentAccessUser0() {
            return AccessUserUtil.getCurrentThreadAccessUser();
        }

        @Override
        public Object getCurrentAccessUserValue(String attrName) {
            return AccessUserUtil.getCurrentThreadAccessUserValue(attrName);
        }

        @Override
        protected void setAccessUser0(Object accessUser) {
            AccessUserUtil.setCurrentThreadAccessUserSupplier(() -> accessUser);
        }

        @Override
        protected void close0() {
            AccessUserUtil.setCurrentThreadAccessUserSupplier(() -> accessUser);
        }

        @Override
        protected AccessUserSnapshot fork0() {
            return new CurrentThreadLocal(accessUser, rootAccessUser, nullToObject, typeEnum);
        }
    }

    class WebHttpServlet extends AccessUserUtil.AbstractAccessUserSnapshot {
        public WebHttpServlet(Object accessUser, Object rootAccessUser, boolean nullToObject, TypeEnum typeEnum) {
            super(accessUser, rootAccessUser, nullToObject, typeEnum);
        }

        @Override
        protected Object getCurrentAccessUser0() {
            return WebSecurityAccessFilter.getCurrentAccessUserIfExist();
        }

        @Override
        public Object getCurrentAccessUserValue(String attrName) {
            return AccessUserUtil.getWebAccessUserValue(attrName, false);
        }

        @Override
        protected void setAccessUser0(Object accessUser) {
            WebSecurityAccessFilter.setCurrentUser(accessUser);
        }

        @Override
        protected void close0() {
            WebSecurityAccessFilter.setCurrentUser(accessUser);
        }

        @Override
        protected AccessUserSnapshot fork0() {
            return new WebHttpServlet(accessUser, rootAccessUser, nullToObject, typeEnum);
        }
    }

    class DubboApache extends AccessUserUtil.AbstractAccessUserSnapshot {
        public DubboApache(Object accessUser, Object rootAccessUser, boolean nullToObject, TypeEnum typeEnum) {
            super(accessUser, rootAccessUser, nullToObject, typeEnum);
        }

        @Override
        protected Object getCurrentAccessUser0() {
            return DubboAccessUserUtil.getApacheAccessUser();
        }

        @Override
        public Object getCurrentAccessUserValue(String attrName) {
            return DubboAccessUserUtil.getApacheAccessUserValue(attrName);
        }

        @Override
        protected void setAccessUser0(Object accessUser) {
            DubboAccessUserUtil.setApacheAccessUser(accessUser);
        }

        @Override
        protected void close0() {
            DubboAccessUserUtil.setApacheAccessUser(accessUser);
        }

        @Override
        protected AccessUserSnapshot fork0() {
            return new DubboApache(accessUser, rootAccessUser, nullToObject, typeEnum);
        }
    }

    class DubboAlibaba extends AccessUserUtil.AbstractAccessUserSnapshot {
        public DubboAlibaba(Object accessUser, Object rootAccessUser, boolean nullToObject, TypeEnum typeEnum) {
            super(accessUser, rootAccessUser, nullToObject, typeEnum);
        }

        @Override
        protected Object getCurrentAccessUser0() {
            return DubboAccessUserUtil.getAlibabaAccessUser();
        }

        @Override
        public Object getCurrentAccessUserValue(String attrName) {
            return DubboAccessUserUtil.getAlibabaAccessUserValue(attrName);
        }

        @Override
        protected void setAccessUser0(Object accessUser) {
            DubboAccessUserUtil.setAlibabaAccessUser(accessUser);
        }

        @Override
        protected void close0() {
            DubboAccessUserUtil.setAlibabaAccessUser(accessUser);
        }

        @Override
        protected AccessUserSnapshot fork0() {
            return new DubboAlibaba(accessUser, rootAccessUser, nullToObject, typeEnum);
        }
    }

    class Null extends AccessUserUtil.AbstractAccessUserSnapshot {
        public Null(Object rootAccessUser, boolean nullToObject, TypeEnum typeEnum) {
            super(null, rootAccessUser, nullToObject, typeEnum);
        }

        @Override
        protected Object getCurrentAccessUser0() {
            return AccessUserUtil.getCurrentThreadAccessUser();
        }

        @Override
        public Object getCurrentAccessUserValue(String attrName) {
            return AccessUserUtil.getCurrentThreadAccessUserValue(attrName);
        }

        @Override
        protected void setAccessUser0(Object accessUser) {
            AccessUserUtil.setCurrentThreadAccessUserSupplier(() -> accessUser);
        }

        @Override
        protected void close0() {
            AccessUserUtil.removeCurrentThreadAccessUser();
        }

        @Override
        protected AccessUserSnapshot fork0() {
            return new Null(rootAccessUser, nullToObject, typeEnum);
        }
    }

}
