# 接入 Checklist

新项目从本脚手架起步时，照着这份清单走一遍即可。

> 目标：**30 分钟内**跑起来一个可编译、可测试、可打包、可发版的工程。

---

## 0. 环境要求

| 项 | 版本 | 检查命令 |
|---|---|---|
| JDK | **17+**（AGP 9 要求）| `java -version` |
| Android SDK | **36**（compileSdk）| — |
| Gradle | 用 `./gradlew`，无需本机安装 | `./gradlew --version` |
| ripgrep | 一键脚本需要 | `brew install ripgrep` |
| Xcode | **仅在跑 iOS 时需要** | `xcode-select -p` 应为 `/Applications/Xcode.app/...` |

> ⚠️ **JDK 必须是 17 以上**。本机默认 `java` 可能是 8，需显式设置：
> ```bash
> export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
> export PATH="$JAVA_HOME/bin:$PATH"
> ```

---

## 1. 从模板生成新项目

```bash
git clone https://github.com/weifangtao/testDemo.git
cd testDemo
./ci/init-android-project.sh \
    --name      MyApp \
    --package   com.itl.myapp \
    --output    ~/AndroidStudioProjects \
    --git-url   https://github.com/your-org/myapp.git
```

脚本会自动完成：包名全量替换、**所有模块（app / lib/core / lib/shared）的包目录搬迁**、项目名 / 应用名替换、git 初始化。

验证生成的工程：

```bash
cd ~/AndroidStudioProjects/MyApp
./gradlew :app:assembleDevDebug test
```

- [ ] 构建成功
- [ ] 28 个测试全绿

---

## 2. 必改项

| # | 改什么 | 在哪 |
|---|---|---|
| 1 | **接口地址** | `lib/shared/src/commonMain/kotlin/<包名>/lib/config/AppEnvironment.kt` |
| 2 | 应用显示名 | `app/src/main/res/values/strings.xml` 的 `app_name` |
| 3 | 主题色 / 字体 | `app/src/main/java/<包名>/ui/theme/` |
| 4 | 崩溃上报 appkey | `app/src/main/java/<包名>/AppApplication.kt`（`CrashGuard.install`）|

- [ ] 三个环境的 `baseUrl` 已改成真实地址
- [ ] `strings.xml` 的 app_name 已改

> 💡 **接口地址写在代码里，不在 Gradle** —— 改地址不用动构建脚本，测试包还能在设置页热切换。

---

## 3. 配置签名（release 必需）

**不做这步，release 产出的是 `-unsigned.apk`，装不上也发不出去。**

```bash
# ① 生成 keystore（已有则跳过）
keytool -genkeypair -v \
  -keystore keystore/release.jks \
  -alias myapp \
  -keyalg RSA -keysize 2048 -validity 10000

# ② 配置密码
cp keystore.properties.example keystore.properties
# 编辑填入 storePassword / keyAlias / keyPassword
```

- [ ] `keystore/release.jks` 存在
- [ ] `keystore.properties` 已填（**该文件已被 .gitignore 忽略，不会入库**）
- [ ] 验证：`./gradlew :app:assembleProdRelease` 产物**不是** `-unsigned.apk`

本地生成的文件位置：

```
keystore/release.jks          ← 密钥（gitignore）
keystore.properties           ← 密码（gitignore）
keystore.properties.example   ← 模板（入库）
```

---

## 4. 熟悉目录规范

```
MyApp/
├── app/                   业务壳（唯一 application 模块）
│   ├── data/              数据层：local(Room) / remote / Repository
│   ├── feature/           业务功能，每页一套 Contract/VM/Screen/Navigation
│   └── di/                Hilt 模块
├── lib/core/              Android 实现层
│   ├── base/              MVI 实现 + Activity/Fragment 基类 + TitleBar
│   ├── net/ storage/ db/ image/ log/ navigation/
└── lib/shared/            KMP 跨端共享层（Android + iOS）
    ├── router/            AppRoute + Navigator
    ├── base/mvi/          MVI 契约 + UiState
    └── net/ storage/ config/
```

**分层铁律**：

| 层 | 放什么 | 判据 |
|---|---|---|
| `lib:shared` | 平台无关契约 | **能在 iOS 上编译通过吗？** |
| `lib:core` | Android 实现 | 用了 Android / Compose / OkHttp / Room |
| `app` | 业务 + 装配 | — |

- [ ] 团队明确：业务代码**不许**依赖 Android（否则将来 iOS 复用不了）

---

