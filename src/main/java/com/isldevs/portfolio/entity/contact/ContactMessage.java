package com.isldevs.portfolio.entity.contact;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "contact_messages")
public class ContactMessage {
  @Id public UUID id = UUID.randomUUID();
  public String name;
  public String email;
  public String subject;

  @Column(columnDefinition = "text")
  public String message;

  @Enumerated(EnumType.STRING)
  public ContactService.Status status = ContactService.Status.UNREAD;

  @Version public Long version;
  public java.time.OffsetDateTime createdAt =
      java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC);
  public java.time.OffsetDateTime updatedAt = createdAt;
}
