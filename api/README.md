# API testing files

Start with [the step-by-step Postman guide](../docs/api-testing.md).

- `portfolio-backend.postman_collection.json`: 54 API examples with CSRF/session handling and response assertions. Data-changing examples are disabled by default.
- `portfolio-local.postman_environment.json`: safe local template with empty credentials. Set values privately after import.
- `openapi.json`: exported typed API contract from the refactored backend.

Use a disposable test database for write examples. Never commit populated environments, cookies, passwords, or API runner reports.
