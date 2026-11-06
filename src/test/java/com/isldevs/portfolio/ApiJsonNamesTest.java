package com.isldevs.portfolio;

import static org.assertj.core.api.Assertions.*;

import com.isldevs.portfolio.common.json.ApiJsonNames;
import java.util.*;
import org.junit.jupiter.api.Test;

class ApiJsonNamesTest {
  @Test
  void convertsMetadataRecursivelyWithoutChangingDocumentProtocolsOrValues() {
    var document =
        Map.of("type", "codeBlock", "attrs", Map.of("textAlign", "left"), "content", List.of());
    Map<String, Object> stored =
        Map.of(
            "displayOrder",
            3,
            "body",
            document,
            "data",
            Map.of(
                "coverImageUrl",
                "https://example.com/someCamelPath.png",
                "nestedItems",
                List.of(Map.of("apiDesign", "notes"))));
    var wire = ApiJsonNames.toWire(stored);
    assertThat(wire).containsEntry("display_order", 3).containsEntry("body", document);
    assertThat(((Map<?, ?>) wire.get("data")).containsKey("cover_image_url")).isTrue();
    assertThat(ApiJsonNames.toInternal(wire)).isEqualTo(stored);
  }

  @Test
  void rejectsAmbiguousMetadataKeysInsteadOfSilentlyOverwriting() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ApiJsonNames.toInternal(Map.of("cover_image", "a", "coverImage", "b")));
  }
}
