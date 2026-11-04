package com.isldevs.portfolio.entity.content;

import com.isldevs.portfolio.common.ApiException;
import java.util.*;

/** Validate the editor document; public rendering never executes HTML, MDX, or scripts. */
public final class StructuredContent {
  private static final Set<String> TYPES =
      Set.of(
          "doc",
          "paragraph",
          "text",
          "heading",
          "bulletList",
          "orderedList",
          "listItem",
          "blockquote",
          "codeBlock",
          "hardBreak",
          "horizontalRule",
          "image",
          "table",
          "tableRow",
          "tableCell",
          "tableHeader",
          "callout",
          "mermaid");
  private static final Set<String> MARKS =
      Set.of("bold", "italic", "underline", "strike", "code", "link", "textStyle", "highlight");

  public static void validate(Map<String, Object> body) {
    if (body == null) return;
    if (!"doc".equals(body.get("type")))
      throw ApiException.invalid("Content must be a structured editor document.");
    node(body, 0, new int[] {0});
  }

  @SuppressWarnings("unchecked")
  private static void node(Map<?, ?> n, int depth, int[] count) {
    if (depth > 30 || ++count[0] > 20000)
      throw ApiException.invalid("The document is too complex.");
    if (!TYPES.contains(String.valueOf(n.get("type"))))
      throw ApiException.invalid("Unsupported editor node.");
    if (n.containsKey("content") && !(n.get("content") instanceof List<?>))
      throw ApiException.invalid("Node content must be an array.");
    if (n.containsKey("marks") && !(n.get("marks") instanceof List<?>))
      throw ApiException.invalid("Node marks must be an array.");
    if (n.containsKey("attrs") && !(n.get("attrs") instanceof Map<?, ?>))
      throw ApiException.invalid("Node attributes must be an object.");
    if (n.containsKey("text") && !(n.get("text") instanceof String))
      throw ApiException.invalid("Text must be a string.");
    if ("doc".equals(n.get("type")) && !(n.get("content") instanceof List<?>))
      throw ApiException.invalid("Document content must be an array.");
    if ("text".equals(n.get("type")) && !(n.get("text") instanceof String))
      throw ApiException.invalid("Text nodes require text.");
    if (n.get("attrs") instanceof Map<?, ?> a) {
      if (a.containsKey("level")
          && (!(a.get("level") instanceof Number level)
              || level.intValue() < 1
              || level.intValue() > 6)) throw ApiException.invalid("Invalid heading level.");
      for (String key : List.of("src", "href", "code", "language", "alt", "title"))
        if (a.containsKey(key) && !(a.get(key) instanceof String))
          throw ApiException.invalid("Invalid editor attribute.");
    }
    if (n.get("text") instanceof String s && s.length() > 100000)
      throw ApiException.invalid("Text block is too long.");
    if (n.get("attrs") instanceof Map<?, ?> attrs) {
      for (String key : List.of("src", "href"))
        if (attrs.get(key) instanceof String url && !safeUrl(url))
          throw ApiException.invalid("Unsafe content URL.");
    }
    if (n.get("marks") instanceof List<?> marks)
      for (Object mark : marks) {
        if (!(mark instanceof Map<?, ?> m) || !MARKS.contains(String.valueOf(m.get("type"))))
          throw ApiException.invalid("Unsupported editor mark.");
        if (m.get("attrs") instanceof Map<?, ?> a
            && a.get("href") instanceof String url
            && !safeUrl(url)) throw ApiException.invalid("Unsafe link.");
      }
    if (n.get("content") instanceof List<?> children)
      for (Object c : children) {
        if (!(c instanceof Map<?, ?> child)) throw ApiException.invalid("Invalid editor document.");
        node(child, depth + 1, count);
      }
  }

  public static boolean safeUrl(String url) {
    if (url == null || url.contains("\\") || url.chars().anyMatch(c -> c < 32 || c == 127))
      return false;
    try {
      var uri = new java.net.URI(url);
      if (url.startsWith("/")) {
        return uri.getAuthority() == null
            && (url.equals("/images/yi-sivlay.png")
                || url.matches("/api/v1/public/media/[A-Za-z0-9_-]+"));
      }
      if ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
        return uri.getHost() != null && uri.getUserInfo() == null;
      return "mailto".equalsIgnoreCase(uri.getScheme())
          && uri.getSchemeSpecificPart().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
    } catch (java.net.URISyntaxException e) {
      return false;
    }
  }

  private StructuredContent() {}
}
