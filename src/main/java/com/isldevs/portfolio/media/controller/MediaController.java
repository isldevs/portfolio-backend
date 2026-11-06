package com.isldevs.portfolio.media.controller;

import com.isldevs.portfolio.common.dto.PageResponse;
import com.isldevs.portfolio.media.dto.MediaResponse;
import com.isldevs.portfolio.media.entity.Visibility;
import com.isldevs.portfolio.media.service.Download;
import com.isldevs.portfolio.media.service.MediaService;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class MediaController {

  private final MediaService service;

  public MediaController(MediaService service) {
    this.service = service;
  }

  @GetMapping("/admin/media")
  public PageResponse<MediaResponse> list(
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.list(q, page, size);
  }

  @PostMapping(value = "/admin/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public MediaResponse upload(
      @RequestPart("file") MultipartFile file,
      @RequestParam(defaultValue = "PUBLIC") Visibility visibility,
      Principal principal) {
    return service.upload(file, visibility, principal.getName());
  }

  @DeleteMapping("/admin/media/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id, Principal principal) {
    service.delete(id, principal.getName());
  }

  @GetMapping("/public/media/{id}")
  public ResponseEntity<Resource> publicDownload(@PathVariable UUID id) {
    return response(service.download(id, true), true);
  }

  @GetMapping("/admin/media/{id}/content")
  public ResponseEntity<Resource> privateDownload(@PathVariable UUID id) {
    return response(service.download(id, false), false);
  }

  private ResponseEntity<Resource> response(Download download, boolean publicRequest) {
    MediaResponse asset = download.asset();
    ContentDisposition disposition =
        (asset.contentType().equals("application/pdf")
                ? ContentDisposition.attachment()
                : ContentDisposition.inline())
            .filename(asset.originalName(), StandardCharsets.UTF_8)
            .build();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(asset.contentType()))
        .contentLength(asset.size())
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .header(
            HttpHeaders.CACHE_CONTROL,
            publicRequest ? "public, max-age=86400, immutable" : "private, no-store")
        .header("X-Content-Type-Options", "nosniff")
        .header("Content-Security-Policy", "default-src 'none'; sandbox")
        .body(new FileSystemResource(download.path()));
  }
}
