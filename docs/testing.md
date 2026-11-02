# Backend testing without Docker

Use Java 21, the checked-in Gradle wrapper, and a dedicated PostgreSQL database. Never point integration tests at production. The CI creates an ephemeral PostgreSQL service for each job; normal local development can use an installed PostgreSQL server.

Create a test role and database as a PostgreSQL administrator:

```sql
CREATE ROLE portfolio_test LOGIN;
\password portfolio_test
CREATE DATABASE portfolio_test OWNER portfolio_test;
```

PowerShell:

```powershell
$env:TEST_DB_URL = 'jdbc:postgresql://127.0.0.1:5432/portfolio_test'
$env:DB_USERNAME = 'portfolio_test'
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Test database password' -AsSecureString)).Password
.\gradlew.bat --no-daemon test bootJar
```

Bash uses `export TEST_DB_URL=...`, `export DB_USERNAME=...`, and a securely entered `DB_PASSWORD`, followed by `./gradlew --no-daemon test bootJar`. Flyway initializes the test schema. Test data must be disposable; integration tests create/update/delete resources. Read `build/reports/tests/test/index.html` for actual execution counts and failures. Do not interpret an unrun or zero-test command as validation.

The core workflow to exercise against the web UI is: sign in, create a draft project, confirm public exclusion, publish it, create a draft article, publish/update it, confirm the public text changes without rebuilding, submit a visitor contact form, read the admin inbox, sign out, and verify admin access fails. Also verify invalid/missing CSRF, anonymous writes, slug collisions, stale version updates, media type/size/path validation, and private media rejection. A successful compilation alone does not establish these behaviors.
