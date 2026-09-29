# Jenkins Pipeline 模板

`ci/Jenkinsfile` 是可复用的 Android 构建发布模板，项目差异集中在
`ci/project-config.groovy`，密码、API Key 和飞书 Webhook 只放 Jenkins Credentials。

“一键”只统一流水线流程，不代表所有项目共用同一套账号、仓库地址或构建任务。
每个项目必须配置自己的 Git 地址、分支、Java/Android SDK 路径、Gradle task、蒲公英
App Key；密码、API Key 和飞书 Webhook URL 只在 Jenkins Credentials 中配置。

## 接入新项目

1. 复制 `ci/Jenkinsfile` 和 `ci/project-config.groovy`。
2. 修改 `project-config.groovy` 中的 Git 地址、Jenkins 工具路径、构建 profile、APK 路径和蒲公英配置。
3. 在 Jenkins 创建一个 Pipeline Job，选择 **Pipeline script from SCM**，配置默认分支和脚本路径 `ci/Jenkinsfile`。模板会自动启用每 5 分钟 SCM 轮询。
4. 保留 `BRANCH` Git 参数（模板也会声明该参数）；模板会按该参数重新 checkout 实际构建分支。
5. 只做自动编译检查时无需配置发布密钥；需要发布时，再配置该项目的 Git、蒲公英 API Key、安装密码和飞书 Webhook Credentials。

Jenkins Credentials 位置：**系统管理 → Credentials → System → Global credentials → Add Credentials**。
在 `project-config.groovy` 中填写 Credential 的 ID，例如 `git.credentials`、
`pgyerApiCredential`、`pgyer.passwordCredential` 和 `feishu.webhookCredential`。

`BUILD_ENV` 不再写死为 WMS 的几个环境，而是填写
`ci/project-config.groovy` 中 `gradle.profiles` 的 key。普通 Android 项目直接使用
`test` / `release`，也可以改成 `debug` / `release` 等项目自己的名称。
`git.defaultBranch` 用于没有选择 `BRANCH` 时的默认分支；新项目可以设置为
`origin/master` 或 `origin/main`。

当前 Jenkins 节点只有可用的 JDK 21，因此模板暂时使用
`/usr/lib/jvm/java-21-openjdk-amd64`。如果需要固定使用 JDK 17，必须先在 Jenkins
节点安装 JDK 17，再将 `ci/project-config.groovy` 中的 `jenkins.javaHome` 改成真实
的 JDK 17 `JAVA_HOME`；不能填写不存在的目录。

模板不包含任何密码、API Key Secret 或 Webhook URL。每个项目可以定义自己的
`gradle.profiles`，例如普通 Android 项目可以配置 `assembleDebug` 和
`assembleRelease`，WMS 项目可以继续配置自己的 flavor task。

示例：

```groovy
git: [
    url: 'http://git.example.com/mobile/testCICD.git',
        credentials: 'git-credential-id',
    defaultBranch: 'origin/main'
],
gradle: [
    module: ':app',
    defaultProfile: 'test',
    profiles: [
        test: [
            checkTask: 'compileDebugKotlin',
            packageTask: 'assembleDebug',
            variant: 'debug',
            aliases: []
        ],
        release: [
            checkTask: 'compileReleaseKotlin',
            packageTask: 'assembleRelease',
            variant: 'release',
            aliases: []
        ]
    ]
]
```

## Jenkins 接入参数

现有构建平台：`http://wms-appbuild.bestfulfill-inc.com:8080/`。
建议新建 Job：`testCICD`（也可以使用已有 Job 名称）。

该 Job 每 5 分钟执行一次默认分支的编译检查，默认关闭蒲公英上传和飞书通知；自动轮询触发时只执行 `checkTask`，不会打包、上传蒲公英或发送飞书。手动构建时才执行 `packageTask`。
代码仓库：`https://github.com/weifangtao/testCICD.git`。

在 Jenkins 的 Pipeline 区域选择 **Pipeline script from SCM**：

- SCM：Git
- Repository URL：上述代码仓库
- Credentials：Public 仓库可留空
- Script Path：`ci/Jenkinsfile`
- Script 所在分支：`main`

构建时使用 `BRANCH` 参数选择真正构建的代码分支；模板会按 `BRANCH` 再次 checkout。
如果需要发布，才配置蒲公英和飞书 Credentials；仅做自动编译检查时不需要这些凭据。

## 一键初始化新项目

在模板项目根目录执行：

```bash
./ci/init-android-project.sh \
  --name StockWms \
  --package com.company.stock.wms \
  --git-url http://git.haoqianyi.com/k3/stock_wms.git \
  --app-name 'Stock WMS'
```

脚本会复制工程模板、替换 Java/Kotlin 包名、移动包目录、更新 CI 项目配置并初始化 Git。
它不会自动提交，也不会覆盖已有目录。

初始化脚本不会复制生产 `.jks`。如果新项目目录没有 `sfwl_logistic_app.jks`，Gradle 会自动使用默认 debug 签名，保证初始化后可以先完成验证构建；正式发布前再通过 Jenkins Credentials 注入正式签名文件。

初始化完成后执行：

```bash
cd StockWms
./gradlew :app:assembleDebug
```

验收口径：从执行初始化命令到项目首次编译成功，目标控制在 4 个有效工时以内。
