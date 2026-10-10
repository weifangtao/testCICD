plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.testdemo.lib.core"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }

    // MVI 单测基类放这里：与主源码同模块，但物理上不会进 APK
    testFixtures { enable = true }
}

dependencies {
    // 跨端共享契约
    implementation(project(":lib:shared"))
    // ── Compose ──
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.material3)

    // ── AndroidX 基础 ──
    api(libs.androidx.lifecycle.viewmodel.ktx)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.lifecycle.runtime.compose)
    api(libs.androidx.lifecycle.viewmodel.compose)
    api(libs.androidx.activity.compose)
    api(libs.androidx.fragment.ktx)

    // ── 存储：KV 用 DataStore，结构化数据用 Room ──
    api(libs.androidx.datastore.preferences)
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)

    // ── 协程 ──
    api(libs.kotlinx.coroutines.android)

    // 网络已迁移到 lib:shared 的 Ktor 实现

    // ── 图片 ──
    api(libs.coil.compose)

    // ── 导航 ──
    // JetBrains 的 KMP 移植版：Android 端可直接替换，iOS/桌面同样支持
    api(libs.jetbrains.navigation.compose)
    api(libs.kotlinx.serialization.json)

    // ── 本模块单测 ──
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // ── testFixtures（MVI 单测基类）──
    testFixturesApi(libs.junit)
    testFixturesApi(libs.kotlinx.coroutines.test)
    testFixturesApi(libs.mockk)
    testFixturesApi(project(":lib:shared"))
}
