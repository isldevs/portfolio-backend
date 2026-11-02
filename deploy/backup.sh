#!/usr/bin/env bash
# Run as root on the VPS. PostgreSQL peer auth is used through its OS account.
# The backend is briefly stopped so its database and media files agree.
set -euo pipefail
backup_dir=${1:?Usage: backup.sh ABSOLUTE_NEW_BACKUP_DIRECTORY [DATABASE_NAME]}
database=${2:-portfolio}
[[ "$backup_dir" = /* && ! -e "$backup_dir" ]] || { echo 'Use a new absolute backup directory' >&2; exit 2; }
[[ "$database" =~ ^[A-Za-z0-9_]+$ ]] || { echo 'Invalid database name' >&2; exit 2; }
[[ -d /var/lib/portfolio/media ]] || { echo 'Media directory missing' >&2; exit 2; }
umask 077
mkdir -m 700 -- "$backup_dir"
was_running=false
if systemctl is-active --quiet portfolio-backend; then was_running=true; fi
resume() { if "$was_running"; then systemctl start portfolio-backend; fi; }
trap resume EXIT
systemctl stop portfolio-backend
runuser -u postgres -- pg_dump --format=custom --dbname="$database" > "$backup_dir/database.dump"
tar -czf "$backup_dir/media.tar.gz" -C /var/lib/portfolio media
pg_restore --list "$backup_dir/database.dump" > "$backup_dir/database.contents"
date -u +%Y-%m-%dT%H:%M:%SZ > "$backup_dir/created-at.txt"
readlink -f /srv/portfolio/backend/current > "$backup_dir/backend-release.txt"
(
  cd "$backup_dir"
  sha256sum database.dump media.tar.gz > SHA256SUMS
)
printf 'Backup completed at %s. Copy encrypted off-host and rehearse restoration.\n' "$backup_dir"
