package com.isldevs.portfolio.media;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "media_assets")
public class MediaAsset {
  @Id public UUID id = UUID.randomUUID();
  public String storageKey;
  public String originalName;
  public String contentType;
  public long size;

  @Enumerated(EnumType.STRING)
  public MediaService.Visibility visibility;

  public java.time.OffsetDateTime createdAt =
      java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC);
}
