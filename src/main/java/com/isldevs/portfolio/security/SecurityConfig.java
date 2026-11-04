package com.isldevs.portfolio.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isldevs.portfolio.auth.AdminUserRepository;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  UserDetailsService users(AdminUserRepository repo) {
    return username ->
        repo.findByUsername(username)
            .map(u -> User.withUsername(u.username).password(u.passwordHash).roles("ADMIN").build())
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
  }

  @Bean
  AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
    var p = new org.springframework.security.authentication.dao.DaoAuthenticationProvider(users);
    p.setPasswordEncoder(encoder);
    return new ProviderManager(p);
  }

  @Bean
  SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  org.springframework.security.core.session.SessionRegistry sessionRegistry() {
    return new org.springframework.security.core.session.SessionRegistryImpl();
  }

  @Bean
  org.springframework.security.web.session.HttpSessionEventPublisher sessionEventPublisher() {
    return new org.springframework.security.web.session.HttpSessionEventPublisher();
  }

  @Bean
  CookieCsrfTokenRepository csrfRepository() {
    var c = new CookieCsrfTokenRepository();
    c.setCookieName("XSRF-TOKEN");
    c.setCookiePath("/");
    return c;
  }

  @Bean
  org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
      loginStrategy(
          org.springframework.security.core.session.SessionRegistry registry,
          CookieCsrfTokenRepository csrf) {
    var concurrent =
        new org.springframework.security.web.authentication.session
            .ConcurrentSessionControlAuthenticationStrategy(registry);
    concurrent.setMaximumSessions(5);
    return new org.springframework.security.web.authentication.session
        .CompositeSessionAuthenticationStrategy(
        java.util.List.of(
            concurrent,
            new org.springframework.security.web.authentication.session
                .ChangeSessionIdAuthenticationStrategy(),
            new org.springframework.security.web.authentication.session
                .RegisterSessionAuthenticationStrategy(registry),
            new CsrfAuthenticationStrategy(csrf)));
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      ObjectMapper mapper,
      SecurityContextRepository contexts,
      CookieCsrfTokenRepository csrf,
      org.springframework.security.core.session.SessionRegistry registry)
      throws Exception {
    var handler = new CsrfTokenRequestAttributeHandler();
    http.csrf(c -> c.csrfTokenRepository(csrf).csrfTokenRequestHandler(handler))
        .securityContext(c -> c.securityContextRepository(contexts))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/public/**",
                        "/api/v1/csrf",
                        "/actuator/health",
                        "/error")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST, "/api/v1/public/contact", "/api/v1/admin/auth/login")
                    .permitAll()
                    .requestMatchers(
                        "/api/v1/admin/**",
                        "/api/v1/openapi/**",
                        "/api/v1/docs/**",
                        "/swagger-ui/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .denyAll())
        .sessionManagement(
            s ->
                s.sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::migrateSession)
                    .maximumSessions(5)
                    .sessionRegistry(registry)
                    .expiredSessionStrategy(
                        event -> problem(event.getResponse(), 401, "Session expired", mapper)))
        .requestCache(RequestCacheConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, ex) -> problem(res, 401, "Authentication required", mapper))
                    .accessDeniedHandler(
                        (req, res, ex) ->
                            problem(res, 403, "Access denied or CSRF token missing", mapper)))
        .headers(
            h ->
                h.contentTypeOptions(c -> {})
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                    .referrerPolicy(
                        r ->
                            r.policy(
                                org.springframework.security.web.header.writers
                                    .ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));
    return http.build();
  }

  private void problem(HttpServletResponse res, int status, String detail, ObjectMapper mapper)
      throws java.io.IOException {
    res.setStatus(status);
    res.setContentType("application/problem+json");
    mapper.writeValue(
        res.getOutputStream(),
        ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), detail));
  }
}
