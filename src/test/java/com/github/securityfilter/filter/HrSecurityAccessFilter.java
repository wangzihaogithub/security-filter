//package com.github.securityfilter.filter;
//
//import com.ig.hr.common.HrAccessUser;
//import com.ig.hr.entity.Customer;
//import com.ig.hr.entity.CustomerLoginToken;
//import com.ig.hr.enumer.CustomerLoginTokenScopeEnum;
//import com.ig.hr.enumer.CustomerUserStatusEnum;
//import com.ig.hr.framework.LocalCacheService;
//import com.ig.hr.framework.WebSecurityAccessFilter;
//import com.ig.hr.response.user.CustomerUserDetailResp;
//import com.ig.hr.service.base.CustomerLoginTokenService;
//import com.ig.hr.service.base.CustomerUserService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import javax.servlet.http.HttpServletRequest;
//import java.util.Collections;
//import java.util.Objects;
//import java.util.Optional;
//
///**
// * HR系统用户信息拦截器
// * 放入用户信息
// *
// * @author acer01
// */
//@Component
//@Slf4j
//public class HrSecurityAccessFilter extends WebSecurityAccessFilter<Integer, HrAccessUser> {
//    private final LocalCacheService cacheService = new LocalCacheService();
//    @Autowired
//    private CustomerLoginTokenService customerLoginTokenService;
//    @Autowired
//    private CustomerUserService customerUserService;
//
//    public HrSecurityAccessFilter() {
//        super(Collections.singletonList(CustomerLoginTokenScopeEnum.HR.getTokenName()));
//    }
//
//    @Override
//    protected boolean isAccessSuccess(HrAccessUser user) {
//        return Objects.equals(user.getStatus(), CustomerUserStatusEnum.NORMAL.getKey())
//                && Optional.ofNullable(user.getCustomer()).map(Customer::getEnableFlag).orElse(true);
//    }
//
//    @Override
//    protected Integer selectUserId(HttpServletRequest request, String accessToken) {
//        CustomerLoginToken po = customerLoginTokenService.queryCustomerLoginTokenByToken(accessToken, CustomerLoginTokenScopeEnum.HR.getKey());
//        if (po == null) {
//            return null;
//        }
//        return po.getCustomerUserId();
//    }
//
//    @Override
//    protected HrAccessUser selectUser(HttpServletRequest request, Integer userId, String accessToken) {
//        CustomerUserDetailResp resp = cacheService.getIfSet("U" + userId, () -> {
//            return customerUserService.queryDetailById(userId);
//        }, 20);
//        if (resp == null) {
//            return null;
//        }
//        return HrAccessUser.convert(request, accessToken, resp);
//    }
//
//}
