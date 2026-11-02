package com.isldevs.portfolio.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MediaService {
  private static final Logger LOG = LoggerFactory.getLogger(MediaService.class);
  private final JdbcTemplate jdbc;
  private final MediaStorage storage;
  private final MediaValidator validator;

  public MediaService(JdbcTemplate jdbc, MediaStorage storage, MediaValidator validator) {
    this.jdbc = jdbc;
    this.storage = storage;
    this.validator = validator;
  }

  public enum Visibility {
    PUBLIC,
    PRIVATE
  }

  public record Asset(
      UUID id,
      String url,
      String originalName,
      String contentType,
      long size,
      Visibility visibility,
      OffsetDateTime createdAt) {}

  private record Stored(Asset asset, String key) {}

  public record Download(Asset asset, Path path) {}

  private final RowMapper<Stored> mapper =
      (rs, row) -> {
        UUID id = rs.getObject("id", UUID.class);
        Visibility visibility = Visibility.valueOf(rs.getString("visibility"));
        String url =
            visibility == Visibility.PUBLIC
                ? "/api/v1/public/media/" + id
                : "/api/v1/admin/media/" + id + "/content";
        return new Stored(
            new Asset(
                id,
                url,
                rs.getString("original_name"),
                rs.getString("content_type"),
                rs.getLong("size"),
                visibility,
                rs.getObject("created_at", OffsetDateTime.class)),
            rs.getString("storage_key"));
      };

  @Transactional
  public Asset upload(MultipartFile file, Visibility visibility, String actor) {
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
    jdbc.update(
        "INSERT INTO media_assets(id,storage_key,original_name,content_type,size,visibility) VALUES"
            + " (?,?,?,?,?,?)",
        id,
        key,
        original,
        type.contentType(),
        bytes.length,
        visibility.name());
    audit("UPLOAD", id, actor);
    return getStored(id).asset();
  }

  @Transactional(readOnly = true)
  public Map<String, Object> list(String q, int page, int size) {
    page = Math.max(0, page);
    size = Math.max(1, Math.min(100, size));
    String term =
        "%"
            + (q == null
                ? ""
                : q.strip().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"))
            + "%";
    Long count =
        jdbc.queryForObject(
            "SELECT count(*) FROM media_assets WHERE original_name ILIKE ?", Long.class, term);
    List<Asset> assets =
        jdbc
            .query(
                "SELECT * FROM media_assets WHERE original_name ILIKE ? ORDER BY created_at DESC,id"
                    + " LIMIT ? OFFSET ?",
                mapper,
                term,
                size,
                (long) page * size)
            .stream()
            .map(Stored::asset)
            .toList();
    long total = count == null ? 0 : count;
    return Map.of(
        "content",
        assets,
        "totalElements",
        total,
        "totalPages",
        (total + size - 1) / size,
        "number",
        page,
        "size",
        size);
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
    Long references =
        jdbc.queryForObject(
            "SELECT (SELECT count(*) FROM content_items WHERE body::text LIKE ? OR data::text LIKE"
                + " ?) + (SELECT count(*) FROM profiles WHERE data::text LIKE ?) + (SELECT count(*)"
                + " FROM blog_revisions WHERE snapshot::text LIKE ?)",
            Long.class,
            term,
            term,
            term,
            term);
    if (references != null && references > 0)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Media is referenced by content or the profile; remove those references first");
    jdbc.update("DELETE FROM media_assets WHERE id=?", id);
    audit("DELETE", id, actor);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            removeFile(stored.key());
          }
        });
  }

  private Stored getStored(UUID id) {
    return jdbc.query("SELECT * FROM media_assets WHERE id=?", mapper, id).stream()
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
  }

  private void audit(String action, UUID id, String actor) {
    jdbc.update(
        "INSERT INTO audit_logs(id,action,resource,resource_id,actor,created_at) VALUES"
            + " (?,?,?,?,?,CURRENT_TIMESTAMP)",
        UUID.randomUUID(),
        action,
        "media_assets",
        id.toString(),
        actor);
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
