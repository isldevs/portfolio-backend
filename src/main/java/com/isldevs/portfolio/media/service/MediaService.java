package com.isldevs.portfolio.media.service;

import com.isldevs.portfolio.audit.service.AuditService;
import com.isldevs.portfolio.common.dto.PageResponse;
import com.isldevs.portfolio.content.repository.ContentRepository;
import com.isldevs.portfolio.media.dto.MediaResponse;
import com.isldevs.portfolio.media.entity.Media;
import com.isldevs.portfolio.media.entity.Visibility;
import com.isldevs.portfolio.media.mapper.MediaMapper;
import com.isldevs.portfolio.media.repository.MediaAssetRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MediaService {

  private static final Logger LOG = LoggerFactory.getLogger(MediaService.class);
  private final MediaAssetRepository assets;
  private final MediaMapper mapper;
  private final ContentRepository content;
  private final AuditService audit;
  private final MediaStorage storage;
  private final MediaValidator validator;

  public MediaService(
      MediaAssetRepository assets,
      MediaMapper mapper,
      ContentRepository content,
      AuditService audit,
      MediaStorage storage,
      MediaValidator validator) {
    this.assets = assets;
    this.mapper = mapper;
    this.content = content;
    this.audit = audit;
    this.storage = storage;
    this.validator = validator;
  }

  @Transactional
  public MediaResponse upload(MultipartFile file, Visibility visibility, String actor) {
    if (file.getSize() <= 0 || file.getSize() > MediaValidator.MAX_SIZE)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "File must be between 1 byte and 10 MB");
    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot read upload");
    }
    MediaValidator.Validated type = validator.validate(file.getOriginalFilename(), bytes);
    UUID id = UUID.randomUUID();
    String key = id + "." + type.extension();
    String original = safeName(file.getOriginalFilename(), type.extension());
    try {
      storage.put(key, bytes);
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Media storage is unavailable");
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) removeFile(key);
          }
        });
    Media asset = new Media();
    asset.id = id;
    asset.storageKey = key;
    asset.originalName = original;
    asset.contentType = type.contentType();
    asset.size = bytes.length;
    asset.visibility = visibility;
    assets.saveAndFlush(asset);
    audit.record("UPLOAD", "media_assets", id, actor);
    return mapper.toDto(asset);
  }

  @Transactional(readOnly = true)
  public PageResponse<MediaResponse> list(String q, int page, int size) {
    page = Math.max(0, page);
    size = Math.max(1, Math.min(100, size));
    String term =
        "%"
            + (q == null
                ? ""
                : q.strip().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"))
            + "%";
    Specification<Media> spec =
        (root, query, cb) ->
            cb.like(cb.lower(root.get("originalName")), term.toLowerCase(Locale.ROOT), '\\');
    var result =
        assets.findAll(
            spec,
            PageRequest.of(
                page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"))));
    return PageResponse.from(result.map(mapper::toDto));
  }

  @Transactional(readOnly = true)
  public Download download(UUID id, boolean publicRequest) {
    Stored stored = getStored(id);
    if (publicRequest && stored.asset().visibility() != Visibility.PUBLIC)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found");
    try {
      Path path = storage.path(stored.key());
      if (!Files.isRegularFile(path))
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media file not found");
      return new Download(stored.asset(), path);
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media file not found");
    }
  }

  @Transactional
  public void delete(UUID id, String actor) {
    Stored stored = getStored(id);
    // Keep assets referenced by a draft, a published item or the profile. False positives are safer
    // than broken references.
    String term = "%" + id + "%";
    if (content.countMediaReferences(term) > 0)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Media is referenced by content or the profile; remove those references first");
    assets.deleteById(id);
    assets.flush();
    audit.record("DELETE", "media_assets", id, actor);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            removeFile(stored.key());
          }
        });
  }

  private Stored getStored(UUID id) {
    Media asset =
        assets
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
    return new Stored(mapper.toDto(asset), asset.storageKey);
  }

  private void removeFile(String key) {
    try {
      storage.delete(key);
    } catch (IOException e) {
      LOG.warn("Could not remove orphaned media file; storage maintenance required");
    }
  }

  static String safeName(String original, String extension) {
    if (original == null) return "upload." + extension;
    String name = original.replace('\\', '/');
    name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[^\\p{L}\\p{N} ._-]", "_");
    if (name.isBlank() || name.equals(".") || name.equals("..")) return "upload." + extension;
    return name.length() > 180 ? name.substring(0, 170) + "." + extension : name;
  }
}
