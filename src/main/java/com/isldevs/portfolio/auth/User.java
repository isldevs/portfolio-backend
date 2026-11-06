package com.isldevs.portfolio.auth;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
  @Id public UUID id = UUID.randomUUID();

  @Column(unique = true)
  public String username;

  public String passwordHash;
  public Instant createdAt = Instant.now();
  public Instant updatedAt = Instant.now();
}
