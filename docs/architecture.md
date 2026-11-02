# Architecture and REST contract

Two independent applications communicate through `/api/v1`. Next.js renders public pages on the server and provides the admin UI. Spring Boot owns all authorization, transactions, validation, persistence, and publication decisions. PostgreSQL and the media directory are persistent; Node/Java processes and login sessions are not.

```text
HTTPS :443 → Nginx ─┬─ /api/v1/* → Spring Boot 127.0.0.1:8080 → PostgreSQL 127.0.0.1:5432
                   └─ /*         → Next.js    127.0.0.1:3000
```

Browser API URLs are relative. Server components use `API_BASE_URL`, never a browser-visible database address or secret. Public reads are dynamic and uncached across publication changes. Draft and archived content are excluded in the backend; frontend hiding alone is not authorization.

## Core DTOs

List responses use:

```typescript
interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // zero-based
  size: number;
}
interface ContentItem {
  id: string;
  kind: string;
  title: string;
  slug: string;
  summary: string;
  body: { type: 'doc'; content?: unknown[] }; // validated Tiptap document
  data: Record<string, unknown>;
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  featured: boolean;
  displayOrder: number;
  version: number;
  createdAt: string;
  updatedAt: string;
  publishedAt: string | null;
}
```

`data` holds resource-specific editorial fields such as `category`, `tags`, `technologies`, `problem`, `solution`, `architecture`, `githubUrl`, `demoUrl`, and SEO metadata. The normalized envelope provides a consistent CRUD and publishing model across content types. The backend uses a shared `content_items` table rather than a separate table for every editorial resource; profile, users, revisions, media, messages, and audit events have dedicated tables. This is an intentional modular-monolith simplification of the proposed database layout.

Updates send the last observed `version`. A conflict response means another editor changed the record; reload and reconcile instead of silently overwriting. Slugs are unique within a resource kind. Content is structured data, never executable MDX or JavaScript. Public renderers permit known nodes and safe links; code blocks are rendered as code.

## Routes

- `GET /api/v1/csrf` returns `{token, headerName}` and establishes CSRF/session state. Include this token under `headerName` on every state-changing request, including login and contact. Refresh it after login/logout because session state can change.
- `POST /api/v1/admin/auth/login` accepts `{username,password}`; `GET .../me` returns current identity; `POST .../logout` closes the session. `POST .../password` accepts `{currentPassword,newPassword}`.
- `GET /api/v1/public/profile` returns profile fields directly. `GET/PUT /api/v1/admin/profile` reads/updates them with version checking.
- Public collections: `/api/v1/public/{projects,articles,case-studies,experiences,skills,categories,tags}`. Public item reads use `/{slug}` where supported and return only published content.
- Admin collections: `/api/v1/admin/{projects,articles,case-studies,experiences,skills,categories,tags,settings}`. `GET` lists, `POST` creates, and `GET/PUT/DELETE /{id}` manage a record. Query filters include `q`, `category`, `tag`, `technology`, `page`, and `size` as supported by the collection.
- Article actions: `POST /api/v1/admin/articles/{id}/publish` and `/unpublish` accept `{version}`. Revision listing and restore use `/articles/{id}/revisions` and `/articles/{id}/revisions/{revisionId}/restore`.
- `GET /api/v1/admin/dashboard` returns database counts and recent audit activity.
- `POST /api/v1/public/contact` accepts `{name,email,subject,message,website}`. `website` is an empty honeypot. Contact messages are never publicly readable.
- `/api/v1/admin/messages` supports listing/search and status changes through `PUT /{id}` with `{status,version}`; statuses are `UNREAD`, `READ`, `RESOLVED`, `ARCHIVED`.
- `/api/v1/admin/media` supports listing and authorized multipart uploads using field `file`. Returned media includes `id`, `url`, `originalName`, `contentType`, `size`, and `createdAt`. Public media URLs use `/api/v1/public/media/{id}`. Uploads accept optional `visibility` (`PUBLIC` or `PRIVATE`, default `PUBLIC`). Private content is downloaded through authenticated `/api/v1/admin/media/{id}/content`; public endpoints reject private assets. Public URLs are intended for public portfolio assets, not private document storage.

The generated OpenAPI schema from the running backend is authoritative for exact validation and operation details. Keep the schema/client synchronized when changing endpoints in either repository. Errors use RFC 9457 Problem Details rather than leaking stack traces or database diagnostics.
