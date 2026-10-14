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
- 一键 CI/CD Demo：`testDemo`，用于演示通用 `ci/` 模板、自动编译检查、手动打包和
  新项目初始化流程，不承载 WMS 的业务配置。

Demo 代码仓库：`https://github.com/weifangtao/testDemo`

Demo Jenkins Job：`http://wms-appbuild.bestfulfill-inc.com:8080/job/testDemo/`

主项目 Jenkins Job：`http://wms-appbuild.bestfulfill-inc.com:8080/job/k3_wms_android/`

两个项目同步的是流水线逻辑和文档结构；仓库地址、分支、Java/Android SDK、Gradle
任务、蒲公英和飞书配置必须按项目分别维护。

## 3. 目录和职责

```text
ci/
├── Jenkinsfile             # 通用流水线，不放密钥
├── project-config.groovy   # 当前项目的仓库、工具、任务和发布配置
├── jenkins-setup.groovy    # Script Console 一键诊断/创建 Jenkins Job
├── init-android-project.sh # 从模板初始化新项目
├── verify-template.sh      # 模板校验（含 Jenkinsfile 与配置一致性检查）
├── README.md               # 快速接入说明
└── CI-CD-使用说明.md       # 本文档
```

## 4. 接入新项目

1. 将 `ci/` 目录复制到新项目根目录（或用 `ci/init-android-project.sh` 一键生成）。
2. 修改 `ci/project-config.groovy`：
   - `git.url`、`git.defaultBranch`、`git.credentials`；
   - `jenkins.androidSdk`、可选的 `jenkins.javaHome`；
   - `gradle.module`、`gradle.profiles.*.qualityTask` 和 `packageTask`；
   - 需要发布时再填写各环境的蒲公英 App Key 和 Credential ID。
3. 确认 `ci/Jenkinsfile` 里 `gitParameter` 的参数名都是插件支持的
   （**`credentialId` 不存在**，写了会直接编译失败）。可跑
   `bash ci/verify-template.sh` 静态校验，见第 10 节。
4. 在 Jenkins 创建 **Pipeline script from SCM** Job：
   - Repository URL：项目 Git 地址；
   - Script Path：`ci/Jenkinsfile`；
   - Script 分支：项目默认分支。

   也可以用 `ci/jenkins-setup.groovy` 在 Script Console 里一键创建，避免手配漏项。
5. 首次手动构建确认配置（这次构建同时会让 Jenkins 注册 `parameters` 定义），
   之后由 SCM 轮询自动检查。
6. 跑一次 `bash ci/verify-template.sh`，确认模板一致性检查通过。


> **为什么 `BUILD_ENV` 用 `choice` 而不是 `string`**：`string` 在 Jenkins 界面上是
> **文本框**，只能手输，很容易拼错成不存在的 profile 名（构建到一半才报错）。
> `choice` 是下拉框，选项固定为 `test` / `prod`，与 `gradle.profiles` 的 key 一一对应。
> 新增环境时要**同时**改 `gradle.profiles` 和 Jenkinsfile 的 `choices`，
> `bash ci/verify-template.sh`（testDemo）会校验两者一致。

自动触发只执行 `qualityTask`，不打包、不上传蒲公英、不发送飞书；手动构建才执行
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
- `BUILD_ENV`：**下拉框**，选项为 `test` / `prod`（对应 `project-config.groovy` 中 `gradle.profiles` 的 key）；
- `UPLOAD_PGYER`：是否上传蒲公英，默认关闭；
- `SEND_FEISHU_NOTICE`：是否发送飞书，默认关闭；
- `UPDATE_DESCRIPTION`：蒲公英更新说明。

验收标准：自动构建日志中出现实际 Java 版本和 `qualityTask`，且没有打包、蒲公英、飞书
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
- 找不到 `qualityTask`：检查 profile 名称和模块前缀，例如 `test :app:lintStagingDebug`。
- 自动构建上传/发通知：确认 Job 使用最新 `ci/Jenkinsfile`，并区分 SCM 自动触发和手动构建。
- 凭据报错：检查 Jenkins Credentials 的 ID，不要把密码或 Token 写入配置文件。
- `BRANCH` 是文本输入框而不是下拉框：见第 10 节。

## 10. BRANCH 分支下拉框

