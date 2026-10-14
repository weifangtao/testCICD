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
# gitParameter 的参数名必须是 Git Parameter 插件真实支持的字段。
# 写错会让 Jenkinsfile 直接编译失败，例如：
#   Invalid parameter "credentialId", did you mean "defaultValue"?
python3 - "$target" <<'PY'
import re, sys, pathlib
p = pathlib.Path(sys.argv[1])
jenkinsfile = (p / 'ci/Jenkinsfile').read_text(encoding='utf-8')

# Git Parameter 插件 GitParameterDefinition 支持的参数
ALLOWED = {
    'name', 'type', 'defaultValue', 'description',
    'branch', 'branchFilter', 'tagFilter',
    'sortMode', 'selectedValue', 'useRepository',
    'quickFilterEnabled', 'listSize', 'requiredParameter',
}

m = re.search(r'gitParameter\s*\((.*?)\n\s*\)', jenkinsfile, re.S)
assert m, 'ci/Jenkinsfile 里找不到 gitParameter(...) 块'

args = set(re.findall(r'(\w+)\s*:', m.group(1)))
unknown = args - ALLOWED
assert not unknown, (
    'gitParameter 出现插件不支持的参数 %s，Jenkins 会直接编译失败。'
    '插件支持的参数：%s' % (sorted(unknown), sorted(ALLOWED))
)
print('OK: gitParameter 参数合法 (%s)' % ', '.join(sorted(args)))
PY

# BUILD_ENV 必须是 choice：用 string 在 Jenkins 界面上是**文本框**，只能手输。
# 而且选项必须与 project-config 的 gradle.profiles key 一致，否则下拉里会出现构建不了的项。
python3 - "$target" <<'PY'
import re, sys, pathlib
p = pathlib.Path(sys.argv[1])
jenkinsfile = (p / 'ci/Jenkinsfile').read_text(encoding='utf-8')
config = (p / 'ci/project-config.groovy').read_text(encoding='utf-8')

m = re.search(r"choice\(\s*name:\s*'BUILD_ENV'\s*,\s*choices:\s*\[([^\]]*)\]", jenkinsfile)
assert m, (
    'BUILD_ENV 必须用 choice(...)；用 string(...) 时 Jenkins 界面是文本框，只能手输'
)
choices = [c.strip().strip("'\"") for c in m.group(1).split(',') if c.strip()]

# gradle.profiles 的 key 缩进 12 空格，形如 `            test: [`
keys = re.findall(r'^\s{12}(\w+)\s*:\s*\[', config, re.M)
assert keys, 'ci/project-config.groovy 里找不到 gradle.profiles 的 key'

assert set(choices) == set(keys), (
    'BUILD_ENV 下拉选项 %s 与 gradle.profiles 的 key %s 不一致；'
    '下拉里会出现选不了/构建不了的组合' % (choices, keys)
)
print('OK: BUILD_ENV choice %s == gradle.profiles %s' % (choices, sorted(keys)))
PY
bash "$target/ci/init-android-project.sh" --name NextSmoke --package com.acceptance.next --output "$output" --no-git >/dev/null
[[ -f "$output/NextSmoke/app/src/main/java/com/acceptance/next/MainActivity.kt" ]]
if [[ "${1:-}" == "--build" ]]; then
  (cd "$target" && ./gradlew :app:assembleStagingDebug :app:testStagingDebugUnitTest)
fi
echo "Template verification passed in ${SECONDS}s (started at ${started}s). Output: $output"
