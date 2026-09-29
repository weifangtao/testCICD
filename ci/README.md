# Jenkins Pipeline 模板

`ci/Jenkinsfile` 是可复用的 Android 构建发布模板，项目差异集中在
`ci/project-config.groovy`，密码、API Key 和飞书 Webhook 只放 Jenkins Credentials。

## 接入新项目

1. 复制 `ci/Jenkinsfile` 和 `ci/project-config.groovy`。
2. 修改 `project-config.groovy` 中的 Git 地址、Jenkins 工具路径、构建 profile、APK 路径和蒲公英配置。
3. 在 Jenkins 创建 Pipeline Job，选择 **Pipeline script from SCM**，配置默认分支和脚本路径 `ci/Jenkinsfile`。
4. 保留 `BRANCH` Git 参数（模板也会声明该参数）；模板会按该参数重新 checkout 实际构建分支。
5. 配置 Git、蒲公英 API Key、蒲公英安装密码和飞书 Webhook Credentials。

`BUILD_ENV` 不再写死为 WMS 的几个环境，而是填写
`ci/project-config.groovy` 中 `gradle.profiles` 的 key。普通 Android 项目直接使用
`test` / `release`，也可以改成 `debug` / `release` 等项目自己的名称。
`git.defaultBranch` 用于没有选择 `BRANCH` 时的默认分支；新项目可以设置为
`origin/master` 或 `origin/main`。

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
            task: 'assembleDebug',
            variant: 'debug',
            aliases: []
        ],
        release: [
            task: 'assembleRelease',
            variant: 'release',
            aliases: []
        ]
    ]
]
```

## 当前 Jenkins 接入参数

现有构建平台：`http://wms-appbuild.bestfulfill-inc.com:8080/`。
现有 Job：`k3_wms_android`。

该 Job 每 5 分钟执行一次 `test` 分支的编译检查，默认关闭蒲公英上传和飞书通知；手动构建时仍可选择分支、环境并开启发布选项。
代码仓库：`http://git.haoqianyi.com/k3/k3_wms_android.git`。

将现有 Job 切换为模板时，在 Jenkins 的 Pipeline 区域选择 **Pipeline script from SCM**：

- SCM：Git
- Repository URL：上述代码仓库
- Credentials：`k3-wms-git`
- Script Path：`ci/Jenkinsfile`
- Script 所在分支：先填包含模板的分支

构建时仍使用现有的 `BRANCH` 参数选择真正打包的代码分支；模板会按 `BRANCH` 再次 checkout。
蒲公英和飞书继续复用现有 Credentials：`pgyer-api-key-test`、`pgyer-api-key-release`、`pgyer-build-password`、`feishu-webhook`。

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
./gradlew :app:assembleK3HaoqianyiTestDebug
```

验收口径：从执行初始化命令到项目首次编译成功，目标控制在 4 个有效工时以内。
