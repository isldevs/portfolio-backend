package com.isldevs.portfolio.profile;

import com.isldevs.portfolio.common.*;
import com.isldevs.portfolio.content.StructuredContent;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProfileService {
  private static final Set<String> FIELDS =
      Set.of(
          "fullName",
          "professionalTitle",
          "headline",
          "shortBio",
          "fullBio",
          "location",
          "specialization",
          "availability",
          "githubUrl",
          "linkedinUrl",
          "publicEmail",
          "portraitUrl",
          "resumeUrl",
          "seoTitle",
          "seoDescription");
  private final ProfileRepository repo;
  private final AuditService audit;

  public ProfileService(ProfileRepository repo, AuditService audit) {
    this.repo = repo;
    this.audit = audit;
  }

  private Profile require() {
    return repo.findAll().stream().findFirst().orElseThrow(ApiException::notFound);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> get() {
    return dto(require());
  }

  public Map<String, Object> update(Map<String, Object> input) {
    Profile p = require();
    Object v = input.get("version");
    if (!(v instanceof Number) || ((Number) v).longValue() != p.version)
      throw ApiException.conflict("The profile changed. Reload before saving.");
    var data = new LinkedHashMap<>(p.data);
    for (String field : FIELDS)
      if (input.containsKey(field)) {
        if (!(input.get(field) instanceof String value)
            || value.length() > (field.equals("fullBio") ? 30000 : 4000))
          throw ApiException.invalid("Invalid profile field: " + field);
        if (field.equals("fullName") && value.isBlank())
          throw ApiException.invalid("Name is required.");
        if ((field.endsWith("Url")) && !value.isEmpty() && !StructuredContent.safeUrl(value))
          throw ApiException.invalid("Invalid profile URL.");
        if (field.equals("publicEmail")
            && !value.isBlank()
            && !value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
          throw ApiException.invalid("Invalid public email.");
        data.put(field, value);
      }
    p.data = data;
    p.updatedAt = Instant.now();
    repo.saveAndFlush(p);
    audit.record("UPDATE", "profile", p.id);
    return dto(p);
  }

  private Map<String, Object> dto(Profile p) {
    Map<String, Object> result = new LinkedHashMap<>(p.data);
    result.put("id", p.id);
    result.put("version", p.version);
    return result;
  }
}
