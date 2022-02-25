//package com.github.securityfilter.accessuser;
//
//import com.ig.hr.framework.AccessToken;
//import com.ig.hr.framework.AccessUser;
//import lombok.Data;
//
///**
// * 内部系统用户
// * 只能是 http://admin.iterget.com 的用户访问口。
// */
//@Data
//public class InnerAccessUser implements AccessUser, AccessToken {
//    private String accessToken;
//    private String name;
//    private Integer id;
//    private String requestDomain;
//    @Override
//    public String getRole() {
//        return "内部系统";
//    }
//}
