# testDemo

移动端统一脚手架：**多模块 + Compose + MVI**，用于新 Android 项目一键起步。

- 架构模式参考 `MVI-Coroutines-Flow`，通用能力移植自 `k3_wms_android` 的成熟实现
- 自带 Jenkins CI/CD 模板与一键初始化脚本

## 快速开始

```bash
./gradlew :app:assembleDevDebug      # 开发包
./gradlew :app:assembleStagingDebug  # 测试包
./gradlew :app:assembleProdRelease   # 生产包
./gradlew test                       # 跑单元测试
```

## 模块结构

当前共 **3 个模块**：

```text
testDemo/
├── app/          Android 业务壳：Compose/MVI、WMS 登录、Room 业务数据库
├── lib/core/     Android 实现：UI 基类、DataStore、Room 构建、日志、图片、导航
├── lib/shared/   KMP 契约、Ktor 网络、错误模型、环境与路由定义
├── ci/           Jenkins 流水线、初始化与模板验证脚本
└── docs/         使用指南、登录接入说明、OKR 验收清单
```

WMS 登录与 Android Keystore 在 `app/auth`；共享库保持通用，不包含 WMS 加密配置。
iOS 当前仅提供共享层及 Darwin 网络引擎，尚未实现 iOS 登录页面与持久化。

## 通用能力一览

| 包 | 提供的能力 |
|---|---|
| `lib.base` | MVI 契约（`MviIntent`/`MviViewState`/`MviSingleEvent`/`MviView`/`MviViewModel`）、`AbstractMviViewModel`、**`AbstractMviActivity` / `AbstractMviFragment`（内置统一标题栏）**、`AppTitleBar`、`MviScreen`、`UiState`+`UiStateHost`、`EnterKeyDebouncer`、`collectIn` |
| `lib.net` | **Ktor 跨端网络**：`NetworkClient`（引擎 Android→OkHttp / iOS→Darwin）、`ApiResult` / `ApiError` / `ErrorMapper`（7 类错误）、`ApiEnvelope` |
| `lib.image` | `ImageEngine` 抽象 + `ImageLoader` 门面 + `AppImage` + `ImageOptions`，默认 Coil，可整体替换 |
| `lib.storage` | `AppStorage` 接口 + **DataStore** 实现（全 suspend / Flow，不阻塞主线程） |
| `lib.db` | Room：`AppRoomConverters`（BigDecimal / List\<String\>）+ `buildDatabase` 统一构建入口 |
| `lib.log` | `AppLogger` + `CrashGuard` |
| `lib.navigation` | 另含 `FeatureGraph` 契约（配合 Hilt 自动注册）|
| `lib.config` | `AppEnvironment`（dev/test/prod）—— **接口地址写在枚举里，不在 Gradle**；支持运行时热切换 |
| `lib.router` | `AppRoute` 类型安全路由表（`@Serializable`）+ `Navigator` 契约，**零平台依赖** |
| `lib.navigation` | 导航装配：`AppNavHost` + `FeatureGraph`（自动注册）。用 **JetBrains KMP 版** navigation-compose，Android/iOS 通用 |
> **混合架构**：少量 Activity 壳 + 壳内 Compose 导航。
> 路由注册归各 feature 所有（`feature/<name>/XxxNavigation.kt`），
> 通过 Hilt `@IntoSet` **自动聚合** —— `MainActivity` **不引用任何具体页面**，新增页面零改动。
| `testFixtures` | `BaseMviViewModelTest`（`runVMTest`）、`TestCoroutineDispatcherRule`、`Expected`、`returnsWithDelay` |

## Demo 覆盖

每个 `lib` 的能力在 `app` 里都有可运行的主流程 demo：

| 页面 | 演示内容 |
|---|---|
| `feature/home` | **主流程**：MVI 全链路、网络请求、图片加载、UiState（加载/成功/错误+重试）、路由跳转 |
| `feature/detail` | 带参数路由（类型安全）、大图 + `ImageOptions` |
| `feature/settings` | 环境热切换（config）、存储读写 + **Flow 订阅**（storage） |

所有页面已统一使用 `AppTitleBar`（左返回 / 中标题 / 右操作区）。
| `feature/lab` | **能力演示**：UiState 四态、日志、崩溃兜底、扫描防抖、图片预加载/清缓存、错误映射 |

