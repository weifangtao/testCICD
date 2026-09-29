# Android CI/CD 使用说明

本项目的 Jenkins 配置和一键初始化入口如下：

```text
ci/Jenkinsfile
ci/project-config.groovy
ci/README.md
ci/init-android-project.sh
```

## Jenkins 编译检查和发布

统一使用 Job：`k3_wms_android`

自动构建每 5 分钟执行一次，默认构建 `test` 分支的测试包：

- 执行内容：`assembleK3HaoqianyiTestDebug`
- 默认不上传蒲公英
- 默认不发送飞书

手动构建时仍可选择发布分支和环境，并打开蒲公英、飞书选项。

构建时可以选择：

- `BRANCH`：要构建的分支；
- `BUILD_ENV`：测试包或生产包；
- `UPLOAD_PGYER`：是否上传蒲公英；
- `SEND_FEISHU_NOTICE`：是否发送飞书通知；
- `UPDATE_DESCRIPTION`：蒲公英更新说明。

`BUILD_ENV` 是项目配置中的 profile 名称，不再固定为 WMS 环境名。WMS 当前配置为
`test` / `release`，新建的普通 Android 项目可以配置为同样的名称，并分别映射到
`assembleDebug` / `assembleRelease`。

发布流水线使用 `ci/Jenkinsfile`，项目差异配置放在 `ci/project-config.groovy`，密钥统一放在 Jenkins Credentials。

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
        test: [task: 'assembleDebug', variant: 'debug', aliases: []],
        release: [task: 'assembleRelease', variant: 'release', aliases: []]
    ]
]
```

## 一键初始化 WMS 模板项目

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
