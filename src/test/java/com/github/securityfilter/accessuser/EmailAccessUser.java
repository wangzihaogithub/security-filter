//package com.github.securityfilter.accessuser;
//
//import com.ig.hr.entity.Talent;
//import com.ig.hr.framework.CustomerAccessUser;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//
//@EqualsAndHashCode(callSuper = true)
//@Data
//public class EmailAccessUser extends PublicAccessUser implements CustomerAccessUser {
//    private Integer customerId;
//    private String role;
//    private boolean bySystemOperate;
//
//    public static EmailAccessUser buildCandidate(Talent talent, String requestDomain) {
//        EmailAccessUser user = new EmailAccessUser();
//        user.setName(talent.getName());
//        user.setRequestDomain(requestDomain);
//        user.setCustomerId(talent.getCustomerId());
//        user.setRole("候选人");
//        user.setBySystemOperate(false);
//        return user;
//    }
//}
