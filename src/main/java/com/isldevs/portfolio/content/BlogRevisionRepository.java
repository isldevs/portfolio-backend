package com.isldevs.portfolio.content;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogRevisionRepository extends JpaRepository<BlogRevision, UUID> {
  List<BlogRevision> findTop100ByContentIdOrderByCreatedAtDesc(UUID contentId);

  Optional<BlogRevision> findByIdAndContentId(UUID id, UUID contentId);
}
