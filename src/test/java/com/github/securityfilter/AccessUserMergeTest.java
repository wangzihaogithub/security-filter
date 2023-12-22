package com.github.securityfilter;

import com.github.securityfilter.util.AccessUserUtil;

import java.util.Collections;
import java.util.Map;

public class AccessUserMergeTest {
    public static void main(String[] args) {
        Map<String, Integer> user1 = Collections.singletonMap("a", 1);
        Map<String, Integer> user2 = Collections.singletonMap("b", 2);
        AccessUserUtil.setAccessUser(Collections.singletonMap("curr", 2));

        Object mergeCurrentAccessUser1 = AccessUserUtil.mergeCurrentAccessUser(user1);
        Object accessUser = AccessUserUtil.mergeAccessUser(user1, user2);
        Object mergeCurrentAccessUserMap = AccessUserUtil.mergeCurrentAccessUserMap(user1, user2);
        Object mergeAccessUserMap = AccessUserUtil.mergeAccessUserMap(user1, user2);
        Object mergeCurrentAccessUser = AccessUserUtil.mergeCurrentAccessUser(user1, user2);

        AccessUserUtil.runOnAccessUser(user2,()->{
            Object accessUser1 = AccessUserUtil.getAccessUser();
            System.out.println("accessUser1 = " + accessUser1);
        }, true);
        System.out.println("mergeCurrentAccessUser = " + mergeCurrentAccessUser);
    }
}
