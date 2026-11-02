# Validation record

Validated in the managed cloud workspace on 2026-11-02 with Java 21, Node.js 24, PostgreSQL 17 and Chromium 154. These are local lab results, not a live deployment certification.

- Frontend: frozen installation, ESLint, strict TypeScript, 7 unit tests and standalone production build.
- Backend: checksum-pinned Gradle wrapper, Flyway migrations, executable build and 20 PostgreSQL/security/content/media tests with no skips.
- Browser: all 6 final production end-to-end tests passed across desktop and mobile; public routes, filtering, responsive overflow checks, contact validation, admin login, project/article draft protection, publishing, updates, stale-version rejection, revisions, contact inbox and logout.
- Admin browser: all 13 screens, profile/settings persistence, private media uploads and anonymous access rejection, unused media deletion, Tiptap tables, Mermaid preview, mobile navigation and session guards.
- Lighthouse mobile against the standalone artifact: performance **95**, accessibility **100**, best practices **100**, SEO **100**; cumulative layout shift **0**. Scores vary by machine, content, network and run. No deployed Core Web Vitals claim is made.
- Release scripts pass shell syntax checks; CI/release YAML parses. Nginx, systemd, TLS renewal, and remote deployment have not been run on a real VPS.

## Deliberate choices and remaining external inputs

The requested reference image was not attached. Styling follows the brief's dark terminal palette, two-column layout, mint accents and compact cards. The supplied transparent portrait is stored in the media library and versioned at `portfolio-web/public/images/yi-sivlay.png`; the résumé remains absent until supplied; no employment history, metrics, private contact details, education, or certifications were invented. Projects and articles are labeled demonstrations.

Editorial resources share a typed envelope in `content_items`; resource-specific fields use validated JSON. Categories and tags use name/slug references, guarded against renaming/deleting referenced records, rather than separate join-table foreign keys. This differs from the suggested per-feature table layout; see `architecture.md`. Production storage is persistent local disk behind an abstraction, with backup and recovery instructions. An S3 adapter and scheduled publication are not included.

A real domain, HTTPS certificates, VPS access, production database credentials, and a fresh production administrator bootstrap are required to deploy. Cloud development credentials are not production credentials.