## 5. 新增页面（5 步）

1. `app/feature/<name>/<Name>Contract.kt` — Intent / State / Event / PartialChange
2. `app/feature/<name>/<Name>ViewModel.kt` — `@HiltViewModel` + `scan(reduce)`
3. `app/feature/<name>/<Name>Screen.kt` — `MviScreen` + `UiStateHost` + `AppTitleBar`
4. `app/feature/<name>/<Name>Navigation.kt` — `@IntoSet` 贡献 `FeatureGraph`
5. `lib/shared/.../lib/router/AppRoute.kt` 加路由类型

**`MainActivity` 不需要改** —— Hilt 自动聚合所有 `FeatureGraph`。

- [ ] 试跑一次第 5 步，确认 `MainActivity` 零改动

---

## 6. 接入 CI

### 6.1 Jenkins 侧准备

| 类型 | 建议 ID | 内容 |
|---|---|---|
| Secret file | `android-keystore` | `release.jks` |
| Secret text | `android-store-password` | keystore 密码 |
| Secret text | `android-key-alias` | key 别名 |
| Secret text | `android-key-password` | key 密码 |
| Secret text | `pgyer-api-key` | 蒲公英 API Key |

### 6.2 项目侧配置

编辑 `ci/project-config.groovy`：

```groovy
git: [ url: 'https://github.com/your-org/myapp.git', credentials: '', defaultBranch: 'origin/main' ],
jenkins: [ androidSdk: '/var/lib/jenkins/android-sdk', gradleUserHome: '/var/lib/jenkins/.gradle' ],
version: [ baseName: '1.0.0', startCode: 100 ],
signing: [
    keystoreFile : 'android-keystore',
    storePassword: 'android-store-password',
    keyAlias     : 'android-key-alias',
    keyPassword  : 'android-key-password'
],
pgyer: [ passwordCredential: 'pgyer-build-password' ],
```

- [ ] Jenkins 新建 Pipeline Job，SCM 指向本仓库，脚本路径 `ci/Jenkinsfile`
- [ ] 上面的凭据已在 Jenkins Credentials 建好
- [ ] `ci/project-config.groovy` 的 signing / version 已填

### 6.3 版本号规则

```
versionName = <baseName>.<BUILD_NUMBER>     例：1.0.0.137
versionCode = <startCode> + <BUILD_NUMBER>  例：237
```

构建时可用参数 `VERSION_NAME` / `VERSION_CODE` 覆盖。

APK 命名格式（CI 依赖它解析版本）：

```
<项目名>-prodRelease-v1.0.0.137-237.apk
```

- [ ] 手动触发一次，确认蒲公英链接可打开

---

## 7. 验收清单

| # | 检查 | 命令 / 方式 |
|---|---|---|
| 1 | 编译通过 | `./gradlew :app:assembleDevDebug` |
| 2 | 测试全绿 | `./gradlew test`（应有 28 个用例）|
| 3 | release 可签名 | `./gradlew :app:assembleProdRelease`，产物无 `-unsigned` |
| 4 | 三套包可装 | `assembleDevDebug` / `assembleStagingDebug` / `assembleProdRelease` |
| 5 | 环境可热切换 | 设置页切换环境 → 请求走新地址 |
| 6 | 离线可用 | 关网络打开首页 → 显示缓存 + 顶部提示 |
| 7 | CI 可发版 | Jenkins 手动构建 → 蒲公英链接可下载 |
| 8 | iOS 可编译 | `./gradlew :lib:shared:compileKotlinIosSimulatorArm64` |

---

## 8. 推广落地建议

| 阶段 | 动作 | 产出 |
|---|---|---|
| 第 1 周 | 选 1 个新项目试点接入 | 试点反馈 issue 列表 |
| 第 2 周 | 组内分享会（1 小时）| 演示：初始化 → 新增页面 → CI 出包 |
| 第 3 周 | 把试点项目的问题回流到模板 | 模板 v1.1 |
| 持续 | 新项目默认从模板起步 | 模板仓库设为团队 Pin |

**分享会大纲**（60 分钟）：

1. 为什么需要统一脚手架（10 min）—— 各组重复造轮子的问题
2. 架构与分层（15 min）—— 三模块 + 分层铁律 + 为什么这么分
3. 实操演示（20 min）—— 一键初始化 → 新增页面 → 提交 → CI 出包
4. 接入规范与 FAQ（10 min）
5. 答疑（5 min）

---

## 常见问题

见 [FAQ.md](./FAQ.md)。
