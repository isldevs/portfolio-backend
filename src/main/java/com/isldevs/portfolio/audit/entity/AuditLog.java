package com.isldevs.portfolio.audit.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
  @Id public UUID id = UUID.randomUUID();
  public String action;
  public String resource;
  public String resourceId;
  public String actor;
  public java.time.Instant createdAt = java.time.Instant.now();
}
