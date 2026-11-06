package com.isldevs.portfolio.content.service;

import com.isldevs.portfolio.audit.dto.AuditResponse;
import com.isldevs.portfolio.audit.mapper.AuditMapper;
import com.isldevs.portfolio.audit.repository.AuditLogRepository;
import com.isldevs.portfolio.contact.entity.Status;
import com.isldevs.portfolio.contact.repository.ContactRepository;
import com.isldevs.portfolio.content.dto.DashboardResponse;
import com.isldevs.portfolio.content.repository.ContentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

  private final ContentRepository content;
  private final ContactRepository messages;
  private final AuditLogRepository logs;
  private final AuditMapper mapper;

  public DashboardService(
      ContentRepository content,
      ContactRepository messages,
      AuditLogRepository logs,
      AuditMapper mapper) {
    this.content = content;
    this.messages = messages;
    this.logs = logs;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public DashboardResponse dashboard() {
    return new DashboardResponse(
        content.countByKind("projects"),
        content.countByKindAndStatus("projects", "PUBLISHED"),
        content.countByKind("articles"),
        content.countByKindAndStatus("articles", "PUBLISHED"),
        content.countByKindAndStatus("articles", "DRAFT"),
        messages.countByStatus(Status.UNREAD),
        logs.findTop15ByOrderByCreatedAtDesc().stream().map(mapper::toDto).toList());
  }

  @Transactional(readOnly = true)
  public List<AuditResponse> audit() {
    return logs.findTop100ByOrderByCreatedAtDesc().stream().map(mapper::toDto).toList();
  }
}
