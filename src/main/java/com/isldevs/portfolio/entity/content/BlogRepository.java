package com.isldevs.portfolio.entity.content;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogRepository extends JpaRepository<Blog, UUID> {
  List<Blog> findTop100ByContentIdOrderByCreatedAtDesc(UUID contentId);

  Optional<Blog> findByIdAndContentId(UUID id, UUID contentId);
}
