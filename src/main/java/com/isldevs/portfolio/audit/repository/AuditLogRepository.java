package com.isldevs.portfolio.audit.repository;

import com.isldevs.portfolio.audit.entity.AuditLog;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
  List<AuditLog> findTop15ByOrderByCreatedAtDesc();

  List<AuditLog> findTop100ByOrderByCreatedAtDesc();
}
