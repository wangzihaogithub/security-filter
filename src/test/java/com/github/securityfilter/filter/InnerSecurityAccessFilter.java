//package com.github.securityfilter.filter;
//
//import com.ig.hr.common.InnerAccessUser;
//import com.ig.hr.framework.WebSecurityAccessFilter;
//import com.ig.hr.util.WebUtil;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import javax.servlet.http.HttpServletRequest;
//
///**
// * 内部调用用户信息拦截器
// * 放入用户信息
// *
// * @author acer01
// */
//@Component
//@Slf4j
//public class InnerSecurityAccessFilter extends WebSecurityAccessFilter<Integer, InnerAccessUser> {
//    @Override
//    protected String[] getAccessTokens(HttpServletRequest request) {
//        return new String[]{"0"};
//    }
//
//    @Override
//    protected Integer selectUserId(HttpServletRequest request, String accessToken) {
//        return 0;
//    }
//
//    @Override
//    protected InnerAccessUser selectUser(HttpServletRequest request, Integer userId, String accessToken) {
//        InnerAccessUser accessUser = new InnerAccessUser();
//        accessUser.setAccessToken(accessToken);
//        accessUser.setName("内部调用用户");
//        accessUser.setId(userId);
//        accessUser.setRequestDomain(WebUtil.getRequestDomain(request, true));
//        return accessUser;
//    }
//
//}
