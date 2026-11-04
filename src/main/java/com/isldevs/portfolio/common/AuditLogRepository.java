package com.isldevs.portfolio.common;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
  List<AuditLog> findTop15ByOrderByCreatedAtDesc();

  List<AuditLog> findTop100ByOrderByCreatedAtDesc();
}
