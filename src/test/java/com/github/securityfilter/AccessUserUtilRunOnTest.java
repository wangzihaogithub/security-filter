package com.github.securityfilter;

import com.github.securityfilter.util.AccessUserUtil;

import java.util.concurrent.Executors;

public class AccessUserUtilRunOnTest {
    public static void main(String[] args) {

        AccessUserUtil.runOnAttribute("attr1", 1, AccessUserUtil.runnable0(() -> {
            System.out.println("attr1 = " + AccessUserUtil.getAccessUser());
            AccessUserUtil.runOnAttribute("attr2", 2, () -> {
                System.out.println("attr1-2 = " + AccessUserUtil.getAccessUser());
            });
            System.out.println("attr1 = " + AccessUserUtil.getAccessUser());

            Executors.newFixedThreadPool(1).execute(AccessUserUtil.runnable(() -> {
                System.out.println("attr1 = " + AccessUserUtil.getAccessUser());
            }));
        }));

        System.out.println("null = " + AccessUserUtil.getAccessUser());

        AccessUserUtil.runOnAccessUser(1, () -> {
            System.out.println("1 = " + AccessUserUtil.getAccessUser());

            AccessUserUtil.runOnAccessUser(2, () -> {
                System.out.println("2 = " + AccessUserUtil.getAccessUser());
                AccessUserUtil.runOnAccessUser(3, () -> {
                    System.out.println("3 = " + AccessUserUtil.getAccessUser());
                    AccessUserUtil.runOnRootAccessUser(() -> {
                        System.out.println("cancel = " + AccessUserUtil.getAccessUser());
                    });
                });
                System.out.println("2 = " + AccessUserUtil.getAccessUser());
            });

            System.out.println("1 = " + AccessUserUtil.getAccessUser());
        });


        AccessUserUtil.runOnAccessUser(11, () -> {
            System.out.println("11 = " + AccessUserUtil.getAccessUser());

            AccessUserUtil.runOnAttribute("22", 22, () -> {
                System.out.println("22 = " + AccessUserUtil.getAccessUser());
                AccessUserUtil.runOnAccessUser(3, () -> {
                    System.out.println("3 = " + AccessUserUtil.getAccessUser());
                });
                System.out.println("22 = " + AccessUserUtil.getAccessUser());
            });

            System.out.println("11 = " + AccessUserUtil.getAccessUser());
        });
    }
}
