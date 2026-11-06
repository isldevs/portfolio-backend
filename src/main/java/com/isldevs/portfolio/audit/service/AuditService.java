package com.isldevs.portfolio.audit.service;

import com.isldevs.portfolio.audit.entity.AuditLog;
import com.isldevs.portfolio.audit.repository.AuditLogRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditService {
  private final AuditLogRepository logs;

  public AuditService(AuditLogRepository logs) {
    this.logs = logs;
  }

  public void record(String action, String resource, Object id) {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    record(action, resource, id, auth == null ? "system" : auth.getName());
  }

  public void record(String action, String resource, Object id, String actor) {
    AuditLog log = new AuditLog();
    log.action = action;
    log.resource = resource;
    log.resourceId = id == null ? null : id.toString();
    log.actor = actor;
    logs.saveAndFlush(log);
  }
}
