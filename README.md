# Portfolio backend

Java 21 / Spring Boot REST API for Yi Sivlay's portfolio and technical-blog CMS. PostgreSQL holds content, profile data, administrator credentials, revisions, contact messages, media metadata, and audit events. Flyway migrations define the schema; Hibernate validates it. The companion `isldevs/portfolio-web` repository is independently built and released.

## Backend structure and API testing

- [Package structure and DTO contracts](docs/package-structure.md): where controllers, services, entities, repositories, DTOs, and mappers belong.
- [Postman testing guide](docs/api-testing.md): import the collection/environment, sign in, run checks, and clean up test records.
- [API testing files](api/README.md): collection, empty environment template, and exported OpenAPI schema.

## API naming compatibility

Request and response JSON uses **snake_case**, including nested CMS metadata. See [the naming convention](docs/package-structure.md#json-naming-convention). Deploy this backend together with the updated `portfolio-web`; the older camelCase frontend is incompatible. Stored database keys and rich-text editor documents are preserved.

## Manage content

Start this backend, PostgreSQL, and the companion web application, then append `/admin/login` to the website address. Sign in through the website, not the backend's port. The public website footer also has an **Admin login** link.

There are no default credentials. For a new database, set your own `ADMIN_USERNAME` and `ADMIN_PASSWORD` before the first backend startup; see [database and secure bootstrap](#database-and-secure-bootstrap) below. The password must contain at least 14 characters and fit within 72 UTF-8 bytes. Bootstrap creates an initial account only: changing environment variables never changes an existing account. Change an existing password under **Security** after signing in.

The prepared cloud workspace has existing development credentials in the protected `/workspace/.runtime/dev.env` file. Read them privately in your own terminal/editor; never paste its contents or credential values into chat or logs. This file is not part of fresh clones, which need credentials you choose yourself.

In the admin sidebar, choose **Projects → Add project** or **Blog posts → Add article**. **Save draft** keeps the record private; **Publish project** or **Publish article** makes it visible publicly. Manage the introduction under **Profile**, employment under **Experience**, expertise under **Skills**, and longer technical narratives under **Case studies**. **Media library** manages files, **Messages** is the contact inbox, and **Settings** controls site configuration; **Categories** and **Tags** organize content.

Edits persist in PostgreSQL, and published changes appear on the public website without a frontend rebuild or deployment. Keep this backend running for sign-in, editing, and public API reads. See [architecture and the API contract](docs/architecture.md) for integration details.

## Prerequisites

- JDK 21, with `JAVA_HOME` and `java` available.
- PostgreSQL 16 or newer, running locally on port 5432.
- The checked-in Gradle wrapper; a separate Gradle install is unnecessary.

The normal development workflow does not require Docker. In an already isolated cloud task, use the existing checkout; there is no need to create an extra Git worktree.

## Database and secure bootstrap

As a PostgreSQL administrator, run these interactively. `\password` prompts securely instead of leaving a password in command history.

```sql
CREATE ROLE portfolio LOGIN;
\password portfolio
CREATE DATABASE portfolio OWNER portfolio;
```

In PowerShell, export the configuration and enter credentials interactively. Do not paste secrets into a tracked `.env` file or commit them. `.env.example` documents variable names; Spring Boot does not automatically load it.

```powershell
Set-Location portfolio-backend
$env:DATABASE_URL = 'jdbc:postgresql://127.0.0.1:5432/portfolio'
$env:DATABASE_USERNAME = 'portfolio'
$env:DATABASE_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Database password' -AsSecureString)).Password
$env:ADMIN_USERNAME = Read-Host 'Administrator username'
$env:ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Unique administrator password' -AsSecureString)).Password
$env:SESSION_COOKIE_SECURE = 'false' # Local HTTP only
$env:MEDIA_DIRECTORY = Join-Path $PWD 'uploads'
.\gradlew.bat bootRun
```

On Linux/macOS, export the same variables and run `./gradlew bootRun`. Flyway applies pending migrations at startup before the API serves requests. Set `SEED_DEMO=false` before the first startup to avoid demonstration content. Seeded sample projects and articles are labeled as demonstrations; the profile includes only the supplied identity. No employment history or private contact information is invented.

Bootstrap is only for creating the initial administrator. After it succeeds, remove `ADMIN_PASSWORD` from the service environment and use the authenticated password-change screen for subsequent changes. Use a unique password of at least 14 characters and no more than 72 UTF-8 bytes. For a secure production origin, set `SESSION_COOKIE_SECURE=true` and use the provided Nginx configuration. In-memory sessions expire after 30 minutes of inactivity; a process restart signs users out.

## Build and test

```powershell
.\gradlew.bat test bootJar
```

Integration tests require their own empty PostgreSQL test database, separate from production. See [testing](docs/testing.md) for variables and CI. Generated reports appear under `build/reports/tests/test`. The server binds to `127.0.0.1:8080`; readiness is `/actuator/health` locally. The frontend uses same-origin `/api/v1` paths.

The OpenAPI document is `/api/v1/openapi`; interactive API docs are at `/api/v1/docs` when enabled and authorized by the backend security policy. Keep these private in production. The API contract is documented in [architecture](docs/architecture.md).

## Production operations

- [VPS deployment and controlled migrations](docs/deployment.md)
- [Database/media backup and recovery](docs/backup-recovery.md)
- [Security and launch checklist](docs/security.md)

Deployment templates target a single Linux VPS with Java 21, PostgreSQL, Nginx, and systemd. Database and backend ports remain private. No server credentials or domain were supplied, so the repository includes deployment configuration rather than a live deployment.
