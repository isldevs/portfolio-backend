# Persistent-data backup and recovery

The PostgreSQL database and media directory form one recoverable unit. The database holds users, content, revisions, audit events, messages, and media metadata; file bytes live under `MEDIA_DIRECTORY`. Never back up only one. Public and private media can contain personal information, as do contact messages. Restrict backup access and encrypt off-host copies.

## Back up

`deploy/backup.sh` is a single-VPS maintenance script. It requires root, PostgreSQL tools, the `postgres` OS user with peer authentication, the installed systemd service, and `/var/lib/portfolio/media`. It briefly stops the backend to align file and database state, creates a custom-format dump and media archive, checks that the dump can be listed, records the backend release path and checksums, and restarts the service only if it was running before the backup.

```bash
sudo ./deploy/backup.sh /var/backups/portfolio/2026-11-02T120000Z portfolio
```

Use a new absolute output directory each time. A failed backup is not a valid recovery point. Monitor the command's exit status, service recovery, and available disk space; the script does not establish off-host durability. Schedule it during a documented maintenance window with a timer/cron entry appropriate to your uptime needs. For low downtime, replace the stop interval with a tested coordinated snapshot/WAL strategy rather than independently copying live files.

Keep at least daily backups and pre-deployment backups, with a retention period selected for the owner's recovery objectives. Use encrypted off-site storage under a separate access boundary. Retain multiple generations so accidental deletes and corruption are recoverable. Store runtime configuration in a password manager or encrypted configuration backup; never copy secret values into source control. Record recovery point and recovery time objectives with the site owner.

## Rehearse recovery

Restore to an isolated machine or a separate database/media directory first. A restore is the meaningful verification; successfully creating or listing a dump is insufficient. Ensure the PostgreSQL major version is compatible with the dump tools.

1. Verify `sha256sum -c SHA256SUMS` and artifact provenance. Treat backup contents as sensitive.
2. Create an empty recovery role/database. Restore without dropping production:

   ```bash
   sudo -u postgres createdb --owner=portfolio portfolio_recovery
   sudo -u postgres pg_restore --no-owner --role=portfolio --dbname=portfolio_recovery /secure/path/database.dump
   ```

   The `postgres` OS user must be able to read the dump; provide a narrowly scoped temporary file location with appropriate permissions instead of making a private backup world-readable. If the recovery role differs, review ownership and permissions explicitly.

3. Extract the trusted media archive into a new recovery directory, preserving keys and relative filenames. Set ownership to the backend service account and remove world access.
4. Start the backend with the matching retained application release, `SEED_DEMO=false`, and a recovery-specific `DATABASE_URL`/`MEDIA_DIRECTORY`. Keep this environment private and do not send outbound notifications.
5. Verify profile, published and draft content, article revisions, administrator login, media byte integrity and visibility, message counts, and audit history. Record the recovery duration and restored backup timestamp.

## Production recovery

Stop writes and preserve a forensic copy of the current database/media before replacing anything. Confirm the acceptable data-loss window with the site owner; restoring an older backup discards newer changes. Restore the verified pair into new database/storage locations, configure the matching application release, validate it privately, then switch the production configuration during maintenance. Keep the prior data until recovery is verified. Re-establish normal backup scheduling afterward.

No script in this repository automatically drops or restores production databases. Application rollback, schema rollback, and data restoration are separate operations; an old JAR cannot reverse a migration.
