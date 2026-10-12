#!/usr/bin/env bash
# 在临时目录验证初始化。传 --build 时还编译生成项目并执行登录/隔离测试。
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
started=$SECONDS
output="$(mktemp -d "${TMPDIR:-/tmp}/template-smoke.XXXXXX")"
name=TemplateSmoke
bash "$ROOT/ci/init-android-project.sh" --name "$name" --package com.example.testdemo.smoke --app-name 'Smoke & QA' --output "$output" --no-git
target="$output/$name"
[[ ! -e "$target/local.properties" && ! -e "$target/wms-auth.properties" && ! -e "$target/.kotlin" ]]
[[ -f "$target/app/src/main/java/com/example/testdemo/smoke/MainActivity.kt" ]]
[[ -f "$target/app/schemas/com.example.testdemo.smoke.data.local.AppDatabase/1.json" ]]
python3 - "$target" <<'PY'
import sys, pathlib, xml.etree.ElementTree as ET
p = pathlib.Path(sys.argv[1])
assert ET.parse(p/'app/src/main/res/values/strings.xml').find("string[@name='app_name']").text == '"Smoke & QA"'
assert 'rootProject.name = "TemplateSmoke"' in (p/'settings.gradle.kts').read_text()
PY
bash "$target/ci/init-android-project.sh" --name NextSmoke --package com.acceptance.next --output "$output" --no-git >/dev/null
[[ -f "$output/NextSmoke/app/src/main/java/com/acceptance/next/MainActivity.kt" ]]
if [[ "${1:-}" == "--build" ]]; then
  (cd "$target" && ./gradlew :app:assembleStagingDebug :app:testStagingDebugUnitTest)
fi
echo "Template verification passed in ${SECONDS}s (started at ${started}s). Output: $output"