| 能力（所在模块）| 在哪看 demo |
|---|---|
| `lib.base` MVI 基座（lib:core）| home（MVI/UiState）、lab（四态 / EnterKeyDebouncer）|
| `lib.base.mvi` 契约（lib:shared）| 同上（契约层，两端共用）|
| `lib.net` Ktor 网络（lib:shared）| home（请求/错误态）、lab（ErrorMapper 映射）|
| `lib.image` 图片抽象（lib:core）| home（列表头像）、lab（选项/预加载/清缓存）|
| `lib.storage` 存储（接口 lib:shared / 实现 lib:core）| settings（写入 + Flow 订阅回显）|
| `lib.db` Room（lib:core）| home（离线缓存，网络失败回落）|
| `lib.log` 日志（lib:core）| lab（写日志 / 触发崩溃验证 CrashGuard）|
| `lib.config` 环境（lib:shared）| settings（运行时切换环境）|
| `lib.router` 路由表（lib:shared）| home → detail / settings → lab |
| `lib.navigation` 导航装配（lib:core）| `feature/*/XxxNavigation.kt` 自注册 |

> 📖 详细用法、目录规范、新增页面步骤见 **[docs/脚手架使用指南.md](docs/脚手架使用指南.md)**

## 环境配置

| flavor | 环境 key | applicationId 后缀 |
|---|---|---|
| `dev` | `dev` | `.dev` |
| `staging` | `test` | `.test` |
| `prod` | `prod` | 无 |

> **接口地址在代码里**：`lib/shared/src/commonMain/kotlin/com/example/testdemo/lib/config/AppEnvironment.kt`，改地址不用动 Gradle。
> Gradle 只提供 `APP_ENV` 标识（flavor 名与环境 key 解耦）。
>
> ⚠️ AGP 保留 `test*` 开头的 flavor 名，所以测试环境 flavor 叫 `staging`。

## 存储

| 场景 | 方案 |
|---|---|
| 配置 / 登录态 / 小 KV | **DataStore**（`AppStorage` 接口，全 suspend + Flow）|
| 结构化数据 / 离线缓存 | **Room**（`lib.db` 提供通用转换器与构建入口）|

```kotlin
// KV
storage.putString("token", value)
storage.observeString("token").collect { ... }

// Room：新增实体只需加进 @Database 的 entities
@Database(entities = [PostEntity::class], version = 1, exportSchema = false)
@TypeConverters(AppRoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        fun create(context: Context) = buildDatabase<AppDatabase>(context, "app.db") { ... }
    }
}
```

`demo` 里的 `PostRepository` 演示了 **数据库为单一数据源（SSOT）**：
UI 订阅 Room 的 `Flow`，网络只负责刷新写库。离线可用、切页不闪烁、
刷新失败不会清空已有内容。

## 测试

```bash
./gradlew test
```

测试覆盖网络错误映射、MVI 页面状态、扫描防抖、WMS 请求协议、密码加密兼容、会话恢复/退出/过期及环境隔离。实际数量以本次 Gradle XML/HTML 报告为准。

ViewModel 单测写法见 `app/src/test/.../HomeViewModelTest.kt`：继承 `BaseMviViewModelTest`，
声明意图流与期望状态/事件序列即可，不用手写订阅与断言样板。

测试基类通过 **Gradle testFixtures** 提供，使用方只需：

```kotlin
testImplementation(testFixtures(project(":lib:core")))
```

物理上不会打进 APK。

## 一键接入 Jenkins

1. Jenkins 新建 Pipeline 任务
2. 选择 `Pipeline script from SCM`，SCM 选 Git
3. 仓库地址：`https://github.com/weifangtao/testDemo.git`
4. 分支填 `*/main`，脚本路径填 `ci/Jenkinsfile`
5. 运行一次，流水线会自动启用每 5 分钟轮询

自动触发只跑编译检查（不开打包/上传/通知）；手动构建可选 `BUILD_ENV`、`BRANCH`，
并按需打开 `UPLOAD_PGYER`、`SEND_FEISHU_NOTICE`。

## 初始化新项目

```bash
./ci/init-android-project.sh \
  --name StockWms \
  --package com.company.stock.wms \
  --git-url https://github.com/your-org/stock-wms.git \
  --app-name 'Stock WMS'
```

脚本会复制工程、替换包名、生成 CI 配置并初始化 Git，不会自动提交或推送。

## 真实登录与验收

启动先进入 WMS 账号密码登录页。登录成功后进入原能力示例首页，设置页可退出登录；仅 Debug 包允许切换环境。常规员工登录使用 `employmentType=false`，临时工与仓库选择流程尚未接入。

本地复制 `wms-auth.properties.example` 为 `wms-auth.properties`，从接口负责人获取兼容配置；CI 使用 `WMS_AES_KEY` / `WMS_AES_IV` 凭据环境变量。文件已忽略，初始化脚本不会复制。配置为空仍能构建，登录时会提示配置缺失。

- [登录接入与隔离说明](docs/登录接入与隔离.md)
- [OKR 验收与发布清单](docs/OKR验收与发布.md)
- `bash ci/verify-template.sh` 验证初始化；加 `--build` 验证生成项目的 APK 和单测，需要 JDK 17+、Android SDK 与依赖下载环境。
