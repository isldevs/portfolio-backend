package com.isldevs.portfolio.content;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ContentController {
  private final ContentService service;

  public ContentController(ContentService service) {
    this.service = service;
  }

  @GetMapping(
      "/public/{kind:projects|articles|case-studies|experiences|skills|categories|tags|settings}")
  public Map<String, Object> publicList(
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
  public ContentDto publicDetail(@PathVariable String kind, @PathVariable String slug) {
    return service.publicDetail(kind, slug);
  }

  @GetMapping(
      "/admin/{kind:projects|articles|case-studies|experiences|skills|categories|tags|settings}")
  public Map<String, Object> list(
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
  public ContentDto get(@PathVariable String kind, @PathVariable UUID id) {
    return service.get(kind, id);
  }

  @PostMapping(
      "/admin/{kind:projects|articles|case-studies|experiences|skills|categories|tags|settings}")
  @ResponseStatus(HttpStatus.CREATED)
  public ContentDto create(@PathVariable String kind, @Valid @RequestBody ContentDto input) {
    return service.create(kind, input);
  }

  @PutMapping(
      "/admin/{kind:projects|articles|case-studies|experiences|skills|categories|tags|settings}/{id}")
  public ContentDto update(
      @PathVariable String kind, @PathVariable UUID id, @Valid @RequestBody ContentDto input) {
    return service.update(kind, id, input);
  }

  @DeleteMapping(
      "/admin/{kind:projects|articles|case-studies|experiences|skills|categories|tags|settings}/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String kind, @PathVariable UUID id, @RequestParam Long version) {
    service.delete(kind, id, version);
  }

  public record VersionBody(Long version) {}

  @PostMapping("/admin/{kind:projects|articles|case-studies}/{id}/publish")
  public ContentDto publish(
      @PathVariable String kind, @PathVariable UUID id, @RequestBody VersionBody body) {
    return service.publish(kind, id, body.version(), true);
  }

  @PostMapping("/admin/{kind:projects|articles|case-studies}/{id}/unpublish")
  public ContentDto unpublish(
      @PathVariable String kind, @PathVariable UUID id, @RequestBody VersionBody body) {
    return service.publish(kind, id, body.version(), false);
  }

  @PostMapping("/admin/{kind:projects|articles|case-studies}/{id}/duplicate")
  @ResponseStatus(HttpStatus.CREATED)
  public ContentDto duplicate(@PathVariable String kind, @PathVariable UUID id) {
    return service.duplicate(kind, id);
  }

  @GetMapping("/admin/articles/{id}/revisions")
  public List<Map<String, Object>> revisions(@PathVariable UUID id) {
    return service.revisions(id);
  }

  @PostMapping("/admin/articles/{id}/revisions/{revisionId}/restore")
  public ContentDto restore(
      @PathVariable UUID id, @PathVariable UUID revisionId, @RequestBody VersionBody body) {
    return service.restore(id, revisionId, body.version());
  }
}
