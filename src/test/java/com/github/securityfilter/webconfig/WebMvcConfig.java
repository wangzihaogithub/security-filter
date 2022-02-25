//package com.github.securityfilter.webconfig;
//
//import com.ig.hr.conf.aop.*;
//import com.ig.hr.enumer.CustomerLoginTokenScopeEnum;
//import com.ig.hr.util.StringUtil;
//import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
//import org.springframework.boot.web.server.WebServerFactoryCustomizer;
//import org.springframework.boot.web.servlet.FilterRegistrationBean;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpMethod;
//import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
//import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//import javax.servlet.*;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.util.EnumSet;
//import java.util.LinkedHashSet;
//import java.util.Set;
//
//@Configuration
//public class WebMvcConfig implements WebMvcConfigurer {
//
//    /**
//     * 只能是customer_user表的用户访问口。 {@link com.ig.hr.common.HrAccessUser}
//     */
//    @Bean
//    public FilterRegistrationBean hrSecurityFilter(HrSecurityAccessFilter filter) {
//        FilterRegistrationBean<HrSecurityAccessFilter> registration = new FilterRegistrationBean<>();
//        registration.setFilter(filter);
//        registration.addUrlPatterns("/api/*", "/statistics/*");
//        return registration;
//    }
//
//    /**
//     * 只能是hunter_account表的用户访问口。 {@link com.ig.hr.common.HunterAccessUser}
//     */
//    @Bean
//    public FilterRegistrationBean hunterSecurityFilter(HunterSecurityAccessFilter filter) {
//        FilterRegistrationBean<HunterSecurityAccessFilter> registration = new FilterRegistrationBean<>();
//        registration.setFilter(filter);
//        registration.addUrlPatterns("/hunter/*");
//        return registration;
//    }
//
//    /**
//     * 只能是 http://admin.itexxx.com 的用户访问口。 {@link com.ig.hr.common.InnerAccessUser}
//     */
//    @Bean
//    public FilterRegistrationBean innerSecurityFilter(InnerSecurityAccessFilter filter) {
//        FilterRegistrationBean<InnerSecurityAccessFilter> registration = new FilterRegistrationBean<>();
//        registration.setFilter(filter);
//        registration.addUrlPatterns("/inner/*");
//        return registration;
//    }
//
//    /**
//     * 不验证身份，开放接口，谁都能访问。 {@link com.ig.hr.common.PublicAccessUser}
//     */
//    @Bean
//    public FilterRegistrationBean publicSecurityFilter(PublicSecurityAccessFilter filter) {
//        FilterRegistrationBean<PublicSecurityAccessFilter> registration = new FilterRegistrationBean<>();
//        registration.setFilter(filter);
//        registration.addUrlPatterns("/common/*");
//        return registration;
//    }
//
//}
