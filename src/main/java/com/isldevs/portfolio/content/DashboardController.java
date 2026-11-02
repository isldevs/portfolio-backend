package com.isldevs.portfolio.content;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class DashboardController {
  private final ContentRepository content;
  private final JdbcTemplate jdbc;

  public DashboardController(ContentRepository content, JdbcTemplate jdbc) {
    this.content = content;
    this.jdbc = jdbc;
  }

  @GetMapping("/dashboard")
  @Transactional(readOnly = true)
  public Map<String, Object> dashboard() {
    return Map.of(
        "totalProjects",
        content.countByKind("projects"),
        "publishedProjects",
        content.countByKindAndStatus("projects", "PUBLISHED"),
        "totalArticles",
        content.countByKind("articles"),
        "publishedArticles",
        content.countByKindAndStatus("articles", "PUBLISHED"),
        "draftArticles",
        content.countByKindAndStatus("articles", "DRAFT"),
        "unreadMessages",
        jdbc.queryForObject(
            "SELECT count(*) FROM contact_messages WHERE status='UNREAD'", Long.class),
        "recentActivity",
        jdbc.queryForList(
            "SELECT id,action,resource,resource_id AS \"resourceId\",actor,created_at AS"
                + " \"createdAt\" FROM audit_logs ORDER BY created_at DESC LIMIT 15"));
  }

  @GetMapping("/audit")
  public List<Map<String, Object>> audit() {
    return jdbc.queryForList(
        "SELECT id,action,resource,resource_id AS \"resourceId\",actor,created_at AS \"createdAt\""
            + " FROM audit_logs ORDER BY created_at DESC LIMIT 100");
  }
}
