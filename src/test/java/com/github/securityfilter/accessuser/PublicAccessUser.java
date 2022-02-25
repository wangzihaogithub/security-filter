//package com.github.securityfilter.accessuser;
//
//import com.ig.hr.framework.AccessToken;
//import com.ig.hr.framework.AccessUser;
//import lombok.Data;
//
///**
// * 开放系统用户
// * 不验证身份，开放接口，谁都能访问
// */
//@Data
//public class PublicAccessUser implements AccessUser, AccessToken {
//    private String accessToken;
//    private String name;
//    private Integer id;
//    private String requestDomain;
//    @Override
//    public String getRole() {
//        return "开放系统";
//    }
//}
