package com.isldevs.portfolio.common;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
  private final JdbcTemplate jdbc;

  public AuditService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void record(String action, String resource, Object id) {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    String actor = auth == null ? "system" : auth.getName();
    jdbc.update(
        "INSERT INTO audit_logs(id,action,resource,resource_id,actor) VALUES (?,?,?,?,?)",
        UUID.randomUUID(),
        action,
        resource,
        id == null ? null : id.toString(),
        actor);
  }
}
