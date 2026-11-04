package com.isldevs.portfolio.content;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class StructuredContentTest {
  @Test
  void rejectsMalformedDocumentsBeforeTheyCanBreakPublicRendering() {
    assertThatThrownBy(
            () -> StructuredContent.validate(Map.of("type", "doc", "content", "invalid")))
        .hasMessageContaining("array");
    assertThatThrownBy(
            () ->
                StructuredContent.validate(
                    Map.of(
                        "type",
                        "doc",
                        "content",
                        List.of(Map.of("type", "paragraph", "content", 17)))))
        .hasMessageContaining("array");
    assertThatThrownBy(
            () ->
                StructuredContent.validate(
                    Map.of("type", "doc", "content", List.of(Map.of("type", "text", "text", 17)))))
        .hasMessageContaining("string");
  }

  @Test
  void disallowsExecutableAndAmbiguousUrls() {
    for (String url :
        List.of(
            "javascript:alert(1)",
            "//evil.example/path",
            "/\\evil.example",
            "https://user@evil.example",
            "https://example.com\n")) assertThat(StructuredContent.safeUrl(url)).as(url).isFalse();
    for (String url :
        List.of("/api/v1/public/media/a", "https://github.com/isldevs", "mailto:owner@example.com"))
      assertThat(StructuredContent.safeUrl(url)).as(url).isTrue();
  }

  @Test
  void acceptsOnlyTheTrustedPortraitAssetAndSafeLocalMediaPathsForImages() {
    for (String url :
        List.of(
            "/images/yi-sivlay.png", "/api/v1/public/media/f662b083-066b-4321-b5d1-921fef7911f8")) {
      assertThat(StructuredContent.safeUrl(url)).as(url).isTrue();
      StructuredContent.validate(imageDocument(url));
    }
    for (String url :
        List.of(
            "//evil.example/portrait.png",
            "/images/other.png",
            "/images/../yi-sivlay.png",
            "/images/yi-sivlay.png/../other.png",
            "/images/yi-sivlay.png?redirect=evil",
            "/api/v1/public/media/../private",
            "/api/v1/public/media/%2e%2e/private",
            "/api/v1/public/media/id%2f..%2fprivate")) {
      assertThat(StructuredContent.safeUrl(url)).as(url).isFalse();
      assertThatThrownBy(() -> StructuredContent.validate(imageDocument(url)))
          .as(url)
          .hasMessageContaining("Unsafe content URL");
    }
  }

  private static Map<String, Object> imageDocument(String src) {
    return Map.of(
        "type", "doc", "content", List.of(Map.of("type", "image", "attrs", Map.of("src", src))));
  }

  @Test
  void acceptsStructuredCodeAndRejectsUnknownExecutableNodes() {
    StructuredContent.validate(
        Map.of(
            "type",
            "doc",
            "content",
            List.of(
                Map.of(
                    "type",
                    "codeBlock",
                    "attrs",
                    Map.of("language", "java"),
                    "content",
                    List.of(Map.of("type", "text", "text", "int answer = 42;"))))));
    assertThatThrownBy(
            () ->
                StructuredContent.validate(
                    Map.of(
                        "type",
                        "doc",
                        "content",
                        List.of(Map.of("type", "script", "text", "alert(1)")))))
        .hasMessageContaining("Unsupported");
  }
}
