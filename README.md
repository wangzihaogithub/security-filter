# security-filter

#### 介绍
不需要复杂配置的用户登录拦截器.解决了dubbo-filter中嵌套调用dubbo查询问题


#### 软件架构
软件架构说明


#### 安装教程

1.  添加maven依赖, 在pom.xml中加入 [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.wangzihaogithub/security-filter/badge.svg)](https://search.maven.org/search?q=g:com.github.wangzihaogithub%20AND%20a:security-filter)


        <!-- 登录access_token拦截器 -->
        <!-- https://mvnrepository.com/artifact/com.github.wangzihaogithub/sse-server -->
        <dependency>
            <groupId>com.github.wangzihaogithub</groupId>
            <artifactId>security-filter</artifactId>
            <version>1.1.12</version>
        </dependency>
        
2.  实现业务逻辑


        @Component
        @Slf4j
        public class HrSecurityAccessFilter extends WebSecurityAccessFilter<Integer, HrAccessUser> {
            private final LocalCacheService cacheService = new LocalCacheService();
            @Autowired
            private CustomerLoginTokenService customerLoginTokenService;
            @Autowired
            private CustomerUserService customerUserService;
        
            public HrSecurityAccessFilter() {
                super(Collections.singletonList("access_token"));
            }
        
            @Override
            protected boolean isAccessSuccess(HrAccessUser user) {
                return Objects.equals(user.getStatus(), CustomerUserStatusEnum.NORMAL.getKey())
                        && Optional.ofNullable(user.getCustomer()).map(Customer::getEnableFlag).orElse(true);
            }
        
            @Override
            protected Integer selectUserId(HttpServletRequest request, String accessToken) {
                CustomerLoginToken po = customerLoginTokenService.queryCustomerLoginTokenByToken(accessToken, CustomerLoginTokenScopeEnum.HR.getKey());
                if (po == null) {
                    return null;
                }
                return po.getCustomerUserId();
            }
        
            @Override
            protected HrAccessUser selectUser(HttpServletRequest request, Integer userId, String accessToken) {
                CustomerUserDetailResp resp = cacheService.getIfSet("U" + userId, () -> {
                    return customerUserService.queryDetailById(userId);
                }, 20);
                if (resp == null) {
                    return null;
                }
                return HrAccessUser.convert(request, accessToken, resp);
            }
        
        }
        
        
3.  注册Filter路由


        /**
         * 只能是customer_user表的用户访问口。 {@link com.ig.hr.common.HrAccessUser}
         */
        @Bean
        public FilterRegistrationBean hrSecurityFilter(HrSecurityAccessFilter filter) {
            FilterRegistrationBean<HrSecurityAccessFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(filter);
            registration.addUrlPatterns("/api/*", "/statistics/*");
            return registration;
        }


#### 使用说明

    // 操作当前用户
    T : AccessUserUtil.getAccessUser()
    Object : AccessUserUtil.getAccessUserValue(attrName)
    boolean :AccessUserUtil.existAccessUser()
    AccessUserUtil.setCurrentThreadAccessUser(accessUser);
    AccessUserUtil.removeCurrentThreadAccessUser();
    AccessUserUtil.runOnAccessUser(accessUser, runnable)
    
    // 异步传递
    CompletableFuture<ResumeApiResponseData<String>> future = new AccessUserCompletableFuture<>(RpcContext.getContext().getCompletableFuture());

    // 自带dubbo拦截器，支持dubbo传递当前用户，使用者可以自行注册到dubbo的/META-INF/services里
    // implements org.apache.dubbo.rpc.Filter 
    com.github.securityfilter.DubboAccessUserFilter 

    // 自带servlet拦截器，使用者可以实现并自行注册到tomcat里
    // implements javax.servlet.Filter
    com.github.securityfilter.WebSecurityAccessFilter