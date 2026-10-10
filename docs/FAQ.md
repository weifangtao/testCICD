# FAQ · 常见问题

按「**构建 / 架构 / 测试 / CI / iOS**」分类。带 🔥 的是**已经踩过的坑**，附真实报错。

---

## 构建

### 🔥 报 `Unresolved reference` 但代码看着没问题

先确认 **JDK 版本**：

```bash
java -version          # 本机默认可能是 1.8 ❌
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
java -version          # 应为 17+
```

AGP 9 要求 JDK 17+，用 8 会出现一堆莫名奇妙的编译错误。

---

### 🔥 `The KSP plugin was detected to be applied but its task class could not be found`

Hilt 插件与 KSP **必须在同一作用域声明**。两个都放根 `build.gradle.kts`：

```kotlin
// build.gradle.kts（根）
plugins {
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
}
```

`kotlin.multiplatform` 同理 —— 只在子模块声明会报
`plugin is already on the classpath with an unknown version`。

---

### 🔥 `Cannot use optimization.enable=true without setting android.r8.gradual.support flag`

AGP 9 起，开启 R8 需要显式开关。`gradle.properties` 加一行：

```properties
android.r8.gradual.support=true
```

---

### 🔥 `Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin`

AGP 9 的「内置 Kotlin」默认禁止该 DSL，但 KSP 正是靠它注册生成目录。
`gradle.properties` 放开：

```properties
android.disallowKotlinSourceSets=false
```

---

### 🔥 `using kotlin.multiplatform is not compatible with com.android.library since AGP 9.0`

AGP 9 起 KMP 的 Android target **必须用新插件**：

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)   // ← 不是 com.android.library
}

kotlin {
    android {              // ← 不是 androidTarget()，也不是 androidLibrary {}
        namespace = "..."
        compileSdk = 36
        minSdk = 24
    }
}
```

> 网上大部分 KMP 教程还是旧写法，会直接报错。

---

### 🔥 `Cannot inline bytecode built with JVM target 17 into bytecode that is being built with JVM target 11`

各模块 JVM target 不一致。统一到 **17**：

```kotlin
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
```

---

### `Dependency ... requires ... compile against version 37`

某些库（如 `hilt-navigation-compose 1.4.0`、`navigation-compose 2.10.x`）要求
compileSdk 37。两条路：

- 装 SDK 37 并升 `compileSdk`
- **降库版本**（本项目当前方案）：`hilt-navigation-compose 1.3.0`

---

### release 打包出来是 `-unsigned.apk`

说明没配置签名。见 [接入-checklist.md](./接入-checklist.md) 第 3 节。

快速判断当前状态：

```bash
ls app/build/outputs/apk/prod/release/
# 出现 *-unsigned.apk 就是没签名
```

未配置时构建**不会失败**（便于纯编译验证），但**装不上也发不出去**。

---

## 架构

### 新页面要改 `MainActivity` 吗？

**不用。** 各 feature 通过 Hilt 的 `@IntoSet` 贡献 `FeatureGraph`，
`MainActivity` 只做：

```kotlin
@Inject lateinit var featureGraphs: Set<@JvmSuppressWildcards FeatureGraph>

