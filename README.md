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

只有 **2 个模块**：业务壳 + 一个通用能力库。

```
testDemo/
├── app/                业务壳工程（唯一 application 模块）
│   └── feature/        home / detail / settings / lab
├── lib/core/           通用能力（所有基建都在这里）
│   ├── base/           MVI 基座 + Activity/Fragment 基类 + TitleBar + UiState + 防抖
│   ├── net/            网络层：NetworkClient / ApiResult / ApiError / ErrorMapper
│   ├── image/          图片能力抽象（外观模式，默认 Coil）
│   ├── storage/        Key-Value 存储（DataStore）
│   ├── db/             Room 通用转换器 + 数据库构建入口
│   ├── log/            日志门面 + 崩溃兜底
│   ├── config/         环境与 BaseUrl 配置
│   ├── router/         路由契约（纯 Kotlin：AppRoute + Navigator）
│   ├── navigation/     导航实现（Navigation Compose）
│   └── testFixtures/   MVI 单测基类（不会进 APK）
└── ci/                 Jenkinsfile / 打包 / 一键初始化
```

> 包名按能力划分（`lib.base` / `lib.net` / …），**模块只有一个**。
> 需要真正独立交付或可选依赖时再拆 —— 拆比合容易。

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

> **接口地址在代码里**：`lib/core/config/AppEnvironment.kt`，改地址不用动 Gradle。
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

当前覆盖：

| 模块 | 用例数 | 内容 |
|---|---|---|
| `lib:core` | 9 | 扫描枪防抖 6 + 路由表 3 |
| `lib:shared`（commonTest）| 11 | ErrorMapper 七类错误映射 |
| `app` | 8 | `HomeViewModel` 4 + `DetailViewModel` 3 + 冒烟 1 |
| **合计** | **28** | `./gradlew test` 一条命令覆盖 |

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