`BRANCH` 依赖 Jenkins 插件 **Git Parameter**，靠 `ci/Jenkinsfile` 里的
`useRepository` 约定用哪个仓库列分支。**分支列表拉不到时会显示为空或报错。**

⚠️ 需要先纠正一个常见误解：

- **`credentialId` 不是这个插件的参数**。写了会直接编译失败：
  `Invalid parameter "credentialId", did you mean "defaultValue"?`
  凭据由 **Job 的 SCM 配置**自动提供，不需要（也无法）在这里单独传。
- **`useRepository` 是正则**，比较对象是 Job SCM 里的 Repository URL，
  只在 Job 定义了多个仓库时用来挑选；留空即用第一个仓库。

### 三个必要条件（缺一不可）

| # | 条件 | 检查方式 |
|---|---|---|
| ① | 安装了 **Git Parameter** 插件并重启过 Jenkins | 系统管理 → 插件管理 → Installed 搜索 `Git Parameter` |
| ② | `Jenkinsfile` 的 `gitParameter` 参数名都是插件支持的（**不要写 `credentialId`**）| `bash ci/verify-template.sh` 会静态校验 |
| ③ | Job **已经跑过一次构建**（或在配置页点过保存），让 Jenkins 重新注册参数定义 | 见下方说明 |

> ⚠️ **第 ③ 条最容易被忽略。**
> Jenkins 会一直沿用「上一次解析出来的参数定义」。所以你把 Jenkinsfile 从
> `string` 改成 `gitParameter` 之后，第一次打开「Build with Parameters」
> 看到的**可能还是旧的输入框**。跑一次构建（或打开 Job 配置点保存）就会刷新。

### 一键诊断与修复

用管理员账号打开 **http://wms-appbuild.bestfulfill-inc.com:8080/script**，
把 `ci/jenkins-setup.groovy` 整个粘进去：

1. 先保持 `CFG.apply = false` 执行 —— 只诊断，不动任何东西；
   它会打印插件版本、Job 的 SCM、以及 `BRANCH` 当前是 `GitParameterDefinition`
   还是 `StringParameterDefinition`。
2. 看清结论后把 `CFG.apply = true`，再执行一次 → 创建/修复 Job 的 SCM 与脚本路径。

脚本顶部 `CFG` 需要按项目改：`jobName`、`repoUrl`、`scriptPath`、
`defaultBranch`、`credentialsId`。

### 相关配置片段

```groovy
gitParameter(
        name: 'BRANCH',
        type: 'PT_BRANCH',
        defaultValue: 'origin/main',
        branchFilter: 'origin/(.*)',
        selectedValue: 'DEFAULT',
        sortMode: 'DESCENDING_SMART',
        useRepository: '.*',   // ← 正则；留空=用 Job SCM 的第一个仓库。凭据由 Job SCM 提供
        description: '打包分支（下拉选择，来自 Git 仓库）'
)
```

### 分支名写法兼容

`BRANCH` 支持下拉选择，也允许手输/上游传参。流水线会先归一化再 checkout：

| 输入 | 归一化结果 |
| --- | --- |
| `origin/main` | `main` |
| `main` | `main` |
| `refs/heads/main` | `main` |
| `refs/remotes/origin/main` | `main` |
| `  main  ` | `main` |

构建日志里会打印 `Resolved branch: xxx (BRANCH param: ...)`，便于确认实际用的分支。

### 想让下拉框同时列出 Tag

把 `type: 'PT_BRANCH'` 改成 `type: 'PT_BRANCH_TAG'`，分支和 Tag 会出现在同一个
下拉框里（发版按 Tag 构建时有用）。

## 模板生成验证与 WMS 配置

`bash ci/verify-template.sh --build` 会在临时目录生成项目、验证包名/schema/配置排除规则，然后编译并运行生成项目单测。节点需安装 bash、rsync、rg、Perl、Python 3、JDK 17+ 并设置 ANDROID_HOME。

WMS 兼容参数通过 Jenkins Secret text 凭据注入 `WMS_AES_KEY` 和 `WMS_AES_IV`（在 `ci/project-config.groovy` 的 `wmsAuth` 中填写两个 Credential ID）。流水线会将它们传给 Gradle，缺失时质量检查仍可运行；需要登录的发布包必须配置后再打包。参数不写入 Git、构建说明或日志。
签名、蒲公英与飞书凭据仍按项目实际配置，不能将本地构建通过当成 Jenkins 发布成功。
