package com.isldevs.portfolio.content.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "blogs")
public class BlogRevision {

  @Id public UUID id = UUID.randomUUID();
  public UUID contentId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  public Map<String, Object> snapshot;

  public String actor;
  public Instant createdAt = Instant.now();
}
