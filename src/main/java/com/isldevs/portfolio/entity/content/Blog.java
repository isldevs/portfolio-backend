package com.isldevs.portfolio.entity.content;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "blogs")
public class Blog {

  @Id public UUID id = UUID.randomUUID();
  public UUID contentId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  public Map<String, Object> snapshot;

  public String actor;
  public Instant createdAt = Instant.now();
}
