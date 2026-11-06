package com.isldevs.portfolio.content.repository;

import com.isldevs.portfolio.content.entity.BlogRevision;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogRevisionRepository extends JpaRepository<BlogRevision, UUID> {
  List<BlogRevision> findTop100ByContentIdOrderByCreatedAtDesc(UUID contentId);

  Optional<BlogRevision> findByIdAndContentId(UUID id, UUID contentId);
}