AppNavHost(navController, HomeRoute) {
    featureGraphs.forEach { it.register(this) }
}
```

照 [接入-checklist.md](./接入-checklist.md) 第 5 节走 5 步即可。

---

### 什么时候该用独立的 Activity，而不是壳内 Compose 页面？

| 场景 | 选择 |
|---|---|
| 需要被外部 App / 通知 / 深链唤起 | **独立 Activity 壳** |
| 需要独立任务栈或特殊 `launchMode` | **独立 Activity 壳** |
| 扫码、拍照、WebView 等系统交互重的页面 | **独立 Activity 壳** |
| 其余常规业务页（绝大多数）| **壳内 Compose 页面** |

> ⚠️ **不要一屏一 Activity**。真实项目几百个 Activity 会导致 Manifest 膨胀到
> 上千行，且跳转没有编译期校验。混合架构把大多数页面收进壳内，Manifest 只留几个壳。

---

### `AppStorage` / `AppEnvironment` 这些该放 `lib:core` 还是 `lib:shared`？

**判据只有一条**：

> **能在 iOS 上编译通过吗？** 能 → `lib:shared`；不能 → `lib:core`。

```bash
# 用这条命令验证
./gradlew :lib:shared:compileKotlinIosSimulatorArm64
```

- 接口 / 契约 / 纯数据模型 → `lib:shared`
- 任何用到 `android.*` / Compose / OkHttp / Room / DataStore 的 → `lib:core`

---

### 接口地址为什么写在代码里，不在 Gradle？

```kotlin
// lib/shared/.../lib/config/AppEnvironment.kt
enum class AppEnvironment(val key: String, val baseUrl: String, val enableHttpLogs: Boolean) {
    DEV ("dev",  "https://api-dev.company.com/",  true),
    TEST("test", "https://api-test.company.com/", true),
    PROD("prod", "https://api.company.com/",      false),
}
```

好处：

1. 改地址**不用动构建脚本**，不用为「同一环境换地址」重新打包
2. 新增环境只改**一处**
3. 测试包可在设置页**热切换**环境

Gradle 只提供 `APP_ENV` 标识（flavor 名与环境 key 解耦）。

> 注意：AGP 保留 `test*` 开头的 flavor 名，所以测试环境 flavor 叫 `staging`。

---

### 为什么不用 Retrofit？

Retrofit 是 JVM 专属，iOS 用不了。改用 **Ktor** 后：

- 同一份客户端代码 Android / iOS 共用（引擎：Android→OkHttp，iOS→Darwin）
- Ktor 的异常类型也是跨端的，`ErrorMapper` 因此能进 `lib:shared`

新增接口不需要写 Retrofit 接口声明，直接：

```kotlin
val result: ApiResult<List<PostDto>> = client.getResult("posts")
```

数据模型加 `@Serializable` 即可。

---

### 🔥 Logcat 里看不到网络请求

三个开关任一没满足，日志就不输出：

| 开关 | 默认值 | 说明 |
|---|---|---|
| `BuildConfig.DEBUG` | — | release 包**永远**不打印 |
| `environment.enableHttpLogs` | dev/test `true`，prod `false` | 环境级 |
| `NetworkConfig.httpLogger` | `HttpLogger.None` | **不传就什么都不打** |

最容易漏的是第三个。必须显式传日志出口：

```kotlin
NetworkConfig(
    enableHttpLogs = BuildConfig.DEBUG && environment.enableHttpLogs,
    httpLogLevel = HttpLogLevel.ALL,
    httpLogger = LogcatHttpLogger,       // ← 漏了这行就没有输出
)
```

然后过滤：

```bash
adb logcat -s Http
```

**注意**：Ktor 默认的 `Logger.DEFAULT` 走 `println`，在 Logcat 里会散在
`System.out` 标签下且经常看不到。本项目用自定义 logger 落到 `Http` 标签，
所以一定要传 `LogcatHttpLogger`。

顺带说明：日志已自动脱敏（`Authorization` / `Cookie` / `token` 等都会变 `***`），
所以开着日志是安全的。

---

### 为什么不用 SharedPreferences？

`commit()` 阻塞主线程有 ANR 风险，变更通知靠 `OnSharedPreferenceChangeListener`（易泄漏）。

本项目：**KV 用 DataStore，结构化数据用 Room**。

---

## 测试

### 🔥 `./gradlew test` 跑不到 `lib:shared` 的测试

KMP 的 Android 测试任务叫 `testAndroidHostTest`，**不在 `test` 生命周期里**，
会被静默跳过。项目已在 `lib/shared/build.gradle.kts` 加了别名任务修掉：

```kotlin
tasks.register("test") {
    dependsOn("testAndroidHostTest")
}
```

若你新建了 KMP 模块，记得照做。验证：

```bash
./gradlew test
# 应看到 28 个用例（含 lib:shared 的 ErrorMapperTest 11 个）
```

---

### 🔥 `States size expected:<3> but was:<2>`

`StateFlow` **按值去重**，且 `shareIn`/`stateIn` 的初始值会被立即替换或合流。

排查方式：在测试里打开日志

```kotlin
override val loggingEnabled = true
```

会打印每个 `[STATE] <-` / `[EVENT] <-` / `[DISPATCH]`，据此写期望值。

**两条经验**：

1. `StateFlow` 只发射**不相等**的值
2. 首个状态可能是 `stateIn` 的初始值，也可能是上游首帧 —— 取决于上游是否同步发射

---

### 🔥 `expected:<5> but was:<3>`（状态少了几帧）

多半是 `flatMapLatest` 取消了在途请求，或 `StateFlow` 合流。

异步依赖用 `returnsWithDelay` 拉开时序：

```kotlin
coEvery { repository.refresh() } returnsWithDelay ApiResult.Success(Unit)
```

---

### 怎么读 `MainActivity` 之外页面的状态做单测？

ViewModel 单测不需要 Android 环境，直接继承 `BaseMviViewModelTest`：

```kotlin
class HomeViewModelTest :
    BaseMviViewModelTest<HomeIntent, HomeState, HomeEvent, HomeViewModel>() {

    @Test
    fun `database content is pushed into state`() {
        runVMTest(
            vmProducer = { HomeViewModel(repository) },
            intents = flowOf(HomeIntent.Load),
            expectedStates = listOf(/* ... */),
        )
    }
}
```

测试基类通过 **Gradle testFixtures** 提供，物理上不会打进 APK：

```kotlin
testImplementation(testFixtures(project(":lib:core")))
```

---

## CI

### CI 里 release 包没签名

在 `ci/project-config.groovy` 的 `signing` 填上 Jenkins Credentials ID。
凭据全部从 Jenkins 取，**密钥不落仓库**：

```groovy
signing: [
    keystoreFile : 'android-keystore',        // Secret file 类型
    storePassword: 'android-store-password',  // Secret text 类型
    keyAlias     : 'android-key-alias',
    keyPassword  : 'android-key-password'
],
```

未配置时流水线会打印 `Signing credentials not configured; release output will be UNSIGNED.`

---

### CI 想指定版本号

构建参数 `VERSION_NAME` / `VERSION_CODE` 可覆盖，留空则自动算：

```
versionName = <baseName>.<BUILD_NUMBER>
versionCode = <startCode> + <BUILD_NUMBER>
```

---

### APK 文件名怎么变了？

由 `app/build.gradle.kts` 的 `androidComponents.onVariants` 统一命名：

```
<项目名>-prodRelease-v1.0.0.137-237.apk
```

**CI 依赖这个格式解析版本**，改命名规则要同步改 `ci/Jenkinsfile`。

---

## iOS

### 🔥 `linkDebugTestIosSimulatorArm64` 失败

需要**完整 Xcode**，只有 Command Line Tools 不够：

```bash
xcode-select -p     # 应为 /Applications/Xcode.app/Contents/Developer
```

```bash
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
```

> 注意：**编译** iOS 产物（`compileKotlinIosSimulatorArm64`）不需要 Xcode，
> 只有**链接 / 跑模拟器**才需要。

---

### iOS 上如何复用 Android 的业务代码？

当前共享范围是**契约 + 网络基础设施**：

| 层 | 现状 |
|---|---|
| 路由表 `AppRoute` / `Navigator` | ✅ 已共享 |
| MVI 契约 / UiState | ✅ 已共享 |
| Ktor 网络 + ErrorMapper | ✅ 已共享 |
| Repository / ViewModel | ⬜ 各端各写（当前选择）|
| UI | ⬜ iOS 用 SwiftUI |

若要进一步共享 Repository / ViewModel，需要：

1. DI 从 **Hilt 换成 Koin**（Hilt 不支持 KMP iOS）
2. ViewModel 移出 `app`，去掉 `@HiltViewModel`
3. 平台实现：`AppStorage` → NSUserDefaults

---

### 导航怎么映射到 SwiftUI？

```kotlin
// shared
@Serializable data object HomeRoute : AppRoute
@Serializable data class DetailRoute(val id: String) : AppRoute
```

iOS 侧用 `NavigationStack(path:)` + `navigationDestination` 对应；
ViewModel 的 `MviSingleEvent`（一次性事件）正好对应 SwiftUI 的 `.onReceive`。

---

## 还有问题？

1. 先看 [脚手架使用指南.md](./脚手架使用指南.md) 对应章节
2. 再对照 [接入-checklist.md](./接入-checklist.md) 的验收清单
3. 仍未解决 → 提到模板仓库 issue，注明**完整报错 + Gradle/Kotlin/AGP 版本**
