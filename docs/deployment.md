# Deploy the backend independently

The application is a modular monolith on a single Linux VPS. Docker is optional and unnecessary. The companion frontend has its own release and systemd unit. No real host, domain, or deployment credential has been supplied; these files are operational templates, not evidence of a live deployment.

## Host preparation

Install JDK/JRE 21, PostgreSQL 16+, Nginx, systemd, Python 3 (readiness script), and Certbot from supported repositories. Create a dedicated PostgreSQL role/database using the README. Bind PostgreSQL to loopback in `postgresql.conf` and restrict its role in `pg_hba.conf`; use SCRAM authentication for local TCP. Do not expose port 5432 through the firewall.

Create a non-login `portfolio-api` Unix account, `/srv/portfolio/backend/releases`, `/var/lib/portfolio/media` owned by `portfolio-api`, and root-owned `/etc/portfolio`. Copy `deploy/backend.env.example` to `/etc/portfolio/backend.env` (mode `0600`) and supply real credentials securely. Set `SESSION_COOKIE_SECURE=true`, `SEED_DEMO=false`, and absolute persistent media storage. Use initial `ADMIN_USERNAME`/`ADMIN_PASSWORD` only until the first administrator has been created, then remove the bootstrap password from the environment file. Changing bootstrap variables never resets an existing account.

Install `deploy/portfolio-backend.service`, verify `/usr/bin/java`, and run `systemctl daemon-reload`. The service binds to `127.0.0.1:8080`; Nginx is its only public gateway. The unit allows writes only to the media directory. Do not run the API as root.

## Build, migrate, activate

On a trusted build host with a separate integration-test database, run:

```bash
./deploy/package-release.sh 2026-11-02.1 /tmp/portfolio-backend-2026-11-02.1.tar.gz
```

This runs the backend tests and packages the executable JAR. Transfer the artifact and checksum over authenticated SSH, verify SHA-256 on the VPS, then extract the trusted artifact into a new `/srv/portfolio/backend/releases/RELEASE_ID` directory, owned by root and readable by `portfolio-api`. A checksum does not authenticate an untrusted artifact publisher. Never overwrite the current release directory.

Before activation:

1. Review all new Flyway SQL scripts and compatibility with both the new application and the retained previous release.
2. Make a database dump and consistent media backup using [backup/recovery](backup-recovery.md). Verify the backup finished successfully and that a recent restore rehearsal exists.
3. Schedule maintenance for locking or destructive migrations; prefer additive expand/migrate/contract changes across separate releases. Test the migration against a restored production-shaped database first.

```bash
sudo ./deploy/activate-release.sh 2026-11-02.1
```

The script stops the old backend before changing its release symlink. Flyway migrates during the controlled startup before JPA schema validation and request serving. An application user must have the permissions required by these migrations; a future operational split can run reviewed migrations under a separate role. Hibernate never updates the schema automatically. Do not edit an already-applied migration or use `flyway repair` to conceal a checksum mismatch.

Activation waits for `/actuator/health` to report `UP`. On failure it stops the service and leaves the release/database state for inspection; it does not guess whether schema rollback is safe. Inspect `journalctl -u portfolio-backend` and `flyway_schema_history`, resolve the cause, and then activate a compatible release. Check an actual profile read, authenticated CMS operation, published page, and contact submission through HTTPS after local readiness passes.

## Reverse proxy and TLS

The Nginx template is shared with the frontend repository. Substitute the real DNS name and certificate paths; initially enable only the HTTP ACME challenge server, issue a certificate with Certbot webroot, then enable the TLS listener. Run `nginx -t` before every reload and test automated certificate renewal. The frontend deployment guide contains the full sequence.

Nginx overwrites forwarded headers instead of appending arbitrary client headers. This works with the backend's native forwarded-header handling because only the local trusted proxy can reach its listener. If you add another proxy layer, re-evaluate trusted-proxy and rate-limiter behavior instead of copying client IP headers blindly.

Do not expose `/actuator`, `/api/v1/openapi`, or API UI assets publicly unless intentionally reviewed. Keep inbound ports 3000, 8080, and 5432 private. Store runtime secrets outside Git and artifacts.

## Rollback

For a migration that is demonstrably backward compatible, activate a retained backend release through the same script. Flyway will retain the newer schema; it does not roll migrations backward. For incompatible or data-changing migrations, stop writes, restore the coordinated database/media backup to a separate recovery environment first, verify it, and use a documented recovery window. Restoring a database loses writes after the backup; involve the site owner before doing so. Do not automatically execute destructive down migrations.

Keep previous release artifacts, migration source, configuration versions without secret values, and backup metadata. Restarting Java invalidates in-memory login sessions; sign in again normally. Monitor service health, restarts, 5xx rates, database connections/disk, media disk, and backup jobs. Use `journalctl -u portfolio-backend`; never log passwords, session cookies, or contact-message bodies.

## Release workflow

The `Package backend release` GitHub Actions workflow can be triggered manually with a unique release ID. It builds/tests an artifact and checksum for download; it does not log into or deploy to a VPS. Deploy the verified artifact with the activation procedure above. This keeps server access and maintenance/migration timing under the operator’s control.
