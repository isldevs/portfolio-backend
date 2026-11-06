package com.isldevs.portfolio.contact.controller;

import com.isldevs.portfolio.common.dto.PageResponse;
import com.isldevs.portfolio.contact.dto.ContactRequest;
import com.isldevs.portfolio.contact.dto.MessageResponse;
import com.isldevs.portfolio.contact.dto.MessageUpdateRequest;
import com.isldevs.portfolio.contact.dto.SubmissionResponse;
import com.isldevs.portfolio.contact.entity.Status;
import com.isldevs.portfolio.contact.service.ContactRateLimiter;
import com.isldevs.portfolio.contact.service.ContactService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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

  @PostMapping("/public/contact")
  public ResponseEntity<SubmissionResponse> submit(
      @Valid @RequestBody ContactRequest form, HttpServletRequest request) {
    // getRemoteAddr comes from the container. Raw X-Forwarded-For is deliberately ignored here.
    if (!limiter.allow(request.getRemoteAddr()))
      throw new com.isldevs.portfolio.common.exception.RateLimitException();
    if (form.website() != null && !form.website().isBlank())
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Contact submission could not be accepted");
    UUID id = service.submit(form);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new SubmissionResponse(id, "Your message has been received."));
  }

  @GetMapping("/admin/messages")
  public PageResponse<MessageResponse> list(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Status status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.list(q, status, page, size);
  }

  @GetMapping("/admin/messages/{id}")
  public MessageResponse get(@PathVariable UUID id) {
    return service.get(id);
  }

  @PutMapping("/admin/messages/{id}")
  public MessageResponse update(
      @PathVariable UUID id, @Valid @RequestBody MessageUpdateRequest update, Principal principal) {
    return service.update(id, update.status(), update.version(), principal.getName());
  }

  @DeleteMapping("/admin/messages/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id, Principal principal) {
    service.delete(id, principal.getName());
  }
}
