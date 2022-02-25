//package com.github.securityfilter.accessuser;
//
//import com.ig.hr.framework.AccessToken;
//import com.ig.hr.framework.CustomerAccessUser;
//import com.ig.hr.response.user.CustomerUserDetailResp;
//import com.ig.hr.util.BeanUtil;
//import com.ig.hr.util.WebUtil;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//
//import javax.servlet.http.HttpServletRequest;
//import java.util.StringJoiner;
//
///**
// * HR系统用户
// * 只能是customer_user表的用户访问口
// */
//@EqualsAndHashCode(callSuper = true)
//@Data
//public class HrAccessUser extends CustomerUserDetailResp implements CustomerAccessUser, AccessToken {
//    private String accessToken;
//    private String requestDomain;
//
//    public static HrAccessUser convert(HttpServletRequest request, String accessToken, CustomerUserDetailResp resp) {
//        HrAccessUser result = BeanUtil.transform(resp, HrAccessUser.class);
//        result.setAccessToken(accessToken);
//        result.setRequestDomain(WebUtil.getRequestDomain(request, false));
//        return result;
//    }
//
//    @Override
//    public String toString() {
//        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
//                .add("id=" + getId())
//                .add("name='" + getName() + "'")
//                .add("email='" + getEmail() + "'")
//                .add("accessToken='" + accessToken + "'")
//                .add("roleName='" + getRoleName() + "'")
//                .add("customer='" + getCustomer() + "'")
//                .toString();
//    }
//}
