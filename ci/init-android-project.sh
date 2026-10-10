#!/usr/bin/env bash
#
# 一键从脚手架初始化新项目。
#
#   ./ci/init-android-project.sh --name MyApp --package com.itl.myapp
#
# 会完成：
#   1. 复制模板（排除 .git / build / 密钥）
#   2. 全量替换包名（app + lib/core + lib/shared 所有 source set）
#   3. 搬迁所有模块的包目录（含 KMP 的 commonMain / androidMain / iosMain）
#   4. 替换项目名、应用名、Git 地址
#   5. git init + remote add
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OLD_PACKAGE="com.example.testdemo"
OLD_APP_NAME="testDemo"
TEMPLATE_GIT_URL="https://github.com/weifangtao/testDemo.git"
DEFAULT_GIT_URL="https://github.com/your-org/your-project.git"

usage() {
  cat <<'USAGE'
Usage:
  init-android-project.sh --name NAME --package PACKAGE [options]

Options:
  --name NAME         项目名（Gradle rootProject.name，也是目录名）
  --package PACKAGE   新包名，如 com.itl.myapp
  --output DIR        输出目录（默认当前目录）
  --git-url URL       新仓库地址
  --app-name NAME     桌面显示名（默认与 --name 相同）
  --no-git            不初始化 git
  --dry-run           只打印计划
  -h, --help          显示帮助
USAGE
}

die() { echo "[init-project][error] $*" >&2; exit 1; }
log() { echo "[init-project] $*"; }

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
[[ -n "$app_name" ]] || app_name="$name"

output="$(cd "$output" 2>/dev/null && pwd)" || die "output directory does not exist"
target="$output/$name"
[[ "$target" != "$ROOT" ]] || die "target cannot be the template directory"
[[ ! -e "$target" ]] || die "target already exists: $target"

log "template : $ROOT"
log "target   : $target"
log "package  : $OLD_PACKAGE -> $package"
log "app name : $app_name"
log "git      : $git_url"
if [[ "$dry_run" == true ]]; then log "--dry-run, exiting"; exit 0; fi

command -v rsync >/dev/null 2>&1 || die "rsync is required"
command -v rg >/dev/null 2>&1 || die "ripgrep (rg) is required: brew install ripgrep"

mkdir -p "$target"
rsync -a \
  --exclude '.git/' \
  --exclude '.gradle/' \
  --exclude '.idea/' \
  --exclude '**/build/' \
  --exclude 'keystore.properties' \
  --exclude '*.jks' \
  --exclude '*.keystore' \
  "$ROOT/" "$target/"

# ── 1) 全量替换包名（所有模块、所有 source set）──
while IFS= read -r -d '' file; do
  replace_text "$file" "$OLD_PACKAGE" "$package"
done < <(rg -l -0 --hidden -g '!.git/**' -g '!**/.gradle/**' -g '!**/build/**' -g '!*.jks' "$OLD_PACKAGE" "$target" || true)
log "package name replaced"

# ── 2) 搬迁包目录（不再写死 app，覆盖所有模块与 KMP source set）──
old_path="$(printf '%s' "$OLD_PACKAGE" | tr '.' '/')"
new_path="$(printf '%s' "$package" | tr '.' '/')"
moved=0
while IFS= read -r -d '' old_dir; do
  new_dir="${old_dir%"$old_path"}$new_path"
  mkdir -p "$(dirname "$new_dir")"
  mv "$old_dir" "$new_dir"
  moved=$((moved + 1))
  log "  moved ${old_dir#"$target"/} -> ${new_dir#"$target"/}"
done < <(find "$target" -type d -path "*/$old_path" -not -path "*/build/*" -not -path "*/.git/*" -print0)
[[ $moved -gt 0 ]] || die "no package directory matching '$old_path' was found"
log "moved $moved package dir(s)"

# 清理搬迁后留下的空目录（如 com/example）
find "$target" -type d -empty -not -path "*/build/*" -not -path "*/.git/*" -delete 2>/dev/null || true

# ── 3) 替换模板 Git 地址 ──
while IFS= read -r -d '' file; do
  replace_text "$file" "$TEMPLATE_GIT_URL" "$git_url"
done < <(rg -l -0 --hidden -g '!.git/**' -g '!**/.gradle/**' -g '!**/build/**' "$TEMPLATE_GIT_URL" "$target" || true)
log "git url replaced"

# ── 4) 项目名 / 应用名 ──
replace_text "$target/settings.gradle.kts" "rootProject.name = \"$OLD_APP_NAME\"" "rootProject.name = \"$name\""
replace_text "$target/app/src/main/res/values/strings.xml" "$OLD_APP_NAME" "$app_name"

# README 标题（若存在）
if [[ -f "$target/README.md" ]]; then
  replace_text "$target/README.md" "# $OLD_APP_NAME" "# $name"
fi

# 文档里出现的模板名（保留说明语境时可用 --app-name 覆盖）
log "project name -> $name, app name -> $app_name"

# ── 5) git ──
if [[ "$init_git" == true ]]; then
  git -C "$target" init >/dev/null
  git -C "$target" remote add origin "$git_url" 2>/dev/null || true
  log "git initialized"
fi

cat <<EOF

[init-project] ✅ 初始化完成: $target

下一步：
  cd $target
  # 1) 配置签名（可选，release 需要）
  cp keystore.properties.example keystore.properties   # 然后填入密码
  # 2) 首次构建
  ./gradlew :app:assembleDevDebug
  # 3) 跑测试
  ./gradlew test
  # 4) 改接口地址
  #    lib/shared/src/commonMain/kotlin/$(printf '%s' "$package" | tr '.' '/')/lib/config/AppEnvironment.kt

模块结构：
  app/         业务壳（Hilt + Compose）
  lib/core/    Android 实现层
  lib/shared/  KMP 跨端共享层（Android + iOS）
EOF
