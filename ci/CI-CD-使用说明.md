# Android 一键 CI/CD 使用说明（通用模板）

## 1. 这套方案解决什么问题

把 Android 项目的构建流程统一成一套 Jenkins Pipeline：新项目只需要复制 `ci/`
目录、填写项目配置、创建一个 Jenkins Job，就可以自动编译检查；需要发布时再手动
选择分支和环境，并按需上传蒲公英、发送飞书通知。

“一键”指流程和模板复用，不代表所有项目共用账号、密钥、仓库地址或 Gradle 任务。
这些项目差异必须写在各自的 `ci/project-config.groovy` 和 Jenkins Credentials 中。

## 2. 主项目与 Demo 对应关系

- 主项目：`k3_wms_android`，用于实际 WMS 业务开发和发布，保留 WMS 自己的分支、
  JDK 8、flavor、Gradle 任务、蒲公英 App Key 和飞书凭据。
- 一键 CI/CD Demo：`testCICD`，用于演示通用 `ci/` 模板、自动编译检查、手动打包和
  新项目初始化流程，不承载 WMS 的业务配置。

Demo 代码仓库：`https://github.com/weifangtao/testCICD`

Demo Jenkins Job：`http://wms-appbuild.bestfulfill-inc.com:8080/job/testCICD/`

主项目 Jenkins Job：`http://wms-appbuild.bestfulfill-inc.com:8080/job/k3_wms_android/`

两个项目同步的是流水线逻辑和文档结构；仓库地址、分支、Java/Android SDK、Gradle
任务、蒲公英和飞书配置必须按项目分别维护。

## 3. 目录和职责

```text
ci/
├── Jenkinsfile             # 通用流水线，不放密钥
├── project-config.groovy   # 当前项目的仓库、工具、任务和发布配置
├── init-android-project.sh # 从模板初始化新项目
├── README.md               # 快速接入说明
└── CI-CD-使用说明.md       # 本文档
```

## 4. 接入新项目

1. 将 `ci/` 目录复制到新项目根目录。
2. 修改 `ci/project-config.groovy`：
   - `git.url`、`git.defaultBranch`、`git.credentials`；
   - `jenkins.androidSdk`、可选的 `jenkins.javaHome`；
   - `gradle.module`、`gradle.profiles.*.checkTask` 和 `packageTask`；
   - 需要发布时再填写各环境的蒲公英 App Key 和 Credential ID。
3. 在 Jenkins 创建 **Pipeline script from SCM** Job：
   - Repository URL：项目 Git 地址；
   - Script Path：`ci/Jenkinsfile`；
   - Script 分支：项目默认分支。
4. 首次手动构建确认配置，之后由 SCM 轮询自动检查。

自动触发只执行 `checkTask`，不打包、不上传蒲公英、不发送飞书；手动构建才执行
`packageTask`，并根据参数决定是否发布。

## 5. Jenkins Credentials

位置：**系统管理 → Credentials → System → Global credentials → Add Credentials**。

凭据只保存在 Jenkins，不写进 Git。配置文件中只填写 Credential ID：

| 用途 | 配置字段 |
| --- | --- |
| 私有 Git 仓库 | `git.credentials` |
| 蒲公英 API Key | `pgyerApiCredential` |
| 蒲公英安装密码 | `pgyer.passwordCredential` |
| 飞书 Webhook | `feishu.webhookCredential` |

自动编译检查不需要蒲公英和飞书凭据。每个项目、每个环境可以使用不同的 App Key 和
Credential ID。

## 6. 构建参数和验收

- `BRANCH`：真正构建的 Git 分支；
- `BUILD_ENV`：`project-config.groovy` 中 `gradle.profiles` 的 key；
- `UPLOAD_PGYER`：是否上传蒲公英，默认关闭；
- `SEND_FEISHU_NOTICE`：是否发送飞书，默认关闭；
- `UPDATE_DESCRIPTION`：蒲公英更新说明。

验收标准：自动构建日志中出现实际 Java 版本和 `checkTask`，且没有打包、蒲公英、飞书
步骤；手动构建能生成 APK，打开发布参数后才执行对应发布步骤。

## 7. Java 环境规则

`jenkins.javaHome` 是可选项。只有 Jenkins 构建节点上确认目录真实存在时才填写；留空
时流水线使用节点 `PATH` 中的 Java，不会注入猜测的 JDK 路径。项目若要求固定 JDK 17，
必须先在节点安装 JDK 17，再填写真实 `JAVA_HOME`。

## 8. 一键初始化新项目

```bash
./ci/init-android-project.sh \
  --name StockWms \
  --package com.company.stock.wms \
  --git-url http://git.example.com/mobile/stock_wms.git \
  --app-name 'Stock WMS'
```

脚本会复制 Android 模板、替换包名、生成 `ci/project-config.groovy` 并初始化 Git；
不会自动提交、push、创建 Jenkins Job，也不会复制生产签名文件。初始化后先执行：

```bash
cd StockWms
./gradlew :app:assembleDebug
```

从执行初始化命令到首次编译成功，目标不超过 0.5 个工作日（约 4 小时）。

## 9. 常见问题

- `JAVA_HOME ... invalid directory`：清空 `jenkins.javaHome`，或填写节点上真实存在的路径。
- 找不到 `checkTask`：检查 profile 名称和模块前缀，例如 `:app:compileDebugKotlin`。
- 自动构建上传/发通知：确认 Job 使用最新 `ci/Jenkinsfile`，并区分 SCM 自动触发和手动构建。
- 凭据报错：检查 Jenkins Credentials 的 ID，不要把密码或 Token 写入配置文件。
