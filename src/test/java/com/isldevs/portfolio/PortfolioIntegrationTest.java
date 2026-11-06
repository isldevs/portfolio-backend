package com.isldevs.portfolio;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.isldevs.portfolio.auth.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PortfolioIntegrationTest {
  @DynamicPropertySource
  static void database(DynamicPropertyRegistry r) {
    r.add(
        "spring.datasource.url",
        () ->
            System.getenv()
                .getOrDefault("TEST_DB_URL", "jdbc:postgresql://127.0.0.1:5432/portfolio_test"));
    r.add("portfolio.admin.username", () -> "");
    r.add("portfolio.admin.password", () -> "");
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired
  UserRepository users;
  @Autowired PasswordEncoder encoder;

  private Map<String, Object> item(String title, String slug, String status) {
    return new LinkedHashMap<>(
        Map.of(
            "title",
            title,
            "slug",
            slug,
            "summary",
            "Test summary",
            "status",
            status,
            "body",
            Map.of(
                "type",
                "doc",
                "content",
                List.of(
                    Map.of(
                        "type",
                        "paragraph",
                        "content",
                        List.of(Map.of("type", "text", "text", "A useful test article."))))),
            "data",
            Map.of("category", "Java", "tags", List.of("test"), "technologies", List.of("Java"))));
  }

  private JsonNode parse(MvcResult r) throws Exception {
    return json.readTree(r.getResponse().getContentAsString());
  }

  @Test
  void authorizationAndCsrfAreEnforced() throws Exception {
    mvc.perform(get("/api/v1/admin/projects")).andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/v1/admin/projects")
                .with(user("test").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(item("Draft", "csrf-draft", "DRAFT"))))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/admin/projects").with(user("viewer").roles("VIEWER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void projectLifecycleHidesDraftsAndRejectsStaleUpdates() throws Exception {
    var input = item("Unique project", "test-project-" + UUID.randomUUID(), "DRAFT");
    JsonNode created =
        parse(
            mvc.perform(
                    post("/api/v1/admin/projects")
                        .with(user("test").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andReturn());
    mvc.perform(get("/api/v1/public/projects").param("featured", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].featured").value(true));
    String id = created.path("id").asText();
    String slug = created.path("slug").asText();
    long version = created.path("version").asLong();
    mvc.perform(get("/api/v1/public/projects/" + slug)).andExpect(status().isNotFound());
    JsonNode published =
        parse(
            mvc.perform(
                    post("/api/v1/admin/projects/" + id + "/publish")
                        .with(user("test").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":" + version + "}"))
                .andExpect(status().isOk())
                .andReturn());
    mvc.perform(get("/api/v1/public/projects/" + slug))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PUBLISHED"));
    input.put("version", version);
    mvc.perform(
            put("/api/v1/admin/projects/" + id)
                .with(user("test").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(input)))
        .andExpect(status().isConflict());
    mvc.perform(
            get("/api/v1/public/projects").param("q", "Unique project").param("technology", "Java"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
    mvc.perform(
            delete("/api/v1/admin/projects/" + id)
                .param("version", published.path("version").asText())
                .with(user("test").roles("ADMIN"))
                .with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  void duplicatesMaximumLengthContentAsAnUnpublishedDraft() throws Exception {
    String prefix = "duplicate-boundary-" + UUID.randomUUID();
    String slug = prefix + "a".repeat(165 - prefix.length()) + "-" + "b".repeat(14);
    var input = item("T".repeat(240), slug, "PUBLISHED");
    JsonNode original =
        parse(
            mvc.perform(
                    post("/api/v1/admin/projects")
                        .with(user("test").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andReturn());
    JsonNode copy =
        parse(
            mvc.perform(
                    post("/api/v1/admin/projects/" + original.path("id").asText() + "/duplicate")
                        .with(user("test").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn());
    assertThat(copy.path("id").asText()).isNotEqualTo(original.path("id").asText());
    assertThat(copy.path("title").asText()).hasSize(240).endsWith(" (copy)");
    assertThat(copy.path("slug").asText()).hasSizeLessThanOrEqualTo(180);
    assertThat(copy.path("slug").asText()).matches("[a-z0-9]+(?:-[a-z0-9]+)*");
    assertThat(copy.path("slug").asText()).doesNotContain("--").isNotEqualTo(slug);
    assertThat(copy.path("status").asText()).isEqualTo("DRAFT");
    assertThat(copy.path("publishedAt").isNull()).isTrue();
    assertThat(copy.path("summary").asText()).isEqualTo(original.path("summary").asText());
    mvc.perform(get("/api/v1/public/projects/" + copy.path("slug").asText()))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/public/projects/" + slug)).andExpect(status().isOk());
  }

  @Test
  void duplicateSlugsAreRejected() throws Exception {
    var input = item("Unique slug", "duplicate-" + UUID.randomUUID(), "DRAFT");
    mvc.perform(
            post("/api/v1/admin/projects")
                .with(user("test").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(input)))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/admin/projects")
                .with(user("test").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(input)))
        .andExpect(status().isConflict());
  }

  @Test
  void articleRevisionRestoreCreatesDraftAndUnsafeDocumentsAreRejected() throws Exception {
    var input = item("Revision article", "revision-" + UUID.randomUUID(), "DRAFT");
    JsonNode first =
        parse(
            mvc.perform(
                    post("/api/v1/admin/articles")
                        .with(user("test").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andReturn());
    String id = first.path("id").asText();
    input.put("version", first.path("version").asLong());
    input.put("title", "Edited title");
    input.put("status", "PUBLISHED");
    JsonNode second =
        parse(
            mvc.perform(
                    put("/api/v1/admin/articles/" + id)
                        .with(user("test").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andReturn());
    JsonNode revisions =
        parse(
            mvc.perform(
                    get("/api/v1/admin/articles/" + id + "/revisions")
                        .with(user("test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn());
    assertThat(revisions.size()).isEqualTo(2);
    String oldest = revisions.get(1).path("id").asText();
    mvc.perform(
            post("/api/v1/admin/articles/" + id + "/revisions/" + oldest + "/restore")
                .with(user("test").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":" + second.path("version").asLong() + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Revision article"))
        .andExpect(jsonPath("$.status").value("DRAFT"));
    mvc.perform(get("/api/v1/public/articles/" + input.get("slug")))
        .andExpect(status().isNotFound());
    input.put("slug", "unsafe-" + UUID.randomUUID());
    input.put(
        "body",
        Map.of(
            "type",
            "doc",
            "content",
            List.of(Map.of("type", "image", "attrs", Map.of("src", "javascript:alert(1)")))));
    mvc.perform(
            post("/api/v1/admin/articles")
                .with(user("test").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(input)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void sessionLoginLogoutAndPasswordChangeWork() throws Exception {
    String password = "Test-only-long-password-" + UUID.randomUUID();
    String username = "test-" + UUID.randomUUID();
    User u = new User();
    u.username = username;
    u.passwordHash = encoder.encode(password);
    users.saveAndFlush(u);
    mvc.perform(
            post("/api/v1/admin/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(Map.of("username", username, "password", "wrong"))))
        .andExpect(status().isUnauthorized());
    MvcResult logged =
        mvc.perform(
                post("/api/v1/admin/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(
                            Map.of("username", username, "password", password))))
            .andExpect(status().isOk())
            .andReturn();
    MockHttpSession session = (MockHttpSession) logged.getRequest().getSession(false);
    assertThat(session).isNotNull();
    mvc.perform(get("/api/v1/admin/auth/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value(username));
    mvc.perform(post("/api/v1/admin/auth/logout").session(session).with(csrf()))
        .andExpect(status().isNoContent());
    assertThat(session.isInvalid()).isTrue();
    session =
        (MockHttpSession)
            mvc.perform(
                    post("/api/v1/admin/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            json.writeValueAsString(
                                Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
    MockHttpSession secondSession =
        (MockHttpSession)
            mvc.perform(
                    post("/api/v1/admin/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            json.writeValueAsString(
                                Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
    mvc.perform(
            post("/api/v1/admin/auth/password")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "currentPassword",
                            password,
                            "newPassword",
                            "New-long-test-password-" + UUID.randomUUID()))))
        .andExpect(status().isNoContent());
    assertThat(session.isInvalid()).isTrue();
    mvc.perform(get("/api/v1/admin/auth/me").session(secondSession))
        .andExpect(status().isUnauthorized());
    var logoutSession =
        (MockHttpSession)
            mvc.perform(
                    post("/api/v1/admin/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            json.writeValueAsString(
                                Map.of("username", username, "password", password))))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getRequest()
                .getSession(false);
  }
}
