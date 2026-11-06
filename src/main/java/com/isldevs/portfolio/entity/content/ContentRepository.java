package com.isldevs.portfolio.entity.content;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ContentRepository extends JpaRepository<ContentItem, UUID>, JpaSpecificationExecutor<ContentItem> {
  Optional<ContentItem> findByKindAndSlugAndStatus(String kind, String slug, String status);

  long countByKind(String kind);

  long countByKindAndStatus(String kind, String status);

  boolean existsByKindAndSlug(String kind, String slug);

  @Query(
      value =
          "SELECT count(*) FROM contents WHERE id <> :id AND (data->>'category' IN"
              + " (:slug,:title) OR jsonb_exists(data->'tags',:slug) OR"
              + " jsonb_exists(data->'tags',:title))",
      nativeQuery = true)
  long countTaxonomyReferences(UUID id, String slug, String title);

  @Query(
      value =
          "SELECT (SELECT count(*) FROM contents WHERE body::text LIKE :term OR data::text"
              + " LIKE :term) + (SELECT count(*) FROM profiles WHERE data::text LIKE :term) +"
              + " (SELECT count(*) FROM blogs WHERE snapshot::text LIKE :term)",
      nativeQuery = true)
  long countMediaReferences(String term);
}
