package com.isldevs.portfolio.content.controller;

import com.isldevs.portfolio.common.dto.PageResponse;
import com.isldevs.portfolio.content.dto.ContentRequest;
import com.isldevs.portfolio.content.dto.ContentResponse;
import com.isldevs.portfolio.content.dto.RevisionResponse;
import com.isldevs.portfolio.content.dto.VersionRequest;
import com.isldevs.portfolio.content.service.ContentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ContentController {
  private final ContentService service;

  public ContentController(ContentService service) {
    this.service = service;
  }

  @GetMapping(
      "/public/{kind:projects|articles|case-studies|experiences|skills|categories|tags|settings}")
  public PageResponse<ContentResponse> publicList(
      @PathVariable String kind,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String tag,
      @RequestParam(required = false) String technology,
      @RequestParam(required = false) Boolean featured,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String sort) {
    return service.list(kind, true, q, category, tag, technology, null, featured, page, size, sort);
  }

  @GetMapping(
      "/public/{kind:projects|articles|case-studies|experiences|skills|categories|tags|settings}/{slug}")
  public ContentResponse publicDetail(@PathVariable String kind, @PathVariable String slug) {
    return service.publicDetail(kind, slug);
  }

  @GetMapping(
      "/admin/{kind:projects|articles|case-studies|experiences|skills|categories|tags|settings}")
  public PageResponse<ContentResponse> list(
      @PathVariable String kind,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String tag,
      @RequestParam(required = false) String technology,
      @RequestParam(required = false) Boolean featured,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String sort) {
    return service.list(
        kind, false, q, category, tag, technology, status, featured, page, size, sort);
  }

  @GetMapping(
      "/admin/{kind:projects|articles|case-studies|experiences|skills|categories|tags|settings}/{id}")
  public ContentResponse get(@PathVariable String kind, @PathVariable UUID id) {
    return service.get(kind, id);
  }

  @PostMapping(
      "/admin/{kind:projects|articles|case-studies|experiences|skills|categories|tags|settings}")
  @ResponseStatus(HttpStatus.CREATED)
  public ContentResponse create(
      @PathVariable String kind, @Valid @RequestBody ContentRequest input) {
    return service.create(kind, input);
  }

  @PutMapping(
      "/admin/{kind:projects|articles|case-studies|experiences|skills|categories|tags|settings}/{id}")
  public ContentResponse update(
      @PathVariable String kind, @PathVariable UUID id, @Valid @RequestBody ContentRequest input) {
    return service.update(kind, id, input);
  }

  @DeleteMapping(
      "/admin/{kind:projects|articles|case-studies|experiences|skills|categories|tags|settings}/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String kind, @PathVariable UUID id, @RequestParam Long version) {
    service.delete(kind, id, version);
  }

  @PostMapping("/admin/{kind:projects|articles|case-studies}/{id}/publish")
  public ContentResponse publish(
      @PathVariable String kind, @PathVariable UUID id, @RequestBody VersionRequest body) {
    return service.publish(kind, id, body.version(), true);
  }

  @PostMapping("/admin/{kind:projects|articles|case-studies}/{id}/unpublish")
  public ContentResponse unpublish(
      @PathVariable String kind, @PathVariable UUID id, @RequestBody VersionRequest body) {
    return service.publish(kind, id, body.version(), false);
  }

  @PostMapping("/admin/{kind:projects|articles|case-studies}/{id}/duplicate")
  @ResponseStatus(HttpStatus.CREATED)
  public ContentResponse duplicate(@PathVariable String kind, @PathVariable UUID id) {
    return service.duplicate(kind, id);
  }

  @GetMapping("/admin/articles/{id}/revisions")
  public List<RevisionResponse> revisions(@PathVariable UUID id) {
    return service.revisions(id);
  }

  @PostMapping("/admin/articles/{id}/revisions/{revisionId}/restore")
  public ContentResponse restore(
      @PathVariable UUID id, @PathVariable UUID revisionId, @RequestBody VersionRequest body) {
    return service.restore(id, revisionId, body.version());
  }
}
