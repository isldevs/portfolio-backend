package com.isldevs.portfolio.entity.profile;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "profiles")
public class Profile {
  @Id public UUID id = UUID.randomUUID();
  @Version public Long version;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  public Map<String, Object> data = new LinkedHashMap<>();

  public Instant updatedAt = Instant.now();
}
