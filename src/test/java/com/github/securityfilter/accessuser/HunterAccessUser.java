//package com.github.securityfilter.accessuser;
//
//import com.ig.hr.entity.Customer;
//import com.ig.hr.entity.HunterCompany;
//import com.ig.hr.entity.HunterCompanyAccount;
//import com.ig.hr.enumer.PipelineUserTypeEnum;
//import com.ig.hr.framework.AccessToken;
//import com.ig.hr.framework.CustomerAccessUser;
//import com.ig.hr.response.hunter.HunterAccountDetailResp;
//import com.ig.hr.response.hunter.HunterCompanyAccountDetailResp;
//import com.ig.hr.util.BeanUtil;
//import com.ig.hr.util.WebUtil;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//
//import javax.servlet.http.HttpServletRequest;
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * 猎头系统用户
// * 只能是hunter_account表的用户访问口
// */
//@EqualsAndHashCode(callSuper = true)
//@Data
//public class HunterAccessUser extends HunterAccountDetailResp implements CustomerAccessUser, AccessToken {
//    private String accessToken;
//    private Integer customerId;
//    private String requestDomain;
//
//    public static HunterAccessUser convert(HttpServletRequest request, String accessToken, Integer customerId,
//                                           HunterAccountDetailResp hunterAccount) {
//        HunterAccessUser result = BeanUtil.transform(hunterAccount, HunterAccessUser.class);
//        result.setCustomerId(customerId);
//        result.setAccessToken(accessToken);
//        result.setRequestDomain(WebUtil.getRequestDomain(request, true));
//        return result;
//    }
//
//    public List<HunterCompanyAccountDetailResp> getHunterCompanyAccountList() {
//        if (customerId == null) {
//            return Collections.emptyList();
//        } else {
//            return getAllHunterCompanyAccountList().stream()
//                    .filter(e -> Objects.equals(customerId, e.getCustomerId()))
//                    .collect(Collectors.toList());
//        }
//    }
//
//    public Customer getCustomer() {
//        return Optional.ofNullable(getHunterCompanyAccount())
//                .map(HunterCompanyAccountDetailResp::getCustomer)
//                .orElse(null);
//    }
//
//    @Override
//    public Integer getCompanyId() {
//        return Optional.ofNullable(getHunterCompanyAccount())
//                .map(HunterCompanyAccount::getHunterCompanyId)
//                .orElse(null);
//    }
//
//    @Override
//    public String toString() {
//        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
//                .add("id=" + getId())
//                .add("name='" + getName() + "'")
//                .add("email='" + getEmail() + "'")
//                .add("customerId=" + customerId)
//                .add("accessToken='" + accessToken + "'")
//                .toString();
//    }
//
//    @Override
//    public String getName() {
//        return Optional.ofNullable(getHunterCompanyAccount()).map(HunterCompanyAccount::getName)
//                .orElse(super.getName());
//    }
//
//    public HunterCompanyAccountDetailResp getHunterCompanyAccount() {
//        for (HunterCompanyAccountDetailResp company : getHunterCompanyAccountList()) {
//            if (company.getHunterCompany() != null) {
//                return company;
//            }
//        }
//        return null;
//    }
//
//    public HunterCompany getHunterCompany() {
//        return Optional.ofNullable(getHunterCompanyAccount())
//                .map(HunterCompanyAccountDetailResp::getHunterCompany)
//                .orElse(null);
//    }
//
//    public boolean isRpo() {
//        return Optional.ofNullable(getHunterCompany())
//                .map(HunterCompany::getEnableRpoFlag)
//                .orElse(false);
//    }
//
//    public String getHunterCompanyName() {
//        return Optional.ofNullable(getHunterCompany())
//                .map(HunterCompany::getName)
//                .orElse(null);
//    }
//
//    @Override
//    public Integer getUserType() {
//        return PipelineUserTypeEnum.HUNTER.getKey();
//    }
//
//    @Override
//    public String getRole() {
//        String hunterCompanyName = getHunterCompanyName();
//        if (hunterCompanyName == null) {
//            return "猎头";
//        } else {
//            return "猎头[" + hunterCompanyName + "]";
//        }
//    }
//}
