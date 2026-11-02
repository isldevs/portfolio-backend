package com.isldevs.portfolio.content;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "content_items",
    uniqueConstraints = @UniqueConstraint(columnNames = {"kind", "slug"}))
public class ContentItem {
  @Id public UUID id = UUID.randomUUID();
  @Version public Long version;
  public String kind;
  public String title;
  public String slug;

  @Column(columnDefinition = "text")
  public String summary = "";

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  public Map<String, Object> body =
      new LinkedHashMap<>(Map.of("type", "doc", "content", List.of()));

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  public Map<String, Object> data = new LinkedHashMap<>();

  public String status = "DRAFT";
  public boolean featured;
  public int displayOrder;
  public Instant createdAt = Instant.now();
  public Instant updatedAt = Instant.now();
  public Instant publishedAt;

  @PreUpdate
  public void updateTime() {
    updatedAt = Instant.now();
  }
}
