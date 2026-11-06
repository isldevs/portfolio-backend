package com.isldevs.portfolio.profile.service;

import com.isldevs.portfolio.audit.service.AuditService;
import com.isldevs.portfolio.common.exception.ApiException;
import com.isldevs.portfolio.common.json.ApiJsonNames;
import com.isldevs.portfolio.content.validation.StructuredContent;
import com.isldevs.portfolio.profile.dto.ProfileResponse;
import com.isldevs.portfolio.profile.entity.Profile;
import com.isldevs.portfolio.profile.mapper.ProfileMapper;
import com.isldevs.portfolio.profile.repository.ProfileRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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
  private final ProfileMapper mapper;

  public ProfileService(ProfileRepository repo, AuditService audit, ProfileMapper mapper) {
    this.repo = repo;
    this.audit = audit;
    this.mapper = mapper;
  }

  private Profile require() {
    return repo.findAll().stream().findFirst().orElseThrow(ApiException::notFound);
  }

  @Transactional(readOnly = true)
  public ProfileResponse get() {
    return mapper.toDto(require());
  }

  public ProfileResponse update(Map<String, Object> input) {
    input = ApiJsonNames.toInternal(input);
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
    return mapper.toDto(p);
  }
}
