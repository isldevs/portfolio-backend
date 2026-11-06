package com.isldevs.portfolio.entity.contact;

import com.isldevs.portfolio.common.AuditService;
import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContactService {

  private final ContactRepository messages;
  private final ContactMapper mapper;
  private final AuditService audit;

  public ContactService(ContactRepository messages,
                        ContactMapper mapper,
                        AuditService audit) {
    this.messages = messages;
    this.mapper = mapper;
    this.audit = audit;
  }

  @Transactional
  public UUID submit(Submission form) {
    Contact message = new Contact();
    message.name = form.name().strip();
    message.email = form.email().strip();
    message.subject = form.subject().strip();
    message.message = form.message().strip();
    return messages.save(message).id;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> list(String q, Status status, int page, int size) {

    page = Math.max(0, page);
    size = Math.max(1, Math.min(100, size));

    Specification<Contact> spec =
        (root, query, cb) -> {
          List<Predicate> conditions = new ArrayList<>();
          if (q != null && !q.isBlank()) {
            String term =
                "%"
                    + q.strip()
                        .toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_")
                    + "%";
            conditions.add(
                cb.or(
                    cb.like(cb.lower(root.get("name")), term, '\\'),
                    cb.like(cb.lower(root.get("email")), term, '\\'),
                    cb.like(cb.lower(root.get("subject")), term, '\\'),
                    cb.like(cb.lower(root.get("message")), term, '\\')));
          }
          if (status != null) conditions.add(cb.equal(root.get("status"), status));
          return cb.and(conditions.toArray(Predicate[]::new));
        };

    final Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));
    final PageRequest pageRequest = PageRequest.of(page, size, sort);
    var result = messages.findAll(spec, pageRequest);

    return Map.of(
        "content", result.map(mapper::toDto).getContent(),
        "totalElements", result.getTotalElements(),
        "totalPages", result.getTotalPages(),
        "number", page,
        "size", size
    );
  }

  private Contact require(UUID id) {
    return messages
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
  }

  @Transactional(readOnly = true)
  public Message get(UUID id) {
    return mapper.toDto(require(id));
  }

  @Transactional
  public Message update(UUID id, Status status, long version, String actor) {
    Contact message = require(id);
    if (message.version != version)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Message changed; reload before saving");
    message.status = status;
    // Updating a message always advances its version, including repeated status assignments.
    message.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    messages.saveAndFlush(message);
    audit.record("UPDATE", "contact_messages", id, actor);
    return mapper.toDto(message);
  }

  @Transactional
  public void delete(UUID id, String actor) {
    messages.delete(require(id));
    messages.flush();
    audit.record("DELETE", "contact_messages", id, actor);
  }
}
