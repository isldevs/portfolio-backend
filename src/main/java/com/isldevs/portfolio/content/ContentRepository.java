package com.isldevs.portfolio.content;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ContentRepository
    extends JpaRepository<ContentItem, UUID>, JpaSpecificationExecutor<ContentItem> {
  Optional<ContentItem> findByKindAndSlugAndStatus(String kind, String slug, String status);

  long countByKind(String kind);

  long countByKindAndStatus(String kind, String status);

  boolean existsByKindAndSlug(String kind, String slug);
}
