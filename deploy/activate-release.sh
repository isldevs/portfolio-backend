#!/usr/bin/env bash
# Migrations run on startup. Review them and make verified backups beforehand.
set -euo pipefail
release_id=${1:?Usage: activate-release.sh RELEASE_ID}
[[ "$release_id" =~ ^[A-Za-z0-9][A-Za-z0-9._-]*$ ]] || exit 2
base=/srv/portfolio/backend
release="$base/releases/$release_id"
[[ -f "$release/app.jar" && -f "$release/RELEASE_ID" ]] || { echo 'Incomplete release' >&2; exit 2; }
[[ $(cat "$release/RELEASE_ID") == "$release_id" ]] || { echo 'Release ID mismatch' >&2; exit 2; }
link="$base/.current.$$"
trap 'rm -f -- "$link"' EXIT
systemctl stop portfolio-backend
ln -s -- "$release" "$link"
mv -Tf -- "$link" "$base/current"
if systemctl start portfolio-backend; then
  for attempt in {1..60}; do
    if curl --fail --silent --max-time 3 http://127.0.0.1:8080/actuator/health | python3 -c 'import json,sys;sys.exit(json.load(sys.stdin).get("status")!="UP")'; then
      printf 'Activated backend %s\n' "$release_id"
      exit 0
    fi
    sleep 1
  done
fi
systemctl stop portfolio-backend
printf 'Backend not ready. Service stopped; inspect journal and migration state. No automatic schema rollback was attempted.\n' >&2
exit 1
