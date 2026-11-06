package com.isldevs.portfolio.entity.media;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "media")
public class Media {
    @Id
    public UUID id = UUID.randomUUID();
    public String storageKey;
    public String originalName;
    public String contentType;
    public long size;

    @Enumerated(EnumType.STRING)
    public Visibility visibility;

    public OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);
}
