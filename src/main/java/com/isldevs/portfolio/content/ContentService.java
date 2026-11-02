package com.isldevs.portfolio.content;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isldevs.portfolio.common.*;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ContentService {
  public static final Set<String> KINDS =
      Set.of(
          "projects",
          "articles",
          "case-studies",
          "experiences",
          "skills",
          "categories",
          "tags",
          "settings");
  private final ContentRepository repo;
  private final JdbcTemplate jdbc;
  private final AuditService audit;
  private final ObjectMapper mapper;

  public ContentService(
      ContentRepository repo, JdbcTemplate jdbc, AuditService audit, ObjectMapper mapper) {
    this.repo = repo;
    this.jdbc = jdbc;
    this.audit = audit;
    this.mapper = mapper;
  }

  public static void kind(String kind) {
    if (!KINDS.contains(kind)) throw ApiException.notFound();
  }

  @Transactional(readOnly = true)
  public Map<String, Object> list(
      String kind,
      boolean publicOnly,
      String q,
      String category,
      String tag,
      String technology,
      String status,
      Boolean featured,
      int page,
      int size,
      String sort) {
    kind(kind);
    int boundedSize = Math.clamp(size, 1, 100);
    int boundedPage = Math.clamp(page, 0, 100000);
    String[] sortParts = (sort == null ? "displayOrder,asc" : sort).split(",");
    String field =
        Set.of("title", "createdAt", "updatedAt", "publishedAt", "displayOrder")
                .contains(sortParts[0])
            ? sortParts[0]
            : "displayOrder";
    Sort order =
        Sort.by(
                sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC,
                field)
            .and(Sort.by(Sort.Direction.DESC, "createdAt"));
    Specification<ContentItem> spec =
        (root, query, cb) -> {
          List<Predicate> ps = new ArrayList<>();
          ps.add(cb.equal(root.get("kind"), kind));
          if (featured != null) ps.add(cb.equal(root.get("featured"), featured));
          if (publicOnly) ps.add(cb.equal(root.get("status"), "PUBLISHED"));
          else if (status != null && !status.isBlank())
            ps.add(cb.equal(root.get("status"), status));
          if (q != null && !q.isBlank()) {
            String like = "%" + escape(q.toLowerCase(Locale.ROOT)) + "%";
            ps.add(
                cb.or(
                    cb.like(cb.lower(root.get("title")), like, '\\'),
                    cb.like(cb.lower(root.get("summary")), like, '\\')));
          }
          if (category != null && !category.isBlank())
            ps.add(
                cb.equal(
                    cb.function(
                        "jsonb_extract_path_text",
                        String.class,
                        root.get("data"),
                        cb.literal("category")),
                    category));
          if (tag != null && !tag.isBlank())
            ps.add(
                cb.like(
                    cb.function(
                        "jsonb_extract_path_text",
                        String.class,
                        root.get("data"),
                        cb.literal("tags")),
                    "%\"" + escape(tag) + "\"%",
                    '\\'));
          if (technology != null && !technology.isBlank())
            ps.add(
                cb.like(
                    cb.function(
                        "jsonb_extract_path_text",
                        String.class,
                        root.get("data"),
                        cb.literal("technologies")),
                    "%\"" + escape(technology) + "\"%",
                    '\\'));
          return cb.and(ps.toArray(Predicate[]::new));
        };
    Page<ContentItem> result = repo.findAll(spec, PageRequest.of(boundedPage, boundedSize, order));
    return Map.of(
        "content",
        result.getContent().stream().map(ContentDto::from).toList(),
        "totalElements",
        result.getTotalElements(),
        "totalPages",
        result.getTotalPages(),
        "number",
        result.getNumber(),
        "size",
        result.getSize());
  }

  private static String escape(String x) {
    return x.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  @Transactional(readOnly = true)
  public ContentDto publicDetail(String kind, String slug) {
    kind(kind);
    return ContentDto.from(
        repo.findByKindAndSlugAndStatus(kind, slug, "PUBLISHED")
            .orElseThrow(ApiException::notFound));
  }

  @Transactional(readOnly = true)
  public ContentDto get(String kind, UUID id) {
    return ContentDto.from(require(kind, id));
  }

  private ContentItem require(String kind, UUID id) {
    kind(kind);
    ContentItem c = repo.findById(id).orElseThrow(ApiException::notFound);
    if (!c.kind.equals(kind)) throw ApiException.notFound();
    return c;
  }

  public ContentDto create(String kind, ContentDto input) {
    kind(kind);
    ContentItem c = new ContentItem();
    c.kind = kind;
    apply(c, input);
    repo.saveAndFlush(c);
    revision(c);
    audit.record("CREATE", kind, c.id);
    return ContentDto.from(c);
  }

  public ContentDto update(String kind, UUID id, ContentDto input) {
    ContentItem c = require(kind, id);
    check(c, input.version());
    if (Set.of("categories", "tags").contains(kind)
        && (!c.slug.equals(input.slug()) || !c.title.equals(input.title()))
        && referenced(c))
      throw ApiException.conflict(
          "Update content references before renaming a referenced taxonomy.");
    apply(c, input);
    repo.saveAndFlush(c);
    revision(c);
    audit.record("UPDATE", kind, id);
    return ContentDto.from(c);
  }

  public void delete(String kind, UUID id, Long version) {
    ContentItem c = require(kind, id);
    check(c, version);
    if (Set.of("categories", "tags").contains(kind) && referenced(c))
      throw ApiException.conflict(
          "This taxonomy is still referenced. Update the referencing content first.");
    repo.delete(c);
    repo.flush();
    audit.record("DELETE", kind, id);
  }

  private boolean referenced(ContentItem c) {
    Long refs =
        jdbc.queryForObject(
            "SELECT count(*) FROM content_items WHERE id <> ? AND (data->>'category' IN (?,?) OR"
                + " jsonb_exists(data->'tags',?) OR jsonb_exists(data->'tags',?))",
            Long.class,
            c.id,
            c.slug,
            c.title,
            c.slug,
            c.title);
    return refs != null && refs > 0;
  }

  private void check(ContentItem c, Long version) {
    if (version == null || !version.equals(c.version))
      throw ApiException.conflict(
          "This record changed, or its version is missing. Reload before saving.");
  }

  private void apply(ContentItem c, ContentDto in) {
    StructuredContent.validate(in.body());
    if (in.data() != null && in.data().toString().length() > 1000000)
      throw ApiException.invalid("Record metadata is too large.");
    if (in.data() != null) {
      for (String field :
          List.of(
              "technologies",
              "tags",
              "gallery",
              "features",
              "responsibilities",
              "achievements",
              "services",
              "tools"))
        if (in.data().containsKey(field)) {
          Object value = in.data().get(field);
          if (!(value instanceof List<?> list)
              || list.size() > 200
              || list.stream().anyMatch(v -> !(v instanceof String str) || str.length() > 4000))
            throw ApiException.invalid(field + " must be an array of strings.");
        }
      for (String field :
          List.of(
              "category",
              "author",
              "coverImage",
              "coverImageUrl",
              "githubUrl",
              "demoUrl",
              "architectureDiagram",
              "problemStatement",
              "solution",
              "architecture",
              "securityConsiderations",
              "databaseDesign",
              "engineeringDecisions",
              "challenges",
              "tradeOffs",
              "lessonsLearned",
              "description",
              "problem",
              "outcomes",
              "technicalApproach",
              "name",
              "company",
              "location",
              "employmentType",
              "startDate",
              "endDate",
              "icon",
              "canonicalUrl",
              "seoTitle",
              "seoDescription"))
        if (in.data().containsKey(field) && !(in.data().get(field) instanceof String))
          throw ApiException.invalid(field + " must be text.");
      for (String field :
          List.of(
              "coverImage",
              "coverImageUrl",
              "githubUrl",
              "demoUrl",
              "architectureDiagram",
              "canonicalUrl"))
        if (in.data().get(field) instanceof String url
            && !url.isBlank()
            && !StructuredContent.safeUrl(url)) throw ApiException.invalid("Unsafe URL: " + field);
    }
    c.title = in.title();
    c.slug = in.slug();
    c.summary = in.summary() == null ? "" : in.summary();
    if (in.body() != null) c.body = new LinkedHashMap<>(in.body());
    if (in.data() != null) c.data = new LinkedHashMap<>(in.data());
    c.featured = Boolean.TRUE.equals(in.featured());
    c.displayOrder = in.displayOrder() == null ? 0 : in.displayOrder();
    String next = in.status() == null ? "DRAFT" : in.status();
    if (next.equals("PUBLISHED") && c.publishedAt == null) c.publishedAt = Instant.now();
    c.status = next;
    c.updatedAt = Instant.now();
  }

  public ContentDto publish(String kind, UUID id, Long version, boolean published) {
    ContentItem c = require(kind, id);
    check(c, version);
    c.status = published ? "PUBLISHED" : "DRAFT";
    if (published && c.publishedAt == null) c.publishedAt = Instant.now();
    repo.saveAndFlush(c);
    revision(c);
    audit.record(published ? "PUBLISH" : "UNPUBLISH", kind, id);
    return ContentDto.from(c);
  }

  public ContentDto duplicate(String kind, UUID id) {
    ContentItem original = require(kind, id);
    ContentItem copy = new ContentItem();
    copy.kind = kind;
    String titleSuffix = " (copy)";
    int titleEnd = Math.min(original.title.length(), 240 - titleSuffix.length());
    if (titleEnd > 0 && Character.isHighSurrogate(original.title.charAt(titleEnd - 1))) {
      titleEnd--;
    }
    copy.title = original.title.substring(0, titleEnd) + titleSuffix;
    String slugSuffix = "-copy-" + UUID.randomUUID().toString().substring(0, 8);
    String slugPrefix =
        original.slug.substring(0, Math.min(original.slug.length(), 180 - slugSuffix.length()));
    copy.slug = slugPrefix.replaceFirst("-+$", "") + slugSuffix;
    copy.summary = original.summary;
    copy.body = new LinkedHashMap<>(original.body);
    copy.data = new LinkedHashMap<>(original.data);
    copy.status = "DRAFT";
    repo.saveAndFlush(copy);
    revision(copy);
    audit.record("DUPLICATE", kind, copy.id);
    return ContentDto.from(copy);
  }

  private void revision(ContentItem c) {
    if (!c.kind.equals("articles")) return;
    try {
      jdbc.update(
          "INSERT INTO blog_revisions(id,content_id,snapshot,actor,created_at) VALUES"
              + " (?,?,?::jsonb,?,clock_timestamp())",
          UUID.randomUUID(),
          c.id,
          mapper.writeValueAsString(ContentDto.from(c)),
          actor());
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  private String actor() {
    var a =
        org.springframework.security.core.context.SecurityContextHolder.getContext()
            .getAuthentication();
    return a == null ? "system" : a.getName();
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> revisions(UUID id) {
    require("articles", id);
    return jdbc
        .queryForList(
            "SELECT id,created_at AS \"createdAt\",actor,snapshot FROM blog_revisions WHERE"
                + " content_id=? ORDER BY created_at DESC LIMIT 100",
            id)
        .stream()
        .map(
            row -> {
              try {
                var m = new LinkedHashMap<String, Object>(row);
                m.put(
                    "snapshot",
                    mapper.readValue(
                        row.get("snapshot").toString(),
                        new TypeReference<Map<String, Object>>() {}));
                return (Map<String, Object>) m;
              } catch (Exception e) {
                throw new IllegalStateException(e);
              }
            })
        .toList();
  }

  public ContentDto restore(UUID id, UUID revisionId, Long version) {
    ContentItem c = require("articles", id);
    check(c, version);
    List<String> found =
        jdbc.query(
            "SELECT snapshot::text FROM blog_revisions WHERE id=? AND content_id=?",
            (rs, n) -> rs.getString(1),
            revisionId,
            id);
    if (found.isEmpty()) throw ApiException.notFound();
    try {
      ContentDto snapshot = mapper.readValue(found.getFirst(), ContentDto.class);
      apply(c, snapshot);
      c.status = "DRAFT";
      repo.saveAndFlush(c);
      revision(c);
      audit.record("RESTORE", "articles", id);
      return ContentDto.from(c);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
