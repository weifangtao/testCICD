# testCICD

这是一个可直接接入现有 Jenkins 的 Android CI/CD 模板项目。

## 一键接入 Jenkins

1. 在 Jenkins 新建一个 Pipeline 任务。
2. 选择 `Pipeline script from SCM`，SCM 选择 Git。
3. 仓库地址填写：`https://github.com/weifangtao/testCICD.git`。
4. 分支填写：`*/main`，脚本路径填写：`ci/Jenkinsfile`。
5. 运行一次任务。流水线会自动启用每 5 分钟轮询。

后续向 `main` 推送代码后，Jenkins 自动执行编译检查；自动触发只运行
`./gradlew :app:compileDebugKotlin`，不会打包、上传蒲公英或发送飞书。

手动点击 Jenkins 的“构建”时，可以选择 `BUILD_ENV` 和 `BRANCH`，并按需打开
`UPLOAD_PGYER`、`SEND_FEISHU_NOTICE`。发布功能需要先在 Jenkins Credentials 和
`ci/project-config.groovy` 中配置对应密钥；默认均为关闭状态。

## 初始化新的 Android 项目

在本项目根目录执行：

```bash
./ci/init-android-project.sh \
  --name StockWms \
  --package com.company.stock.wms \
  --git-url https://github.com/your-org/stock-wms.git \
  --app-name 'Stock WMS'
```

脚本会复制 Android 工程、替换包名、生成 CI 配置并初始化 Git，不会自动提交或推送。
