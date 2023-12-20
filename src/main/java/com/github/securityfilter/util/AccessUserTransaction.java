package com.github.securityfilter.util;

import java.util.LinkedList;
import java.util.concurrent.Callable;

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
public class AccessUserTransaction implements AutoCloseable {
    private final Object oldAccessUser = AccessUserUtil.getAccessUserIfExistNull();
    private final LinkedList<Object> currentAccessUserList = new LinkedList<>();

    public Object begin(Object accessUser) {
        Object old = currentAccessUserList.isEmpty() ? null : currentAccessUserList.get(0);
        this.currentAccessUserList.addFirst(accessUser);
        AccessUserUtil.setAccessUser(accessUser);
        return old;
    }

    public Object end() {
        Object oldAccessUser;
        if (currentAccessUserList.isEmpty()) {
            oldAccessUser = this.oldAccessUser;
        } else {
            currentAccessUserList.removeFirst();
            oldAccessUser = currentAccessUserList.isEmpty() ? this.oldAccessUser : currentAccessUserList.get(0);
        }
        AccessUserUtil.setAccessUser(oldAccessUser);
        return oldAccessUser;
    }

    public <T> T runOn(Object accessUser, Callable<T> callable) {
        try {
            begin(accessUser);
            return callable.call();
        } catch (Exception e) {
            PlatformDependentUtil.sneakyThrows(e);
            return null;
        } finally {
            end();
        }
    }

    public void runOn(Object accessUser, AccessUserUtil.Runnable runnable) {
        try {
            begin(accessUser);
            runnable.run();
        } catch (Throwable e) {
            PlatformDependentUtil.sneakyThrows(e);
        } finally {
            end();
        }
    }

    @Override
    public void close() {
        AccessUserUtil.setAccessUser(oldAccessUser);
    }

}
