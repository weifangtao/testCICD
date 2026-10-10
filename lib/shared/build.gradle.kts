/**
 * 跨端共享层（Kotlin Multiplatform）。
 *
 * 契约 + 网络基础设施（Ktor）。业务层（Repository / ViewModel）暂不放这里。
 *
 * 注：AGP 9 起 KMP 的 Android target 必须用
 * `com.android.kotlin.multiplatform.library`。
 */
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.example.testdemo.lib.shared"
        compileSdk = 36
        minSdk = 24
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)

        // 让 commonTest 在 Android 主机测试里也能跑
        withHostTest {}
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)

            // Ktor 客户端（跨端）
            api(libs.ktor.client.core)
            api(libs.ktor.client.content.negotiation)
            api(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)
        }
        androidMain.dependencies {
            // Android 用 OkHttp 引擎（和原 Retrofit 底层同一套）
            implementation(libs.ktor.client.okhttp)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        iosMain.dependencies {
            // iOS 用 Darwin（URLSession）引擎
            implementation(libs.ktor.client.darwin)
        }
    }
}

// ══════════════════════════════════════════════════════════════
// 让根项目的 `./gradlew test` 也覆盖本模块。
//
// KMP 的 Android 测试任务叫 testAndroidHostTest，不在 `test` 生命周期里，
// 不加这个别名的话根项目跑 test 会**静默跳过** lib:shared 的全部测试。
//
// iOS 测试（allTests）不在 CI 里跑 —— 链接测试二进制需要完整 Xcode。
// ══════════════════════════════════════════════════════════════
tasks.register("test") {
    group = "verification"
    description = "Alias for testAndroidHostTest, so `./gradlew test` covers this module."
    dependsOn("testAndroidHostTest")
}
