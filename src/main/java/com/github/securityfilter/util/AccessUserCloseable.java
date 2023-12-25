package com.github.securityfilter.util;

import com.github.securityfilter.WebSecurityAccessFilter;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.securityfilter.util.PlatformDependentUtil.CLOSEABLE_THREAD_LOCAL;

public interface AccessUserCloseable extends AutoCloseable {

    static boolean exist() {
        LinkedList<AccessUserCloseable> list = CLOSEABLE_THREAD_LOCAL.get();
        return !list.isEmpty();
    }

    static AccessUserCloseable current() {
        LinkedList<AccessUserCloseable> list = CLOSEABLE_THREAD_LOCAL.get();
        return list.isEmpty() ? null : list.getFirst();
    }

    static AccessUserCloseable first() {
        LinkedList<AccessUserCloseable> list = CLOSEABLE_THREAD_LOCAL.get();
        return list.isEmpty() ? null : list.getLast();
    }

    static AccessUserCloseable index(int index) {
        LinkedList<AccessUserCloseable> list = CLOSEABLE_THREAD_LOCAL.get();
        return list.isEmpty() || index < 0 || index >= list.size() ? null : list.get(index);
    }

    Object getAccessUser();

    Object getCurrentAccessUser();

    Object getCurrentAccessUserValue(String attrName);

    void setAccessUser(Object accessUser, boolean mergeAccessUser);

    @Override
    void close();

    static Object mergeAccessUser(Object accessUser1, Object accessUser2) {
        return AccessUserUtil.mergeAccessUser(accessUser1, accessUser2);
    }

    class CurrentThreadCloseable implements AccessUserCloseable {
        private final Object accessUser;
        private final Thread thread = Thread.currentThread();
        private final boolean nullToObject;
        private final AtomicBoolean close = new AtomicBoolean();

        public CurrentThreadCloseable(Object accessUser, boolean nullToObject) {
            this.accessUser = accessUser;
            this.nullToObject = nullToObject;
            CLOSEABLE_THREAD_LOCAL.get().addFirst(this);
        }

        @Override
        public Object getAccessUser() {
            if (nullToObject) {
                return accessUser;
            } else {
                return AccessUserUtil.isNull(accessUser) ? null : accessUser;
            }
        }

        @Override
        public Object getCurrentAccessUser() {
            return AccessUserUtil.getCurrentThreadAccessUser();
        }

        @Override
        public Object getCurrentAccessUserValue(String attrName) {
            return AccessUserUtil.getCurrentThreadAccessUserValue(attrName);
        }

        @Override
        public void setAccessUser(Object accessUser, boolean mergeAccessUser) {
            if (close.get()) {
                throw new IllegalStateException("close");
            }
            Object setterAccessUser = mergeAccessUser ? mergeAccessUser(this.accessUser, accessUser) : accessUser;
            if (setterAccessUser == null) {
                setterAccessUser = AccessUserUtil.NULL;
            }
            AccessUserUtil.setCurrentThreadAccessUser(setterAccessUser);
        }

        @Override
        public void close() {
            if (thread != Thread.currentThread()) {
                throw new IllegalStateException("thread != Thread.currentThread(). get=" + thread + "close=" + Thread.currentThread());
            }
            if (close.compareAndSet(false, true)) {
                CLOSEABLE_THREAD_LOCAL.get().removeFirst();
                AccessUserUtil.setCurrentThreadAccessUser(accessUser);
            }
        }
    }

    class WebCloseable implements AccessUserCloseable {
        private final Object accessUser;
        private final Thread thread = Thread.currentThread();
        private final boolean replaceNull;
        private final AtomicBoolean close = new AtomicBoolean();

        public WebCloseable(Object accessUser, boolean replaceNull) {
            this.accessUser = accessUser;
            this.replaceNull = replaceNull;
            CLOSEABLE_THREAD_LOCAL.get().addFirst(this);
        }

        @Override
        public Object getAccessUser() {
            if (replaceNull) {
                return accessUser;
            } else {
                return AccessUserUtil.isNull(accessUser) ? null : accessUser;
            }
        }

        @Override
        public Object getCurrentAccessUser() {
            return WebSecurityAccessFilter.getCurrentAccessUserIfExist();
        }

        @Override
        public Object getCurrentAccessUserValue(String attrName) {
            return AccessUserUtil.getWebAccessUserValue(attrName, false);
        }

        @Override
        public void setAccessUser(Object accessUser, boolean mergeAccessUser) {
            if (close.get()) {
                throw new IllegalStateException("close");
            }
            WebSecurityAccessFilter.setCurrentUser(mergeAccessUser ? mergeAccessUser(this.accessUser, accessUser) : accessUser);
        }

        @Override
        public void close() {
            if (thread != Thread.currentThread()) {
                throw new IllegalStateException("thread != Thread.currentThread(). get=" + thread + "close=" + Thread.currentThread());
            }

            if (close.compareAndSet(false, true)) {
                CLOSEABLE_THREAD_LOCAL.get().removeFirst();
                WebSecurityAccessFilter.setCurrentUser(accessUser);
            }
        }
    }

