package com.isldevs.portfolio.contact;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContactService {
  private final JdbcTemplate jdbc;

  public ContactService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public enum Status {
    UNREAD,
    READ,
    RESOLVED,
    ARCHIVED
  }

  public record Message(
      UUID id,
      String name,
      String email,
      String subject,
      String message,
      Status status,
      long version,
      OffsetDateTime createdAt) {}

  private final RowMapper<Message> mapper =
      (rs, row) ->
          new Message(
              rs.getObject("id", UUID.class),
              rs.getString("name"),
              rs.getString("email"),
              rs.getString("subject"),
              rs.getString("message"),
              Status.valueOf(rs.getString("status")),
              rs.getLong("version"),
              rs.getObject("created_at", OffsetDateTime.class));

  @Transactional
  public UUID submit(ContactController.Submission form) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO contact_messages(id,name,email,subject,message) VALUES (?,?,?,?,?)",
        id,
        form.name().strip(),
        form.email().strip(),
        form.subject().strip(),
        form.message().strip());
    return id;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> list(String q, Status status, int page, int size) {
    page = Math.max(0, page);
    size = Math.max(1, Math.min(100, size));
    StringBuilder where = new StringBuilder(" WHERE 1=1");
    List<Object> args = new ArrayList<>();
    if (q != null && !q.isBlank()) {
      where.append(" AND (name ILIKE ? OR email ILIKE ? OR subject ILIKE ? OR message ILIKE ?)");
      String term =
          "%" + q.strip().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
      for (int i = 0; i < 4; i++) args.add(term);
    }
    if (status != null) {
      where.append(" AND status = ?");
      args.add(status.name());
    }
    Long total =
        jdbc.queryForObject(
            "SELECT count(*) FROM contact_messages" + where, Long.class, args.toArray());
    List<Object> paged = new ArrayList<>(args);
    paged.add(size);
    paged.add((long) page * size);
    List<Message> messages =
        jdbc.query(
            "SELECT * FROM contact_messages"
                + where
                + " ORDER BY created_at DESC,id LIMIT ? OFFSET ?",
            mapper,
            paged.toArray());
    long count = total == null ? 0 : total;
    return Map.of(
        "content",
        messages,
        "totalElements",
        count,
        "totalPages",
        (count + size - 1) / size,
        "number",
        page,
        "size",
        size);
  }

  @Transactional(readOnly = true)
  public Message get(UUID id) {
    return jdbc.query("SELECT * FROM contact_messages WHERE id=?", mapper, id).stream()
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
  }

  @Transactional
  public Message update(UUID id, Status status, long version, String actor) {
    int count =
        jdbc.update(
            "UPDATE contact_messages SET status=?,version=version+1,updated_at=CURRENT_TIMESTAMP"
                + " WHERE id=? AND version=?",
            status.name(),
            id,
            version);
    if (count == 0) {
      get(id);
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Message changed; reload before saving");
    }
    audit("UPDATE", id, actor);
    return get(id);
  }

  @Transactional
  public void delete(UUID id, String actor) {
    if (jdbc.update("DELETE FROM contact_messages WHERE id=?", id) == 0)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found");
    audit("DELETE", id, actor);
  }

  private void audit(String action, UUID id, String actor) {
    jdbc.update(
        "INSERT INTO audit_logs(id,action,resource,resource_id,actor,created_at) VALUES"
            + " (?,?,?,?,?,CURRENT_TIMESTAMP)",
        UUID.randomUUID(),
        action,
        "contact_messages",
        id.toString(),
        actor);
  }
}
