package com.isldevs.portfolio.config;

import com.isldevs.portfolio.auth.entity.User;
import com.isldevs.portfolio.auth.repository.UserRepository;
import com.isldevs.portfolio.content.entity.ContentItem;
import com.isldevs.portfolio.content.repository.ContentRepository;
import com.isldevs.portfolio.profile.entity.Profile;
import com.isldevs.portfolio.profile.repository.ProfileRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class Bootstrap implements CommandLineRunner {

  private final UserRepository users;
  private final ProfileRepository profiles;
  private final ContentRepository content;
  private final PasswordEncoder encoder;

  @Value("${portfolio.admin.username}")
  private String username;

  @Value("${portfolio.admin.password}")
  private String password;

  @Value("${portfolio.seed-demo}")
  private boolean seedDemo;

  public Bootstrap(
      UserRepository users,
      ProfileRepository profiles,
      ContentRepository content,
      PasswordEncoder encoder) {
    this.users = users;
    this.profiles = profiles;
    this.content = content;
    this.encoder = encoder;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (users.count() == 0 && (!username.isBlank() || !password.isBlank())) {
      if (username.isBlank()
          || password.length() < 14
          || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
        throw new IllegalStateException(
            "Bootstrap requires ADMIN_USERNAME and an ADMIN_PASSWORD of at least 14 characters and"
                + " no more than 72 UTF-8 bytes.");
      User admin = new User();
      admin.username = username;
      admin.passwordHash = encoder.encode(password);
      users.save(admin);
    }
    if (profiles.count() == 0) {
      Profile p = new Profile();
      p.data =
          new LinkedHashMap<>(
              Map.ofEntries(
                  Map.entry("fullName", "Yi Sivlay"),
                  Map.entry("professionalTitle", "Senior Backend Developer"),
                  Map.entry("headline", "Building Reliable Systems Behind Every Transaction."),
                  Map.entry(
                      "shortBio",
                      "I design and build secure, scalable, and maintainable backend systems using"
                          + " Java, Spring Boot, and modern technologies, with a focus on"
                          + " enterprise applications, distributed systems, and financial"
                          + " technology."),
                  Map.entry(
                      "fullBio",
                      "I focus on Java, Spring Boot, microservices, and enterprise systems in"
                          + " banking and financial technology. Employment history, education, and"
                          + " certifications will be added after verification."),
                  Map.entry("location", "Phnom Penh, Cambodia"),
                  Map.entry(
                      "specialization", "Java · Spring Boot · Microservices · Enterprise Systems"),
                  Map.entry("availability", "Contact to discuss availability"),
                  Map.entry("githubUrl", "https://github.com/isldevs"),
                  Map.entry("linkedinUrl", ""),
                  Map.entry("publicEmail", ""),
                  Map.entry("portraitUrl", "/images/yi-sivlay.png"),
                  Map.entry("resumeUrl", ""),
                  Map.entry("seoTitle", "Yi Sivlay — Senior Backend Developer"),
                  Map.entry(
                      "seoDescription",
                      "Java, Spring Boot, and enterprise backend engineering by Yi Sivlay, Phnom"
                          + " Penh, Cambodia.")));
      profiles.save(p);
    }
    if (!seedDemo || content.count() > 0) return;
    project(
        "banking-microservices-platform",
        "Banking Microservices Platform",
        "A demonstration of ledger boundaries, reliable event delivery, and secure service"
            + " communication.",
        List.of("Java", "Spring Boot", "PostgreSQL"),
        "Fintech",
        1,
        "Model a transfer as an explicit state machine with traceable accounting entries.",
        "Use a relational ledger, idempotent commands, and a transactional outbox to avoid losing"
            + " events between a database commit and publication.");
    project(
        "enterprise-api-gateway",
        "Enterprise API Gateway",
        "A sample edge service exploring routing, request budgets, and consistent API contracts.",
        List.of("Java", "Spring Cloud", "OAuth2"),
        "Infrastructure",
        2,
        "Provide one governed entry point for independently evolving APIs.",
        "Validate identity at the edge, propagate correlation IDs, and enforce bounded timeouts"
            + " while keeping domain authorization inside each service.");
    project(
        "authentication-authorization-service",
        "Authentication & Authorization Service",
        "A sample identity system demonstrating secure sessions and fine-grained access decisions.",
        List.of("Java", "Spring Security", "PostgreSQL"),
        "Security",
        3,
        "Protect administrative workflows without exposing credentials to browser storage.",
        "Use adaptive password hashing, HTTP-only sessions, CSRF tokens, and authorization checks"
            + " on every protected operation.");
    article(
        "understanding-spring-boot-transaction-management",
        "Understanding Spring Boot Transaction Management",
        "Spring Boot",
        "Transactions are a consistency boundary. Understand proxies, propagation, and rollback"
            + " before choosing an annotation.",
        List.of("Java", "Spring Boot"),
        List.of(
            heading("What a transaction guarantees"),
            paragraph(
                "A database transaction groups work into an atomic unit. Isolation controls which"
                    + " concurrent changes can be observed; it does not automatically make every"
                    + " application invariant safe. Choose constraints and locking for the"
                    + " invariant you need."),
            heading("Spring's proxy boundary"),
            paragraph(
                "Spring normally applies @Transactional through a proxy. A call from one method to"
                    + " another on the same instance bypasses that proxy. Put transaction"
                    + " boundaries on public service methods invoked through a managed bean."),
            code(
                "java",
                "@Transactional\n"
                    + "public Transfer transfer(TransferCommand command) {\n"
                    + "    ledger.record(command);\n"
                    + "    outbox.append(command.id());\n"
                    + "    return Transfer.accepted(command.id());\n"
                    + "}"),
            heading("Rollback and propagation"),
            paragraph(
                "The default rollback rules cover unchecked exceptions and Error, not every checked"
                    + " exception. Set rollbackFor intentionally when required. REQUIRED joins an"
                    + " existing transaction; REQUIRES_NEW creates an independent one and can"
                    + " increase connection usage. Test commit and rollback against PostgreSQL."),
            heading("Practical guardrails"),
            paragraph(
                "Keep network calls outside long database transactions where possible. Read-only is"
                    + " a useful optimization hint, not an authorization boundary. An outbox needs"
                    + " a reliable publisher and consumer idempotency; writing an outbox row alone"
                    + " does not provide exactly-once delivery.")));
    article(
        "jwt-authentication-vs-oauth2-in-spring-security",
        "JWT Authentication vs OAuth2 in Spring Security",
        "Security",
        "JWT is a token format; OAuth2 is an authorization framework. They solve different parts of"
            + " the security model.",
        List.of("Security", "Spring Security"),
        List.of(
            heading("Compare the correct concepts"),
            paragraph(
                "JWT defines a compact representation of claims. OAuth 2.0 defines how clients"
                    + " obtain and use access tokens. An OAuth2 access token may be a JWT or an"
                    + " opaque value. OpenID Connect adds an identity layer; do not treat OAuth2"
                    + " alone as a login protocol."),
            heading("Validation matters"),
            paragraph(
                "A resource server must verify the signature with trusted keys, constrain accepted"
                    + " algorithms, and validate issuer, audience, and time claims. Decoding a"
                    + " token is not verification. Never accept an ID token as an access token"
                    + " without the appropriate protocol guarantees."),
            code(
                "java",
                "http.oauth2ResourceServer(resource -> resource.jwt(Customizer.withDefaults()));"),
            heading("Browser sessions are often simpler"),
            paragraph(
                "For a same-origin portfolio CMS, an HTTP-only session cookie avoids exposing"
                    + " authentication tokens to JavaScript. Use CSRF protection, secure cookies"
                    + " over HTTPS, session rotation on login, and server-side authorization. The"
                    + " appropriate choice depends on clients and trust boundaries.")));
    article(
        "designing-reliable-rest-apis-with-idempotency",
        "Designing Reliable REST APIs with Idempotency",
        "System Design",
        "Retries are inevitable. Make their meaning explicit with scoped idempotency keys and"
            + " database constraints.",
        List.of("REST", "Architecture"),
        List.of(
            heading("Retries create ambiguity"),
            paragraph(
                "A client timeout does not prove that a server failed to commit. Retrying a payment"
                    + " request without an idempotency mechanism can create a second operation."),
            heading("Store the decision atomically"),
            paragraph(
                "Scope a key by authenticated principal and operation. Persist the key, a request"
                    + " fingerprint, and the resulting response with the business transaction."
                    + " Reject reuse with different parameters. Concurrent identical requests need"
                    + " a unique constraint or lock so only one performs the mutation."),
            code(
                "sql",
                "CREATE UNIQUE INDEX request_key_scope\n"
                    + "ON idempotency_keys (principal_id, operation, request_key);"),
            heading("Define expiration and failure semantics"),
            paragraph(
                "Document retention windows and whether a failed request can be retried with the"
                    + " same key. Responses must not leak another principal's result. Idempotency"
                    + " reduces duplicate effects within the designed boundary; it does not remove"
                    + " every failure mode in a distributed workflow.")));
    article(
        "optimizing-sql-queries-in-enterprise-applications",
        "Optimizing SQL Queries in Enterprise Applications",
        "Database",
        "Start with a query plan and representative data, then measure each change.",
        List.of("PostgreSQL", "SQL"),
        List.of(
            heading("Measure the workload"),
            paragraph(
                "Identify slow statements and their frequency before adding indexes. Query time in"
                    + " an empty development database is rarely representative. Use realistic"
                    + " distributions and consider application-level N+1 queries."),
            code(
                "sql",
                "EXPLAIN (ANALYZE, BUFFERS)\n"
                    + "SELECT id, title\n"
                    + "FROM content_items\n"
                    + "WHERE kind = 'articles' AND status = 'PUBLISHED'\n"
                    + "ORDER BY display_order, published_at DESC\n"
                    + "LIMIT 20;"),
            heading("Interpret the plan carefully"),
            paragraph(
                "EXPLAIN ANALYZE executes the statement. Use a safe environment and special care"
                    + " with mutations. Compare estimated and actual row counts, loops, buffer"
                    + " reads, and sort behavior. A sequential scan can be the best plan for a"
                    + " small table."),
            heading("Index the access path"),
            paragraph(
                "Composite index order should reflect filtering and ordering requirements. Every"
                    + " index adds write cost and storage. Avoid unbounded pagination on large"
                    + " offsets; a stable cursor can be a better fit. Verify improvements with"
                    + " representative measurements rather than invented performance claims.")));
    article(
        "microservices-practical-design-trade-offs",
        "Microservices Architecture: Practical Design Trade-offs",
        "Architecture",
        "Service boundaries bring independent deployment and operational complexity. Start with"
            + " domain needs.",
        List.of("Microservices", "Architecture"),
        List.of(
            heading("A deployment choice with consequences"),
            paragraph(
                "Microservices can isolate ownership and release cadence, but introduce network"
                    + " failures, distributed observability, and cross-service consistency"
                    + " problems. A modular monolith is often a strong starting point when the"
                    + " domain and team are still evolving."),
            heading("Model boundaries first"),
            paragraph(
                "Use cohesive business capabilities and explicit contracts. A shared database"
                    + " schema across services creates coupling even if processes deploy"
                    + " separately. Avoid turning each database table into a remote service."),
            Map.of(
                "type",
                "mermaid",
                "attrs",
                Map.of(
                    "code",
                    "flowchart LR\n"
                        + "  Client --> API\n"
                        + "  API --> Ledger\n"
                        + "  Ledger --> Outbox\n"
                        + "  Outbox --> Publisher\n"
                        + "  Publisher --> Notifications")),
            heading("Make reliability explicit"),
            paragraph(
                "Set deadlines, bound retries, add backoff with jitter, and design idempotent"
                    + " operations. Circuit breakers and queues each have costs and failure modes."
                    + " Document the consistency model and compensating actions rather than"
                    + " promising universal exactly-once processing.")));
    content.save(
        item(
            "case-studies",
            "designing-an-idempotent-transfer-api",
            "Designing an Idempotent Transfer API",
            "A demonstration case study in safely retrying financial commands.",
            map(
                "category",
                "System Design",
                "technologies",
                List.of("Java", "PostgreSQL"),
                "problem",
                "A caller can time out after a transfer commits, then retry.",
                "architecture",
                "An API layer delegates to a transactional service with a unique idempotency key"
                    + " and append-only ledger.",
                "solution",
                "Persist a scoped request fingerprint and response in the same transaction as the"
                    + " command.",
                "engineeringDecisions",
                "Use relational constraints as the final arbiter of concurrent uniqueness.",
                "security",
                "Scope keys to an authenticated actor; verify authorization before retrieving a"
                    + " prior result.",
                "challenges",
                "Handle in-flight duplicates and define an explicit retention window.",
                "tradeOffs",
                "Additional storage and transaction complexity buy clearer retry behavior.",
                "outcomes",
                "Demonstration design only; no production metrics or employment claims.",
                "lessonsLearned",
                "A retry contract belongs in the API specification, not only client code."),
            List.of(
                heading("Demonstration case study"),
                paragraph(
                    "This is an illustrative engineering exercise, not a verified professional"
                        + " engagement."),
                heading("Problem"),
                paragraph(
                    "A network timeout leaves the caller unsure whether a transfer was accepted."),
                heading("Approach"),
                paragraph(
                    "Use a scoped idempotency key, request fingerprint, and a unique database"
                        + " constraint. Return a saved result only after checking the requesting"
                        + " principal.")),
            true,
            1));
    String[][] skills = {
      {"Backend Development", "Java, Spring Boot, REST API design"},
      {"System Architecture", "Modular monoliths, distributed boundaries, reliability"},
      {"Database Engineering", "PostgreSQL, transactions, schema design"},
      {"Application Security", "Spring Security, sessions, authorization"},
      {"Microservices", "Service boundaries, resilient communication"},
      {"Performance Optimization", "Query plans, profiling, workload measurement"}
    };
    int order = 0;
    for (String[] s : skills)
      content.save(
          item(
              "skills",
              slug(s[0]),
              s[0],
              s[1],
              Map.of("category", s[0], "description", s[1]),
              List.of(),
              true,
              ++order));
    for (String category :
        List.of(
            "Java",
            "Spring Boot",
            "Microservices",
            "System Design",
            "Database",
            "Security",
            "Performance",
            "Architecture"))
      content.save(
          item(
              "categories",
              slug(category),
              category,
              "Technical writing about " + category,
              Map.of(),
              List.of(),
              false,
              0));
    for (String tag :
        List.of(
            "Java",
            "Spring Boot",
            "PostgreSQL",
            "Security",
            "REST",
            "Architecture",
            "SQL",
            "Microservices",
            "Spring Security"))
      content.save(item("tags", slug(tag), tag, "", Map.of(), List.of(), false, 0));
    content.save(
        item(
            "settings",
            "site",
            "Site settings",
            "Public site configuration",
            Map.of(
                "services",
                List.of(
                    "Backend API Development",
                    "Microservices Architecture",
                    "Database Design & Optimization",
                    "API Security",
                    "Technical Consulting",
                    "Code Review"),
                "tools",
                List.of("IntelliJ IDEA", "Git", "Gradle", "Postman", "PostgreSQL", "Linux")),
            List.of(),
            false,
            0));
  }

  private void project(
      String slug,
      String title,
      String summary,
      List<String> technologies,
      String category,
      int order,
      String problem,
      String solution) {
    var data = new LinkedHashMap<String, Object>();
    data.put("category", category);
    data.put("technologies", technologies);
    data.put("demonstration", true);
    data.put("problemStatement", problem);
    data.put("solution", solution);
    data.put(
        "architecture",
        "Layered API, transactional application service, domain model, and PostgreSQL"
            + " persistence.");
    data.put(
        "features",
        List.of(
            "Versioned REST API", "Input validation", "Auditability", "Explicit failure handling"));
    data.put(
        "securityConsiderations",
        "Authenticate callers, authorize operations, validate inputs, and avoid exposing sensitive"
            + " data in logs.");
    data.put(
        "databaseDesign",
        "Relational constraints preserve invariants; indexes follow measured query patterns.");
    data.put(
        "engineeringDecisions",
        "Keep module boundaries explicit and choose infrastructure only when the use case requires"
            + " it.");
    data.put("challenges", "Define concurrency semantics and recovery paths before scaling.");
    data.put(
        "tradeOffs",
        "Stronger consistency limits write concurrency; additional asynchronous components increase"
            + " operational cost.");
    data.put(
        "lessonsLearned",
        "Treat reliability as behavior that can be exercised with failure tests.");
    data.put("githubUrl", "");
    data.put("demoUrl", "");
    content.save(
        item(
            "projects",
            slug,
            title,
            summary,
            data,
            List.of(
                heading("Demonstration project"),
                paragraph(
                    "This sample is an illustrative architecture, not a verified professional"
                        + " project."),
                heading("Problem"),
                paragraph(problem),
                heading("Solution"),
                paragraph(solution)),
            true,
            order));
  }

  private void article(
      String slug,
      String title,
      String category,
      String summary,
      List<String> tags,
      List<Map<String, Object>> nodes) {
    content.save(
        item(
            "articles",
            slug,
            title,
            summary,
            Map.of(
                "category",
                category,
                "tags",
                tags,
                "author",
                "Yi Sivlay",
                "demonstration",
                true,
                "reviewStatus",
                "Unreviewed demonstration content",
                "readingTime",
                Math.max(2, nodes.size() / 2)),
            nodes,
            false,
            0));
  }

  private static ContentItem item(
      String kind,
      String slug,
      String title,
      String summary,
      Map<String, Object> data,
      List<Map<String, Object>> nodes,
      boolean featured,
      int order) {
    ContentItem c = new ContentItem();
    c.kind = kind;
    c.slug = slug;
    c.title = title;
    c.summary = summary;
    c.status = "PUBLISHED";
    c.featured = featured;
    c.displayOrder = order;
    c.data = new LinkedHashMap<>(data);
    c.body = new LinkedHashMap<>(Map.of("type", "doc", "content", nodes));
    c.publishedAt = java.time.Instant.now();
    return c;
  }

  private static Map<String, Object> map(Object... pairs) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) m.put((String) pairs[i], pairs[i + 1]);
    return m;
  }

  private static String slug(String s) {
    return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
  }

  private static Map<String, Object> heading(String s) {
    return Map.of(
        "type",
        "heading",
        "attrs",
        Map.of("level", 2),
        "content",
        List.of(Map.of("type", "text", "text", s)));
  }

  private static Map<String, Object> paragraph(String s) {
    return Map.of("type", "paragraph", "content", List.of(Map.of("type", "text", "text", s)));
  }

  private static Map<String, Object> code(String language, String text) {
    return Map.of(
        "type",
        "codeBlock",
        "attrs",
        Map.of("language", language),
        "content",
        List.of(Map.of("type", "text", "text", text)));
  }
}
