package com.isldevs.portfolio.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Uses the same PostgreSQL datasource as the integration profile; no Docker required. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MediaContactIntegrationTest {
  @DynamicPropertySource
  static void database(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () ->
            System.getenv()
                .getOrDefault("TEST_DB_URL", "jdbc:postgresql://127.0.0.1:5432/portfolio_test"));
    registry.add("portfolio.admin.username", () -> "");
    registry.add("portfolio.admin.password", () -> "");
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;

  @Test
  void contactRequiresCsrfAndDoesNotExposeTheInbox() throws Exception {
    mvc.perform(
            post("/api/v1/public/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .content(submission()))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/admin/messages")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/public/messages")).andExpect(status().is4xxClientError());
  }

  @Test
  @WithMockUser(username = "media-test-admin", roles = "ADMIN")
  void persistsContactAndEnforcesOptimisticMessageUpdates() throws Exception {
    String response =
        mvc.perform(
                post("/api/v1/public/contact")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(submission()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = json.readTree(response).get("id").asText();
    mvc.perform(get("/api/v1/admin/messages").param("q", "Integration test subject"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
    mvc.perform(
            put("/api/v1/admin/messages/" + id)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"READ\",\"version\":0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("READ"))
        .andExpect(jsonPath("$.version").value(1));
    mvc.perform(
            put("/api/v1/admin/messages/" + id)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"RESOLVED\",\"version\":0}"))
        .andExpect(status().isConflict());
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM audit_logs WHERE resource_id=?", Long.class, id))
        .isEqualTo(1);
  }

  @Test
  @WithMockUser(username = "media-test-admin", roles = "ADMIN")
  void validatesMediaKeepsPrivateFilesPrivateAndProtectsReferences() throws Exception {
    ByteArrayOutputStream image = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", image);
    MockMultipartFile upload =
        new MockMultipartFile("file", "portrait.png", "image/png", image.toByteArray());
    String response =
        mvc.perform(
                multipart("/api/v1/admin/media")
                    .file(upload)
                    .param("visibility", "PRIVATE")
                    .with(csrf()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode media = json.readTree(response);
    String id = media.get("id").asText();
    mvc.perform(get("/api/v1/public/media/" + id)).andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/admin/media/" + id + "/content"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/png"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    jdbc.update(
        "UPDATE profiles SET data=jsonb_set(data,'{portraitUrl}',to_jsonb(?::text))",
        media.get("url").asText());
    mvc.perform(delete("/api/v1/admin/media/" + id).with(csrf())).andExpect(status().isConflict());
    jdbc.update("UPDATE profiles SET data=data-'portraitUrl'");
    UUID contentId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO content_items(id,kind,title,slug) VALUES (?,?,?,?)",
        contentId,
        "articles",
        "Media revision test",
        "media-test-" + contentId);
    jdbc.update(
        "INSERT INTO blog_revisions(id,content_id,snapshot,actor) VALUES"
            + " (?,?,jsonb_build_object('media',?::text),?)",
        UUID.randomUUID(),
        contentId,
        media.get("url").asText(),
        "media-test-admin");
    // A revision must remain restorable after the current article removes its image.
    mvc.perform(delete("/api/v1/admin/media/" + id).with(csrf())).andExpect(status().isConflict());
    MockMultipartFile fake =
        new MockMultipartFile(
            "file", "portrait.png", "image/png", "<html>invalid</html>".getBytes());
    mvc.perform(multipart("/api/v1/admin/media").file(fake).with(csrf()))
        .andExpect(status().isBadRequest());
  }

  private static String submission() {
    return "{\"name\":\"Integration"
        + " Visitor\",\"email\":\"visitor@example.com\",\"subject\":\"Integration test"
        + " subject\",\"message\":\"This is a valid integration test"
        + " message.\",\"website\":\"\"}";
  }
}
