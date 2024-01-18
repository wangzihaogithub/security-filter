package com.github.securityfilter.util;

import java.util.LinkedList;

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
    public static boolean MERGE_USER = "true".equalsIgnoreCase(System.getProperty("AccessUserTransaction.MERGE_USER", "false"));
    private final AccessUserSnapshot snapshot = AccessUserUtil.openSnapshot();
    private final LinkedList<Object> currentAccessUserList = new LinkedList<>();
    private boolean mergeUser = MERGE_USER;

    public void setMergeUser(boolean mergeUser) {
        this.mergeUser = mergeUser;
    }

    public boolean isMergeUser() {
        return this.mergeUser;
    }

    public Object begin(Object accessUser) {
        return begin(accessUser, mergeUser);
    }

    public Object begin(Object accessUser, boolean mergeUser) {
        Object old = currentAccessUserList.isEmpty() ? snapshot.getAccessUser() : currentAccessUserList.get(0);

        Object mergeAccessUser = mergeUser && AccessUserUtil.isNotNull(accessUser) ? AccessUserUtil.mergeAccessUser(old, accessUser) : accessUser;
        this.currentAccessUserList.addFirst(mergeAccessUser);
        snapshot.setAccessUser(mergeAccessUser, false);
        return old;
    }

    public Object end() {
        Object oldAccessUser;
        if (currentAccessUserList.isEmpty()) {
            oldAccessUser = snapshot.getAccessUser();
        } else {
            currentAccessUserList.removeFirst();
            oldAccessUser = currentAccessUserList.isEmpty() ? snapshot.getAccessUser() : currentAccessUserList.get(0);
        }
        snapshot.setAccessUser(oldAccessUser, false);
        return oldAccessUser;
    }

    public <T> T runOn(Object accessUser, AccessUserUtil.Callable0<T> callable) {
        try {
            begin(accessUser);
            return callable.call();
        } catch (Throwable e) {
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
        snapshot.close();
    }

}
