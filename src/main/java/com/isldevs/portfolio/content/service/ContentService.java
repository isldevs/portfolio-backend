package com.isldevs.portfolio.content.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isldevs.portfolio.audit.service.AuditService;
import com.isldevs.portfolio.common.dto.PageResponse;
import com.isldevs.portfolio.common.exception.ApiException;
import com.isldevs.portfolio.content.dto.ContentRequest;
import com.isldevs.portfolio.content.dto.ContentResponse;
import com.isldevs.portfolio.content.dto.RevisionResponse;
import com.isldevs.portfolio.content.entity.BlogRevision;
import com.isldevs.portfolio.content.entity.ContentItem;
import com.isldevs.portfolio.content.mapper.ContentMapper;
import com.isldevs.portfolio.content.repository.BlogRevisionRepository;
import com.isldevs.portfolio.content.repository.ContentRepository;
import com.isldevs.portfolio.content.validation.StructuredContent;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
  private final BlogRevisionRepository revisions;
  private final ContentMapper contentMapper;
  private final AuditService audit;
  private final ObjectMapper mapper;

  public ContentService(
      ContentRepository repo,
      BlogRevisionRepository revisions,
      ContentMapper contentMapper,
      AuditService audit,
      ObjectMapper mapper) {
    this.repo = repo;
    this.revisions = revisions;
    this.contentMapper = contentMapper;
    this.audit = audit;
    this.mapper = mapper;
  }

  public static void kind(String kind) {
    if (!KINDS.contains(kind)) throw ApiException.notFound();
  }

  @Transactional(readOnly = true)
  public PageResponse<ContentResponse> list(
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
    String[] sortParts = (sort == null ? "display_order,asc" : sort).split(",");
    sortParts[0] = com.isldevs.portfolio.common.json.ApiJsonNames.internal(sortParts[0]);
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
    return PageResponse.from(result.map(contentMapper::toDto));
  }

  private static String escape(String x) {
    return x.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  @Transactional(readOnly = true)
  public ContentResponse publicDetail(String kind, String slug) {
    kind(kind);
    return contentMapper.toDto(
        repo.findByKindAndSlugAndStatus(kind, slug, "PUBLISHED")
            .orElseThrow(ApiException::notFound));
  }

  @Transactional(readOnly = true)
  public ContentResponse get(String kind, UUID id) {
    return contentMapper.toDto(require(kind, id));
  }

  private ContentItem require(String kind, UUID id) {
    kind(kind);
    ContentItem c = repo.findById(id).orElseThrow(ApiException::notFound);
    if (!c.kind.equals(kind)) throw ApiException.notFound();
    return c;
  }

  public ContentResponse create(String kind, ContentRequest input) {
    kind(kind);
    ContentItem c = new ContentItem();
    c.kind = kind;
    apply(c, input);
    repo.saveAndFlush(c);
    revision(c);
    audit.record("CREATE", kind, c.id);
    return contentMapper.toDto(c);
  }

  public ContentResponse update(String kind, UUID id, ContentRequest input) {
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
    return contentMapper.toDto(c);
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
    return repo.countTaxonomyReferences(c.id, c.slug, c.title) > 0;
  }

  private void check(ContentItem c, Long version) {
    if (version == null || !version.equals(c.version))
      throw ApiException.conflict(
          "This record changed, or its version is missing. Reload before saving.");
  }

  private void apply(ContentItem c, ContentRequest in) {
    var metadata = com.isldevs.portfolio.common.json.ApiJsonNames.toInternal(in.data());
    StructuredContent.validate(in.body());
    if (metadata != null && metadata.toString().length() > 1000000)
      throw ApiException.invalid("Record metadata is too large.");
    if (metadata != null) {
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
        if (metadata.containsKey(field)) {
          Object value = metadata.get(field);
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
        if (metadata.containsKey(field) && !(metadata.get(field) instanceof String))
          throw ApiException.invalid(field + " must be text.");
      for (String field :
          List.of(
              "coverImage",
              "coverImageUrl",
              "githubUrl",
              "demoUrl",
              "architectureDiagram",
              "canonicalUrl"))
        if (metadata.get(field) instanceof String url
            && !url.isBlank()
            && !StructuredContent.safeUrl(url)) throw ApiException.invalid("Unsafe URL: " + field);
    }
    c.title = in.title();
    c.slug = in.slug();
    c.summary = in.summary() == null ? "" : in.summary();
    if (in.body() != null) c.body = new LinkedHashMap<>(in.body());
    if (metadata != null) c.data = new LinkedHashMap<>(metadata);
    c.featured = Boolean.TRUE.equals(in.featured());
    c.displayOrder = in.displayOrder() == null ? 0 : in.displayOrder();
    String next = in.status() == null ? "DRAFT" : in.status();
    if (next.equals("PUBLISHED") && c.publishedAt == null) c.publishedAt = Instant.now();
    c.status = next;
    c.updatedAt = Instant.now();
  }

  public ContentResponse publish(String kind, UUID id, Long version, boolean published) {
    ContentItem c = require(kind, id);
    check(c, version);
    c.status = published ? "PUBLISHED" : "DRAFT";
    if (published && c.publishedAt == null) c.publishedAt = Instant.now();
    repo.saveAndFlush(c);
    revision(c);
    audit.record(published ? "PUBLISH" : "UNPUBLISH", kind, id);
    return contentMapper.toDto(c);
  }

  public ContentResponse duplicate(String kind, UUID id) {
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
    return contentMapper.toDto(copy);
  }

  private void revision(ContentItem c) {
    if (!c.kind.equals("articles")) return;
    BlogRevision revision = new BlogRevision();
    revision.contentId = c.id;
    revision.snapshot =
        mapper.convertValue(contentMapper.toDto(c), new TypeReference<Map<String, Object>>() {});
    revision.actor = actor();
    revisions.save(revision);
  }

  private String actor() {
    var a =
        org.springframework.security.core.context.SecurityContextHolder.getContext()
            .getAuthentication();
    return a == null ? "system" : a.getName();
  }

  @Transactional(readOnly = true)
  public List<RevisionResponse> revisions(UUID id) {
    require("articles", id);
    return revisions.findTop100ByContentIdOrderByCreatedAtDesc(id).stream()
        .map(
            revision ->
                new RevisionResponse(
                    revision.id,
                    revision.createdAt,
                    revision.actor,
                    mapper.convertValue(
                        com.isldevs.portfolio.common.json.ApiJsonNames.toWire(revision.snapshot),
                        ContentResponse.class)))
        .toList();
  }

  public ContentResponse restore(UUID id, UUID revisionId, Long version) {
    ContentItem c = require("articles", id);
    check(c, version);
    BlogRevision found =
        revisions.findByIdAndContentId(revisionId, id).orElseThrow(ApiException::notFound);
    ContentRequest snapshot =
        mapper.convertValue(
            com.isldevs.portfolio.common.json.ApiJsonNames.toWire(found.snapshot),
            ContentRequest.class);
    apply(c, snapshot);
    c.status = "DRAFT";
    repo.saveAndFlush(c);
    revision(c);
    audit.record("RESTORE", "articles", id);
    return contentMapper.toDto(c);
  }
}
