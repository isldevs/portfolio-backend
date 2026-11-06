package com.isldevs.portfolio.auth;

import com.isldevs.portfolio.common.ApiException;
import com.isldevs.portfolio.common.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final CookieCsrfTokenRepository csrfRepository;
    private final SessionAuthenticationStrategy loginStrategy;
    private final SessionRegistry sessions;
    private final AuthenticationManager manager;
    private final SecurityContextRepository contexts;
    private final AuditService audit;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthController(AuthenticationManager manager,
                          SecurityContextRepository contexts,
                          AuditService audit,
                          UserRepository users,
                          PasswordEncoder encoder,
                          SessionAuthenticationStrategy loginStrategy,
                          SessionRegistry sessions,
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

    @PostMapping("/admin/auth/login")
    public Map<String, String> login(
            @Valid @RequestBody Login login, HttpServletRequest request, HttpServletResponse response) {
        if (login.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw ApiException.invalid("Password exceeds the supported length.");
        String account = "account:" + login.username().toLowerCase(Locale.ROOT);
        String key = account + "|" + request.getRemoteAddr();
        var now = Instant.now();
        synchronized (Attempt.attempts) {
            Attempt.attempts.entrySet().removeIf(e -> e.getValue().until.isBefore(now));
        }
        var current = Attempt.attempts.get(key);
        var accountAttempts = Attempt.attempts.get(account);
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
            Attempt.attempts.remove(key);
            Attempt.attempts.remove(account);
            audit.record("LOGIN", "admin", login.username());
            return Map.of("username", authentication.getName(), "role", "ADMIN");
        } catch (AuthenticationException e) {
            Attempt.attempts.compute(
                    key, (k, v) -> new Attempt(v == null ? 1 : v.count + 1, now.plusSeconds(900)));
            Attempt.attempts.compute(
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
        if (session != null)
            session.invalidate();
        SecurityContextHolder.clearContext();
        csrfRepository.saveToken(null, req, res);
        var cookie = new jakarta.servlet.http.Cookie("PORTFOLIO_SESSION", "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        res.addCookie(cookie);
    }

    @PostMapping("/admin/auth/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void password(@Valid @RequestBody PasswordChange input, Authentication auth, HttpServletRequest req) {
        User user = users.findByUsername(auth.getName()).orElseThrow(ApiException::notFound);
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
