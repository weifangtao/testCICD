#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OLD_PACKAGE="com.itl.k3.wms"
DEFAULT_GIT_URL="http://git.haoqianyi.com/k3/k3_wms_android.git"

usage() {
  echo "Usage: $0 --name NAME --package PACKAGE [--output DIR] [--git-url URL] [--app-name NAME] [--no-git] [--dry-run]"
}

die() { echo "[init-project][error] $*" >&2; exit 1; }
replace_text() {
  local file="$1" old="$2" new="$3"
  OLD_VALUE="$old" NEW_VALUE="$new" perl -pi -e 's/\Q$ENV{OLD_VALUE}\E/$ENV{NEW_VALUE}/g' "$file"
}

name=""
package=""
output="$(pwd)"
git_url="$DEFAULT_GIT_URL"
app_name=""
init_git=true
dry_run=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --name) [[ $# -gt 1 ]] || die "--name requires a value"; name="$2"; shift 2 ;;
    --package) [[ $# -gt 1 ]] || die "--package requires a value"; package="$2"; shift 2 ;;
    --output) [[ $# -gt 1 ]] || die "--output requires a value"; output="$2"; shift 2 ;;
    --git-url) [[ $# -gt 1 ]] || die "--git-url requires a value"; git_url="$2"; shift 2 ;;
    --app-name) [[ $# -gt 1 ]] || die "--app-name requires a value"; app_name="$2"; shift 2 ;;
    --no-git) init_git=false; shift ;;
    --dry-run) dry_run=true; shift ;;
    -h|--help) usage; exit 0 ;;
    *) die "unknown option: $1" ;;
  esac
done

[[ "$name" =~ ^[A-Za-z][A-Za-z0-9_-]*$ ]] || die "invalid or missing --name"
[[ "$package" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$ ]] || die "invalid or missing --package"
output="$(cd "$output" 2>/dev/null && pwd)" || die "output directory does not exist"
target="$output/$name"
[[ "$target" != "$ROOT" ]] || die "target cannot be the template directory"
[[ ! -e "$target" ]] || die "target already exists: $target"

echo "[init-project] template: $ROOT"
echo "[init-project] target: $target"
echo "[init-project] package: $OLD_PACKAGE -> $package"
echo "[init-project] git: $git_url"
[[ "$dry_run" == true ]] && exit 0

command -v rsync >/dev/null 2>&1 || die "rsync is required"
mkdir -p "$target"
rsync -a --exclude '.git/' --exclude '.gradle/' --exclude '**/build/' --exclude 'apk/' --exclude '*.jks' "$ROOT/" "$target/"

while IFS= read -r -d '' file; do
  replace_text "$file" "$OLD_PACKAGE" "$package"
done < <(rg -l -0 --hidden -g '!.git/**' -g '!**/.gradle/**' -g '!**/build/**' -g '!**/*.apk' -g '!**/*.jks' "$OLD_PACKAGE" "$target" || true)

old_path="$(printf '%s' "$OLD_PACKAGE" | tr '.' '/')"
new_path="$(printf '%s' "$package" | tr '.' '/')"
for root in "$target/app/src/main/java" "$target/app/src/main/kotlin" "$target/app/src/test/java" "$target/app/src/androidTest/java"; do
  old_dir="$root/$old_path"
  new_dir="$root/$new_path"
  if [[ -d "$old_dir" ]]; then
    mkdir -p "$(dirname "$new_dir")"
    mv "$old_dir" "$new_dir"
  fi
done

replace_text "$target/ci/project-config.groovy" "$DEFAULT_GIT_URL" "$git_url"
if [[ -n "$app_name" ]]; then
  while IFS= read -r -d '' file; do
    replace_text "$file" "灏仟亿" "$app_name"
  done < <(rg -l -0 --hidden -g '!.git/**' -g '!**/.gradle/**' -g '!**/build/**' "灏仟亿" "$target" || true)
fi

if [[ "$init_git" == true ]]; then
  git -C "$target" init >/dev/null
  git -C "$target" remote add origin "$git_url"
fi

echo "[init-project] initialized: $target"
echo "[init-project] verify: cd $target && ./gradlew :app:assembleK3HaoqianyiTestDebug"