    class DubboApacheCloseable implements AccessUserCloseable {
        private final Object accessUser;
        private final Thread thread = Thread.currentThread();
        private final boolean replaceNull;
        private final AtomicBoolean close = new AtomicBoolean();

        public DubboApacheCloseable(Object accessUser, boolean replaceNull) {
            this.accessUser = accessUser;
            this.replaceNull = replaceNull;
            CLOSEABLE_THREAD_LOCAL.get().addFirst(this);
        }

        @Override
        public Object getAccessUser() {
            if (replaceNull) {
                return accessUser;
            } else {
                return AccessUserUtil.isNull(accessUser) ? null : accessUser;
            }
        }

        @Override
        public Object getCurrentAccessUser() {
            return DubboAccessUserUtil.getApacheAccessUser();
        }

        @Override
        public Object getCurrentAccessUserValue(String attrName) {
            return DubboAccessUserUtil.getApacheAccessUserValue(attrName);
        }

        @Override
        public void setAccessUser(Object accessUser, boolean mergeAccessUser) {
            if (close.get()) {
                throw new IllegalStateException("close");
            }
            DubboAccessUserUtil.setApacheAccessUser(mergeAccessUser ? mergeAccessUser(this.accessUser, accessUser) : accessUser);
        }

        @Override
        public void close() {
            if (thread != Thread.currentThread()) {
                throw new IllegalStateException("thread != Thread.currentThread(). get=" + thread + "close=" + Thread.currentThread());
            }
            if (close.compareAndSet(false, true)) {
                CLOSEABLE_THREAD_LOCAL.get().removeFirst();
                DubboAccessUserUtil.setApacheAccessUser(accessUser);
            }
        }
    }

    class DubboAlibabaCloseable implements AccessUserCloseable {
        private final Object accessUser;
        private final Thread thread = Thread.currentThread();
        private final boolean replaceNull;
        private final AtomicBoolean close = new AtomicBoolean();

        public DubboAlibabaCloseable(Object accessUser, boolean replaceNull) {
            this.accessUser = accessUser;
            this.replaceNull = replaceNull;
            CLOSEABLE_THREAD_LOCAL.get().addFirst(this);
        }

        @Override
        public Object getAccessUser() {
            if (replaceNull) {
                return accessUser;
            } else {
                return AccessUserUtil.isNull(accessUser) ? null : accessUser;
            }
        }

        @Override
        public Object getCurrentAccessUser() {
            return DubboAccessUserUtil.getAlibabaAccessUser();
        }

        @Override
        public Object getCurrentAccessUserValue(String attrName) {
            return DubboAccessUserUtil.getAlibabaAccessUserValue(attrName);
        }

        @Override
        public void setAccessUser(Object accessUser, boolean mergeAccessUser) {
            if (close.get()) {
                throw new IllegalStateException("close");
            }
            DubboAccessUserUtil.setAlibabaAccessUser(mergeAccessUser ? mergeAccessUser(this.accessUser, accessUser) : accessUser);
        }

        @Override
        public void close() {
            if (thread != Thread.currentThread()) {
                throw new IllegalStateException("thread != Thread.currentThread(). get=" + thread + "close=" + Thread.currentThread());
            }
            if (close.compareAndSet(false, true)) {
                CLOSEABLE_THREAD_LOCAL.get().removeFirst();
                DubboAccessUserUtil.setAlibabaAccessUser(accessUser);
            }
        }
    }

    class NullCloseable implements AccessUserCloseable {
        private final Thread thread = Thread.currentThread();
        private final AtomicBoolean close = new AtomicBoolean();

        public NullCloseable() {
            CLOSEABLE_THREAD_LOCAL.get().addFirst(this);
        }

        @Override
        public Object getAccessUser() {
            return null;
        }

        @Override
        public Object getCurrentAccessUser() {
            return AccessUserUtil.getCurrentThreadAccessUser();
        }

        @Override
        public Object getCurrentAccessUserValue(String attrName) {
            return AccessUserUtil.getCurrentThreadAccessUserValue(attrName);
        }

        @Override
        public void setAccessUser(Object accessUser, boolean mergeAccessUser) {
            if (close.get()) {
                throw new IllegalStateException("close");
            }
            Object setterAccessUser = accessUser;
            if (setterAccessUser == null) {
                setterAccessUser = AccessUserUtil.NULL;
            }
            AccessUserUtil.setCurrentThreadAccessUser(setterAccessUser);
        }

        @Override
        public void close() {
            if (thread != Thread.currentThread()) {
                throw new IllegalStateException("thread != Thread.currentThread(). get=" + thread + "close=" + Thread.currentThread());
            }
            if (close.compareAndSet(false, true)) {
                CLOSEABLE_THREAD_LOCAL.get().removeFirst();
                AccessUserUtil.removeCurrentThreadAccessUser();
            }
        }
    }

}
