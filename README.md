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
            <version>1.1.5</version>
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

1.  xxxx
2.  xxxx
3.  xxxx

#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request


#### 特技

1.  使用 Readme\_XXX.md 来支持不同的语言，例如 Readme\_en.md, Readme\_zh.md
2.  Gitee 官方博客 [blog.gitee.com](https://blog.gitee.com)
3.  你可以 [https://gitee.com/explore](https://gitee.com/explore) 这个地址来了解 Gitee 上的优秀开源项目
4.  [GVP](https://gitee.com/gvp) 全称是 Gitee 最有价值开源项目，是综合评定出的优秀开源项目
5.  Gitee 官方提供的使用手册 [https://gitee.com/help](https://gitee.com/help)
6.  Gitee 封面人物是一档用来展示 Gitee 会员风采的栏目 [https://gitee.com/gitee-stars/](https://gitee.com/gitee-stars/)
