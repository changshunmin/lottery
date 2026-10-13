package com.lottery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                // 预检请求放行
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 公开资源：抽奖页及其依赖
                .antMatchers("/", "/index.html", "/favicon.svg", "/icons.svg").permitAll()
                // 公开只读接口：奖品列表、最近记录
                .antMatchers(HttpMethod.GET, "/api/prizes", "/api/lottery/recent").permitAll()
                // 公开抽奖：任何人可抽
                .antMatchers(HttpMethod.POST, "/api/lottery/draw").permitAll()
                // 其余接口需要登录（分页查询、领奖、奖品配置）
                .antMatchers("/api/**").authenticated()
                // 管理页需要登录
                .antMatchers("/admin/**").authenticated()
                .anyRequest().permitAll()
            .and()
            .exceptionHandling()
                // API 未登录返回 401，页面未登录跳转登录页
                .defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        new AntPathRequestMatcher("/api/**"))
                .defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new AntPathRequestMatcher("/**"))
            .and()
            .formLogin();
        return http.build();
    }
}
