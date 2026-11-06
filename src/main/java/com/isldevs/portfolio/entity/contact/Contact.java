package com.isldevs.portfolio.entity.contact;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "contact")
public class Contact {

  @Id public UUID id = UUID.randomUUID();
  public String name;
  public String email;
  public String subject;

  @Column(columnDefinition = "text")
  public String message;

  @Enumerated(EnumType.STRING)
  public Status status = Status.UNREAD;

  @Version public Long version;
  public OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);
  public OffsetDateTime updatedAt = createdAt;
}
