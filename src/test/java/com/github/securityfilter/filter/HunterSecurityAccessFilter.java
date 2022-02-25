//package com.github.securityfilter.filter;
//
//import com.ig.hr.common.HunterAccessUser;
//import com.ig.hr.entity.CustomerLoginToken;
//import com.ig.hr.enumer.CustomerLoginTokenScopeEnum;
//import com.ig.hr.framework.LocalCacheService;
//import com.ig.hr.framework.ResponseData;
//import com.ig.hr.framework.WebSecurityAccessFilter;
//import com.ig.hr.response.hunter.HunterAccountDetailResp;
//import com.ig.hr.response.hunter.HunterCompanyAccountDetailResp;
//import com.ig.hr.service.base.CustomerLoginTokenService;
//import com.ig.hr.service.hunter.HunterAccountService;
//import com.ig.hr.util.StringUtil;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import javax.servlet.FilterChain;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.util.Collections;
//import java.util.List;
//import java.util.Objects;
//
///**
// * 猎头系统用户信息拦截器
// * 放入用户信息
// *
// * @author acer01
// */
//@Component
//@Slf4j
//public class HunterSecurityAccessFilter extends WebSecurityAccessFilter<Integer, HunterAccessUser> {
//    private final LocalCacheService cacheService = new LocalCacheService().cloneFlag(false);
//    @Autowired
//    private CustomerLoginTokenService customerLoginTokenService;
//    @Autowired
//    private HunterAccountService hunterAccountService;
//
//    public HunterSecurityAccessFilter() {
//        super(Collections.singletonList(CustomerLoginTokenScopeEnum.HUNTER.getTokenName()));
//    }
//
//    /**
//     * 是否可以访问该客户的数据
//     *
//     * @param accessUser
//     * @return
//     */
//    private boolean canAccessResourceOfScope(HttpServletRequest request, HunterAccessUser accessUser) {
//        Integer customerId = accessUser.getCustomerId();
//        if (customerId == null) {
//            return true;
//        }
//        List<HunterCompanyAccountDetailResp> allHunterCompanyAccountList = accessUser.getAllHunterCompanyAccountList();
//        if (allHunterCompanyAccountList == null || allHunterCompanyAccountList.isEmpty()) {
//            return false;
//        }
//        return allHunterCompanyAccountList.stream().anyMatch(e ->
//                Objects.equals(customerId, e.getCustomerId()));
//    }
//
//    @Override
//    protected void onAccessSuccess(HttpServletRequest request, HttpServletResponse response, FilterChain chain, HunterAccessUser accessUser) throws IOException, ServletException {
//        if (canAccessResourceOfScope(request, accessUser)) {
//            chain.doFilter(request, response);
//        } else {
//            ResponseData result = new ResponseData<>(true,
//                    "406", null, "账号已被停用，请联系客户HR");
//            writeToBody(response, result);
//        }
//    }
//
//    @Override
//    protected Integer selectUserId(HttpServletRequest request, String accessToken) {
//        CustomerLoginToken po = customerLoginTokenService.queryCustomerLoginTokenByToken(accessToken, CustomerLoginTokenScopeEnum.HUNTER.getKey());
//        if (po == null) {
//            return null;
//        }
//        return po.getCustomerUserId();
//    }
//
//    @Override
//    protected HunterAccessUser selectUser(HttpServletRequest request, Integer hunterAccountId, String accessToken) {
//        String customerIdString = request.getParameter("customerId");
//        Integer customerId;
//        if (StringUtil.isNumeric(customerIdString)) {
//            customerId = Integer.valueOf(customerIdString);
//        } else {
//            customerId = null;
//        }
//
//        HunterAccessUser accessUser = cacheService.getIfSet("U" + hunterAccountId + "_" + customerId, () -> {
//            HunterAccountDetailResp hunterAccount = hunterAccountService.selectDetailById(hunterAccountId);
//            return HunterAccessUser.convert(request, accessToken, customerId, hunterAccount);
//        }, 20);
//        return accessUser;
//    }
//
//}
