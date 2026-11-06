package com.isldevs.portfolio.entity.contact;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class ContactController {

  private final ContactService service;
  private final ContactRateLimiter limiter;

  public ContactController(ContactService service, ContactRateLimiter limiter) {
    this.service = service;
    this.limiter = limiter;
  }

  public record Update(@NotNull Status status, @NotNull @Min(0) Long version) {}

  @PostMapping("/public/contact")
  public ResponseEntity<?> submit(@Valid @RequestBody Submission form, HttpServletRequest request) {
    // getRemoteAddr comes from the container. Raw X-Forwarded-For is deliberately ignored here.
    if (!limiter.allow(request.getRemoteAddr()))
      return ResponseEntity.status(429)
          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
          .header("Retry-After", "3600")
          .body(
              ProblemDetail.forStatusAndDetail(
                  HttpStatus.TOO_MANY_REQUESTS, "Please try again later"));
    if (form.website() != null && !form.website().isBlank())
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Contact submission could not be accepted");
    UUID id = service.submit(form);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of("id", id, "message", "Your message has been received."));
  }

  @GetMapping("/admin/messages")
  public Map<String, Object> list(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Status status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.list(q, status, page, size);
  }

  @GetMapping("/admin/messages/{id}")
  public Message get(@PathVariable UUID id) {
    return service.get(id);
  }

  @PutMapping("/admin/messages/{id}")
  public Message update(
      @PathVariable UUID id, @Valid @RequestBody Update update, Principal principal) {
    return service.update(id, update.status(), update.version(), principal.getName());
  }

  @DeleteMapping("/admin/messages/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id, Principal principal) {
    service.delete(id, principal.getName());
  }
}
