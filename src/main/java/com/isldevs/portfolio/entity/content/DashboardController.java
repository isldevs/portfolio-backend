package com.isldevs.portfolio.entity.content;

import com.isldevs.portfolio.common.AuditDto;
import com.isldevs.portfolio.common.AuditLogRepository;
import com.isldevs.portfolio.common.AuditMapper;
import com.isldevs.portfolio.entity.contact.ContactRepository;
import com.isldevs.portfolio.entity.contact.Status;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class DashboardController {

    private final ContentRepository content;
    private final ContactRepository messages;
    private final AuditLogRepository logs;
    private final AuditMapper mapper;

    public DashboardController(ContentRepository content,
                               ContactRepository messages,
                               AuditLogRepository logs,
                               AuditMapper mapper) {
        this.content = content;
        this.messages = messages;
        this.logs = logs;
        this.mapper = mapper;
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        return Map.of(
                "totalProjects", content.countByKind("projects"),
                "publishedProjects", content.countByKindAndStatus("projects", "PUBLISHED"),
                "totalArticles", content.countByKind("articles"),
                "publishedArticles", content.countByKindAndStatus("articles", "PUBLISHED"),
                "draftArticles", content.countByKindAndStatus("articles", "DRAFT"),
                "unreadMessages", messages.countByStatus(Status.UNREAD),
                "recentActivity", logs.findTop15ByOrderByCreatedAtDesc().stream().map(mapper::toDto).toList()
        );
    }

    @GetMapping("/audit")
    @Transactional(readOnly = true)
    public List<AuditDto> audit() {
        return logs.findTop100ByOrderByCreatedAtDesc().stream().map(mapper::toDto).toList();
    }
}
