package com.github.securityfilter;

import com.github.securityfilter.util.AccessUserTransaction;
import com.github.securityfilter.util.AccessUserUtil;

public class AccessUserTransactionTest {
    public static void main(String[] args) {
        AccessUserUtil.setAccessUser("abc");
        try (AccessUserTransaction transaction = AccessUserUtil.openTransaction()) {
            System.out.println("abc = " + AccessUserUtil.getAccessUser());
            transaction.begin(1);
            System.out.println("1 = " + AccessUserUtil.getAccessUser());
            transaction.begin(2);
            System.out.println("2 = " + AccessUserUtil.getAccessUser());
            transaction.end();
            System.out.println("1 = " + AccessUserUtil.getAccessUser());
            transaction.end();

            System.out.println("abc = " + AccessUserUtil.getAccessUser());

            transaction.begin(2);
            System.out.println("2 = " + AccessUserUtil.getAccessUser());
        }
        System.out.println("abc = " + AccessUserUtil.getAccessUser());
    }
}
