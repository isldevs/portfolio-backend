package com.isldevs.portfolio.content.dto;

import com.isldevs.portfolio.audit.dto.AuditResponse;
import java.util.List;

@com.fasterxml.jackson.databind.annotation.JsonNaming(
    com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DashboardResponse(
    long totalProjects,
    long publishedProjects,
    long totalArticles,
    long publishedArticles,
    long draftArticles,
    long unreadMessages,
    List<AuditResponse> recentActivity) {}
