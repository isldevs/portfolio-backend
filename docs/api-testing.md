# Test the backend API with Postman

Postman is a desktop application for sending requests to the backend and viewing its responses. You do not need to write Java code to use this collection.

## Files to import

- [Postman collection](../api/portfolio-backend.postman_collection.json): 54 organized example requests and automatic checks.
- [Local environment template](../api/portfolio-local.postman_environment.json): server address and empty credential fields.
- [OpenAPI contract](../api/openapi.json): the exported API definitions for tools and developers.

The collection is for a **disposable development/test database**. Do not enable its write tests against your live portfolio: the examples publish test records, submit a message, and delete records they create. A read-only run still signs in/out and creates the normal authentication audit entries.

## 1. Prepare the backend

Ask your developer to start PostgreSQL and this backend with a separate test database and a test administrator account. The repository README explains bootstrap. There are no default credentials in these files.

The default address is `http://127.0.0.1:8080`. This means the backend is on your own computer. If it runs elsewhere, ask the developer for the correct test address. This collection was also verified using a separate local backend on port 8082; change the address to match your instance.

**Database compatibility:** upstream commits before this refactor changed existing Flyway migrations and renamed tables. This change does not repair or migrate an older installed database. Tests were run on fresh isolated databases. For an existing installation, have the operator compare its migration history and prepare an upgrade/backup plan. Do not delete a live database or disable Flyway validation to make tests pass.

## 2. Import and select your environment

1. Install/open a current version of Postman.
2. Click **Import** and choose both JSON files listed above.
3. Open **Environments → Portfolio local (fill credentials privately)**.
4. Set the values in the table below.
5. Select that environment in Postman's environment selector before sending requests.

| Variable              | What to enter                                                                                     |
| --------------------- | ------------------------------------------------------------------------------------------------- |
| `baseUrl`             | Backend origin, for example `http://127.0.0.1:8080`, without a trailing slash or `/api/v1` suffix |
| `username`            | Your test administrator username                                                                  |
| `password`            | Its password, entered privately                                                                   |
| `allowWrites`         | Leave `false` for the first run                                                                   |
| `allowProfileUpdate`  | Leave `false` unless intentionally testing profile saving                                         |
| `allowPasswordChange` | Leave `false`; password changes should be tested individually                                     |
| `new_password`        | Leave empty unless intentionally testing password change                                          |
| `mediaFile`           | Leave empty unless testing upload; see below                                                      |

Environment values override collection variables with the same name. Change the value in the **selected environment**, not just the collection, when enabling a test.

Keep populated credentials local/private. Do not share, commit, screenshot, or export an environment after filling in passwords. The committed environment is deliberately empty of credentials.

## 3. Run the safe first check

Open the collection and choose **Run collection**. Run one iteration in the listed order with `allowWrites=false`. Review the **Test Results** after completion. Green checks mean the actual response matched that test; skipped write requests have not been tested.

Keep Postman's cookie jar enabled. The login response establishes a session cookie, which Postman sends on later administrator requests. This is session authentication, not a bearer-token API.

The collection automatically fetches a fresh CSRF token before state-changing requests and adds its required header. Do not manually copy an old token or disable CSRF protection. Login/logout can change session/token state.

## 4. Run the content-changing tests

Only after confirming the backend uses disposable data:

1. Set `allowWrites=true` in the selected environment.
2. Keep `allowPasswordChange=false`.
3. Run the complete collection in its listed order, once.
4. Check the test results and verify that the created project/article/message have been deleted by their cleanup requests.

The content lifecycle defaults to `kind=projects`. You can set the collection variable to `articles` or `case-studies` when repeating that lifecycle. Its explicit publish/unpublish endpoints support these three kinds. Other kinds are covered by the public/admin list examples and share the generic create/update/delete API documented in OpenAPI.

The collection stores IDs, slugs, and current version numbers from responses automatically. Later requests need those values. Running a request in the middle without its setup requests can fail. The “Anonymous identity must fail” check expects a fresh logged-out cookie jar: use the final **Sign out** request or clear cookies before restarting a partially completed run.

### Optional profile test

Set both `allowWrites=true` and `allowProfileUpdate=true`. The administrator-read folder captures the current profile. The optional update sends those same values back and advances its version. Do not run this against a live profile being edited by someone else.

### Optional media test

Set `mediaFile` to the absolute path of a local test PNG, JPEG, GIF, WebP, or PDF, below the 10 MB limit. Alternatively select the local file in the upload request's **Body → form-data → file** field. Postman may ask for permission to read files outside its working directory; use a permitted local folder.

