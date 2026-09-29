# Android CI/CD 使用说明

本项目的 Jenkins 配置和一键初始化入口如下：

```text
ci/Jenkinsfile
ci/project-config.groovy
ci/README.md
ci/init-android-project.sh
```

## Jenkins 编译检查和发布

统一使用一个 Jenkins Pipeline Job（例如：`testCICD`）。

“一键”只统一流水线流程，不会替每个项目创建 Git/Jenkins 账号，也不会自动生成蒲公英
API Key 或飞书 Webhook。每个项目需要在 `ci/project-config.groovy` 填写自己的仓库、
分支、Java/Android SDK 路径和 Gradle 任务；密钥统一放在 Jenkins Credentials。

自动构建每 5 分钟执行一次，默认检查 `main` 分支：

- 执行内容：`compileDebugKotlin`
- 默认不上传蒲公英
- 默认不发送飞书

手动构建时仍可选择发布分支和环境，并按需打开蒲公英、飞书选项。

构建时可以选择：

- `BRANCH`：要构建的分支；
- `BUILD_ENV`：测试包或生产包；
- `UPLOAD_PGYER`：是否上传蒲公英；
- `SEND_FEISHU_NOTICE`：是否发送飞书通知；
- `UPDATE_DESCRIPTION`：蒲公英更新说明。

`BUILD_ENV` 是项目配置中的 profile 名称，不再固定为 WMS 环境名。WMS 当前配置为
`test` / `release`，新建的普通 Android 项目可以配置为同样的名称，并分别映射到
`assembleDebug` / `assembleRelease`。

当前 Jenkins 节点使用 JDK 21，路径由
`ci/project-config.groovy` 的 `jenkins.javaHome` 指定。若项目要求 JDK 17，需先在
Jenkins 节点安装 JDK 17，再把该配置改为节点上的真实路径，不能填写不存在的目录。

发布流水线使用 `ci/Jenkinsfile`，项目差异配置放在 `ci/project-config.groovy`，密钥统一放在 Jenkins Credentials。

Jenkins Credentials 位置：**系统管理 → Credentials → System → Global credentials → Add Credentials**。
创建后把 Credential 的 ID 填入 `git.credentials`、`pgyerApiCredential`、
`pgyer.passwordCredential` 或 `feishu.webhookCredential`。自动编译检查不需要蒲公英和飞书密钥。

## 接入已有 Android 项目

如果项目已经由 Android Studio 创建，不需要重新复制整个 WMS 工程。把
`ci/Jenkinsfile` 和 `ci/project-config.groovy` 放到项目根目录的 `ci/` 下，然后只修改
`project-config.groovy`：

1. `git.url`、`git.defaultBranch` 和 `git.credentials`；
2. `jenkins.javaHome`、`jenkins.androidSdk`；
3. `gradle.module` 和 `gradle.profiles` 中的 Gradle task、variant；
4. 如果项目需要发布，再填写各 profile 的蒲公英配置和 Jenkins Credentials。

普通 Android 项目的核心配置示例：

```groovy
gradle: [
    module: ':app',
    defaultProfile: 'test',
    profiles: [
        test: [checkTask: 'compileDebugKotlin', packageTask: 'assembleDebug', variant: 'debug', aliases: []],
        release: [checkTask: 'compileReleaseKotlin', packageTask: 'assembleRelease', variant: 'release', aliases: []]
    ]
]
```

## 一键初始化 Android 模板项目

在当前项目根目录执行：

```bash
./ci/init-android-project.sh \
  --name StockWms \
  --package com.company.stock.wms \
  --git-url http://git.haoqianyi.com/k3/stock_wms.git \
  --app-name 'Stock WMS'
```

脚本会复制工程模板、替换包名、生成 CI 配置并初始化 Git，不会自动提交或 push。

更完整的接入说明见 [README.md](README.md)。
