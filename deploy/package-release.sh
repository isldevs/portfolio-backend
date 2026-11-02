#!/usr/bin/env bash
set -euo pipefail
repo=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$repo"
release_id=${1:?Usage: deploy/package-release.sh RELEASE_ID OUTPUT_TARBALL}
archive=${2:?Usage: deploy/package-release.sh RELEASE_ID OUTPUT_TARBALL}
[[ "$release_id" =~ ^[A-Za-z0-9][A-Za-z0-9._-]*$ ]] || { echo 'Invalid release ID' >&2; exit 2; }
[[ "$archive" = /* ]] || archive="$PWD/$archive"
[[ ! -e "$archive" ]] || { echo 'Output already exists' >&2; exit 2; }
./gradlew --no-daemon test bootJar
stage=$(mktemp -d)
trap 'rm -rf -- "$stage"' EXIT
shopt -s nullglob
jars=(build/libs/*.jar)
app_jars=()
for jar in "${jars[@]}"; do [[ "$jar" == *-plain.jar ]] || app_jars+=("$jar"); done
[[ ${#app_jars[@]} -eq 1 ]] || { echo 'Expected one executable JAR' >&2; exit 1; }
cp -- "${app_jars[0]}" "$stage/app.jar"
printf '%s\n' "$release_id" > "$stage/RELEASE_ID"
tar -czf "$archive" -C "$stage" .
sha256sum "$archive" > "$archive.sha256"
printf 'Packaged %s\n' "$archive"
