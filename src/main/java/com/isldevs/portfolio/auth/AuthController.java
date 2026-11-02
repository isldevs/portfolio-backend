package com.isldevs.portfolio.auth;

import com.isldevs.portfolio.common.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
  private final CookieCsrfTokenRepository csrfRepository;
  private final org.springframework.security.web.authentication.session
          .SessionAuthenticationStrategy
      loginStrategy;
  private final org.springframework.security.core.session.SessionRegistry sessions;
  private final AuthenticationManager manager;
  private final SecurityContextRepository contexts;
  private final AuditService audit;
  private final AdminUserRepository users;
  private final PasswordEncoder encoder;

  private record Attempt(int count, Instant until) {}

  private final Map<String, Attempt> attempts =
      Collections.synchronizedMap(
          new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Attempt> e) {
              return size() > 10000;
            }
          });

  public AuthController(
      AuthenticationManager manager,
      SecurityContextRepository contexts,
      AuditService audit,
      AdminUserRepository users,
      PasswordEncoder encoder,
      org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
          loginStrategy,
      org.springframework.security.core.session.SessionRegistry sessions,
      CookieCsrfTokenRepository csrfRepository) {
    this.csrfRepository = csrfRepository;
    this.loginStrategy = loginStrategy;
    this.sessions = sessions;
    this.manager = manager;
    this.contexts = contexts;
    this.audit = audit;
    this.users = users;
    this.encoder = encoder;
  }

  @GetMapping("/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }

  public record Login(
      @NotBlank @Size(max = 120) String username, @NotBlank @Size(max = 256) String password) {}

  @PostMapping("/admin/auth/login")
  public Map<String, String> login(
      @Valid @RequestBody Login login, HttpServletRequest request, HttpServletResponse response) {
    if (login.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw ApiException.invalid("Password exceeds the supported length.");
    String account = "account:" + login.username().toLowerCase(Locale.ROOT);
    String key = account + "|" + request.getRemoteAddr();
    var now = Instant.now();
    synchronized (attempts) {
      attempts.entrySet().removeIf(e -> e.getValue().until.isBefore(now));
    }
    var current = attempts.get(key);
    var accountAttempts = attempts.get(account);
    if ((current != null && current.count >= 5)
        || (accountAttempts != null && accountAttempts.count >= 10))
      throw new ApiException(
          HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts. Try again in 15 minutes.");
    try {
      Authentication authentication =
          manager.authenticate(
              UsernamePasswordAuthenticationToken.unauthenticated(
                  login.username(), login.password()));
      loginStrategy.onAuthentication(authentication, request, response);
      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(authentication);
      SecurityContextHolder.setContext(context);
      contexts.saveContext(context, request, response);
      attempts.remove(key);
      attempts.remove(account);
      audit.record("LOGIN", "admin", login.username());
      return Map.of("username", authentication.getName(), "role", "ADMIN");
    } catch (AuthenticationException e) {
      attempts.compute(
          key, (k, v) -> new Attempt(v == null ? 1 : v.count + 1, now.plusSeconds(900)));
      attempts.compute(
          account, (k, v) -> new Attempt(v == null ? 1 : v.count + 1, now.plusSeconds(900)));
      audit.record("LOGIN_FAILED", "admin", login.username());
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
    }
  }

  @GetMapping("/admin/auth/me")
  public Map<String, String> me(Authentication auth) {
    return Map.of("username", auth.getName(), "role", "ADMIN");
  }

  @PostMapping("/admin/auth/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(HttpServletRequest req, HttpServletResponse res) {
    audit.record("LOGOUT", "admin", null);
    var session = req.getSession(false);
    if (session != null) session.invalidate();
    SecurityContextHolder.clearContext();
    csrfRepository.saveToken(null, req, res);
    var cookie = new jakarta.servlet.http.Cookie("PORTFOLIO_SESSION", "");
    cookie.setPath("/");
    cookie.setHttpOnly(true);
    cookie.setMaxAge(0);
    res.addCookie(cookie);
  }

  public record PasswordChange(
      @NotBlank String currentPassword, @Size(min = 14, max = 128) String newPassword) {}

  @PostMapping("/admin/auth/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional
  public void password(
      @Valid @RequestBody PasswordChange input, Authentication auth, HttpServletRequest req) {
    AdminUser user = users.findByUsername(auth.getName()).orElseThrow(ApiException::notFound);
    if (!encoder.matches(input.currentPassword(), user.passwordHash))
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Current password is incorrect.");
    if (input.newPassword() == null
        || input.newPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw ApiException.invalid(
          "New password requires at least 14 characters and at most 72 UTF-8 bytes.");
    user.passwordHash = encoder.encode(input.newPassword());
    user.updatedAt = Instant.now();
    users.save(user);
    audit.record("PASSWORD_CHANGE", "admin", user.id);
    sessions
        .getAllSessions(auth.getPrincipal(), false)
        .forEach(org.springframework.security.core.session.SessionInformation::expireNow);
    req.getSession().invalidate();
    SecurityContextHolder.clearContext();
  }
}
