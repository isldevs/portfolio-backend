package com.isldevs.portfolio.content;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "blog_revisions")
public class BlogRevision {
  @Id public UUID id = UUID.randomUUID();
  public UUID contentId;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  public java.util.Map<String, Object> snapshot;

  public String actor;
  public java.time.Instant createdAt = java.time.Instant.now();
}
