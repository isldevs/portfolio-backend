# Prepared cloud workspace

Both Git repositories live directly under `/workspace`. Use the existing checkouts; this cloud task is already isolated and does not need a Git worktree.

Verified local Java compiler, PostgreSQL and Gradle installations are retained under `/workspace/.tools`. The PostgreSQL cluster, media and generated development-only credentials are outside both Git repositories in `/workspace/.runtime`. `dev.env` has mode 0600; source it without printing it or committing its values.

To refresh the prepared snapshot, stop the web process first and run:

```bash
/workspace/.runtime/refresh.sh
```

This verifies tests and production builds. It stages the next backend artifact at `/workspace/.runtime/backend/app-next.jar`; stop the old backend before replacing `app.jar`. Services must be started again after a restored cloud session. The saved environment startup instructions contain the exact commands and readiness checks. Dependencies and database files survive publication; live processes and login sessions do not.

For ordinary development, source `/workspace/.runtime/dev.env`, run `./gradlew bootRun` in `/workspace/portfolio-backend`, and run `npm run dev` in `/workspace/portfolio-web`. Run long-lived services in persistent terminal sessions. Keep ports local. For a production smoke check, copy `.next/static` into `.next/standalone/.next/static` and run `HOSTNAME=127.0.0.1 PORT=3000 node .next/standalone/server.js`.

Use `CHROMIUM_EXECUTABLE_PATH=/usr/bin/chromium npm run test:e2e` with credentials sourced from `dev.env` for the full browser suite. No separate browser download is required here. On other machines install a supported Playwright browser using its documented installer. Production setup is independent of this cloud snapshot; follow the Windows/local and VPS deployment documentation.
