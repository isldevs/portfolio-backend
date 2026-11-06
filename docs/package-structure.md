# Backend packages and DTO response contracts

The backend is organized by feature first, then by responsibility. Controllers, services, and DTOs are no longer mixed into entity packages.

```text
com.isldevs.portfolio
├── auth
│   ├── controller      Login, logout, identity, CSRF, password endpoints
│   ├── dto             Login/password inputs; user and CSRF responses
│   ├── entity          User JPA entity
│   ├── repository      User database access
│   └── service         Login-attempt state
├── profile
│   ├── controller
│   ├── dto             ProfileResponse
│   ├── entity
│   ├── repository
│   ├── mapper          MapStruct profile response mapping
│   └── service
├── content
│   ├── controller      Content, revisions, dashboard/audit endpoints
│   ├── dto             ContentRequest, ContentResponse, RevisionResponse,
│   │                   DashboardResponse, VersionRequest
│   ├── entity          ContentItem, BlogRevision
│   ├── repository
│   ├── mapper
│   ├── service         Content and dashboard queries
│   └── validation      StructuredContent validation
├── contact
│   ├── controller
│   ├── dto             ContactRequest, MessageUpdateRequest,
│   │                   SubmissionResponse, MessageResponse
│   ├── entity          Contact, status enum
│   ├── repository
│   ├── mapper
│   └── service         Contact operations and rate limiting
├── media
│   ├── controller
│   ├── dto             MediaResponse
│   ├── entity          Media, visibility enum
│   ├── repository
│   ├── mapper
│   └── service         Storage, validation, upload/download coordination
├── audit
│   ├── dto             AuditResponse
│   ├── entity
│   ├── repository
│   ├── mapper
│   └── service
├── common
│   ├── dto             PageResponse<T>
│   └── exception       Application errors and Problem Details handlers
├── config              Bootstrap data/account setup
└── security            Spring Security configuration
```

## Responsibility rules

- **Controller:** HTTP paths, validation, status/headers, DTO input/output. The dashboard controller delegates database queries to its service.
- **Service:** business rules and transactions; it coordinates repositories, mapping, auditing, and storage.
- **Repository:** Spring Data JPA queries. PostgreSQL JSON reference checks remain native repository queries.
- **Entity:** database storage model. Do not return JPA entities from HTTP endpoints.
- **DTO:** explicit request or response Java records (immutable data classes). Response names end in `Response`.
- **Mapper:** MapStruct-generated Spring beans convert persistence models into response DTOs. Mapping uses `ReportingPolicy.ERROR` to expose unmapped fields at compile time.
- **Validation/exception:** content validation and consistent HTTP errors, outside persistence classes.

Binary downloads still return `ResponseEntity<Resource>`, and successful delete/logout operations still return no body. Errors use Spring's typed RFC 9457 `ProblemDetail`, with the contact rate-limit retry header preserved.

## Response mapping

| Endpoint family                                        | Response DTO                                               |
| ------------------------------------------------------ | ---------------------------------------------------------- |
| CSRF                                                   | `CsrfResponse`                                             |
| Login/current administrator                            | `UserResponse`                                             |
| Public/admin profile and profile update                | `ProfileResponse`                                          |
| Public/admin content lists                             | `PageResponse<ContentResponse>`                            |
| Content create/update/detail/publish/duplicate/restore | `ContentResponse`                                          |
| Article revision list                                  | `List<RevisionResponse>` with a `ContentResponse` snapshot |
| Dashboard                                              | `DashboardResponse`                                        |
| Audit list                                             | `List<AuditResponse>`                                      |
| Contact submission                                     | `SubmissionResponse`                                       |
| Inbox list/detail/update                               | `PageResponse<MessageResponse>` / `MessageResponse`        |
| Media list/upload                                      | `PageResponse<MediaResponse>` / `MediaResponse`            |

The page envelope retains the existing JSON shape:

```json
{
  "content": [],
  "total_elements": 0,
  "total_pages": 0,
  "number": 0,
  "size": 20
}
```

Endpoint paths, version checks, cookie/CSRF behavior, and binary download headers are preserved. Request and response JSON property names now use snake_case; older camelCase clients must be updated. Profile responses expose the supported fields explicitly rather than returning every arbitrary key stored in profile JSON.

Rich-text documents and resource-specific metadata remain flexible JSON objects. `ContentResponse.body` and `.data` use Jackson `JsonNode` so a typed response can still represent nested editor documents and varying CMS metadata without changing their JSON layout. Request validation and JSONB persistence may still use internal maps; these are not map-based response envelopes. `ContentRequest` remains separate from the output DTO. The profile update request keeps its existing partial-update map input semantics.

`ResponseContractTest` scans controller signatures and nested DTO records to reject map/wildcard/JPA-entity responses. Integration tests verify pagination, auth, revisions, messages, media, profile filtering, and the existing website-compatible fields.

## Making future changes

1. Add or update the relevant request/response in that feature's `dto` package.
2. Keep data access in repositories and business rules in services.
3. Update the mapper and run compilation to check generated mappings.
4. Add meaningful regression coverage for changed behavior.
5. Run `./gradlew --no-daemon test bootJar spotlessCheck` with a disposable configured PostgreSQL test database.
6. Export OpenAPI from the running backend, update the [API collection](api-testing.md), and verify affected requests.

This refactor does not add or edit database migrations. Earlier upstream commits changed existing migrations/table names; old installations need a separate reviewed database upgrade plan. Never use Flyway repair, database deletion, or disabling validation as an automatic workaround.

## JSON naming convention

All API DTO properties use `snake_case`, for example `display_order`, `created_at`, `total_elements`, `full_name`, `header_name`, `current_password`, and `new_password`. Java record components and database/storage keys keep their internal naming. CMS metadata inside `data` is converted recursively at the API boundary; send keys such as `cover_image_url` and `github_url`. Profile partial updates also accept the snake_case field names.

The rich-text `body` follows Tiptap's editor document protocol. Node type values and editor attribute keys such as `codeBlock` or `textAlign` are preserved. Enum values, URLs, HTTP header names, and human text are not renamed. Sort values support snake_case field names, for example `sort=created_at,desc`.

DTO records declare `@JsonNaming(SnakeCaseStrategy.class)` so generated OpenAPI also documents the wire names; Spring Jackson is configured with the same default. New DTOs must retain that convention. `ApiJsonNames` handles dynamic metadata and normalizes older article revision snapshots when they are read/restored. No database key migration is needed for this naming change. Deploy the updated frontend and backend together: the older website expects camelCase DTO fields and is incompatible with the new wire contract.