With `allowWrites=true`, the optional requests upload a PUBLIC test asset, download it through both download endpoints, then delete it. Its URL is publicly accessible while the file exists. Do not upload confidential material. To inspect private visibility separately, change the upload visibility to PRIVATE; public download should then return 404, so the unchanged public-download example's expected-200 assertion will need to be adjusted for that manual test.

### Optional password test

Run **Change password** individually, after other checks. It requires `allowWrites=true`, `allowPasswordChange=true`, the correct existing `password`, and a chosen `new_password` of at least 14 characters and at most 72 UTF-8 bytes.

This changes the actual test account password and ends its sessions. Update your local `password` value and sign in again afterward. Leave this request disabled in normal collection runs; otherwise later authenticated requests may fail after the password change.

## What each folder tests

| Folder                        | Purpose                                                                                                                      |
| ----------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| 01 Public API                 | Health, CSRF response, public profile, and published lists for all eight content kinds                                       |
| 02 Session                    | Anonymous access rejection, login, and current administrator DTO                                                             |
| 03 Administrator reads        | Dashboard, audit, all content lists, profile, inbox, and media pagination                                                    |
| 04 Content lifecycle          | Create draft, read, verify draft privacy, update, publish, public detail, reject stale version, duplicate, unpublish, delete |
| 05 Article revisions          | Create article, list typed revision snapshots, restore as draft, delete test article                                         |
| 06 Contact inbox lifecycle    | Submit message, read it, mark it read with version checking, delete it                                                       |
| 07 Optional manual operations | Profile saving, file upload/download/delete, and opt-in password change                                                      |
| 08 End session                | Logout and rejection of access after logout                                                                                  |

Health and OpenAPI are intentionally private in the documented production proxy setup. A blocked health request through a public production origin does not prove the backend is broken; use the local test origin.

## Common results explained

| Result                     | Meaning / next step                                                                                           |
| -------------------------- | ------------------------------------------------------------------------------------------------------------- |
| 200                        | Request succeeded and returned data                                                                           |
| 201                        | A record/file/message was created                                                                             |
| 204                        | Succeeded with no response body; do not try to parse JSON                                                     |
| 400                        | Check required fields, slug, file format/size, or request values                                              |
| 401                        | Login is required, credentials are incorrect, or the session ended                                            |
| 403                        | Check session cookies, CSRF handling, and whether the same backend address is used throughout                 |
| 404                        | Record missing/unpublished, wrong slug/ID, or private file accessed publicly; some negative tests expect this |
| 409                        | Version conflict, duplicate slug, or referenced record/file; read the latest record before retrying           |
| 429                        | Too many requests/login attempts; observe `Retry-After` when provided and stop repeating the request          |
| Cannot connect / 502 / 503 | Check backend, PostgreSQL, port, proxy, and test address                                                      |

If a run stops halfway, test records may remain. Use their captured IDs and version values in the cleanup requests, or remove only the known test records through the admin screen. Slugs start with `api-check-` or `api-article-`; review records before deleting anything. Never use a broad delete query against a live database.

## Developer command-line runner

A current Newman runner can execute this Postman v2.1 collection. Supply an environment file containing local test values outside the repository:

```bash
newman run api/portfolio-backend.postman_collection.json -e /private/path/local-test-environment.json
```

Install Newman using your normal approved tooling. Keep its reports and populated environment files out of Git: reports can include request bodies, cookies, or credentials. The collection uses `pm.execution.skipRequest()` to prevent unintended writes; use a runner version that supports it. Validation used Newman 6.2.1.

## Validation performed for this change

- 30 backend tests passed, including response-contract and profile-field filtering checks.
- JAR build and Spotless formatting checks passed.
- Collection run on a disposable local backend: 125 assertions passed; password change was intentionally skipped. Profile-save and public-media upload/download/delete were exercised.
- Default read-only collection run: 81 assertions passed.
- OpenAPI was exported from the running refactored backend with typed response schemas.

These results describe the local validation run, not a production deployment or an existing database upgrade.

## Request and response names

JSON API fields use snake_case: `header_name` in the CSRF response, `current_password`/`new_password` for password changes, `full_name`/`short_bio` in profiles, `display_order`/`created_at` in content, and `total_elements`/`total_pages` in list responses. CMS metadata also uses snake_case, such as `cover_image_url`. The collection checks response property names automatically.

Keep the editor document format inside `body` unchanged. Its type names and attributes are part of the editor protocol. Postman's own collection format and control-variable names such as `baseUrl`/`allowWrites` are not API JSON fields.
