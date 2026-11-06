package com.isldevs.portfolio.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "contents", uniqueConstraints = @UniqueConstraint(columnNames = {"kind", "slug"}))
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
