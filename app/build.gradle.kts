import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// ══════════════════════════════════════════════════════════════
// 版本号：优先 Gradle 属性（-PVERSION_CODE=..）→ 环境变量 → 默认值
// CI 里由 Jenkinsfile 注入，本地不用改代码
// ══════════════════════════════════════════════════════════════
fun buildValue(key: String, default: String): String =
    (findProperty(key) as String?)?.takeIf { it.isNotBlank() }
        ?: System.getenv(key)?.takeIf { it.isNotBlank() }
        ?: default

// ══════════════════════════════════════════════════════════════
// 签名：keystore.properties（本地，已 gitignore）→ 环境变量（CI 凭据）
// 两者都没有时，release 产出未签名包（不会构建失败，便于纯编译验证）
// ══════════════════════════════════════════════════════════════
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

fun signingValue(key: String): String? =
    keystoreProps.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: System.getenv(key)?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("RELEASE_STORE_FILE")
val hasReleaseKeystore = listOf(
    releaseStoreFile,
    signingValue("RELEASE_STORE_PASSWORD"),
    signingValue("RELEASE_KEY_ALIAS"),
    signingValue("RELEASE_KEY_PASSWORD"),
).all { !it.isNullOrBlank() }
val requireReleaseSigning = buildValue("REQUIRE_RELEASE_SIGNING", "false").toBoolean()

if (requireReleaseSigning && !hasReleaseKeystore) {
    error("Release signing is required, but RELEASE_STORE_FILE/PASSWORD/ALIAS/KEY_PASSWORD are incomplete")
}

val wmsProperties = Properties().apply {
    rootProject.file("wms-auth.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun wmsValue(key: String): String = (wmsProperties.getProperty(key) ?: System.getenv(key).orEmpty())
    .let { "\"" + it.replace("\\", "\\\\").replace("\"", "\\\"") + "\"" }

android {
    namespace = "com.example.testdemo"
    compileSdk { version = release(36) { minorApiLevel = 1 } }

    defaultConfig {
        applicationId = "com.example.testdemo"
        buildConfigField("String", "WMS_AES_KEY", wmsValue("WMS_AES_KEY"))
        buildConfigField("String", "WMS_AES_IV", wmsValue("WMS_AES_IV"))
        minSdk = 24
        targetSdk = 36
        versionCode = buildValue("VERSION_CODE", "1").toInt()
        versionName = buildValue("VERSION_NAME", "1.0.0")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = signingValue("RELEASE_STORE_PASSWORD")
                keyAlias = signingValue("RELEASE_KEY_ALIAS")
                keyPassword = signingValue("RELEASE_KEY_PASSWORD")

                // 覆盖 Android 7.0+（v2）与 9.0+（v3），v1 兜底老设备
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        // 接口地址不在这里配 —— 见 lib/shared/src/commonMain/.../config/AppEnvironment.kt
        create("dev") {
            dimension = "environment"; applicationIdSuffix = ".dev"
            buildConfigField("String", "APP_ENV", "\"dev\"")
        }
        // 注意：AGP 保留 test* 开头的 flavor 名，这里用 staging 代表测试环境。
        create("staging") {
            dimension = "environment"; applicationIdSuffix = ".test"
            buildConfigField("String", "APP_ENV", "\"test\"")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "APP_ENV", "\"prod\"")
        }
    }

    buildTypes {
        debug {
            // 不混淆，便于调试
            optimization { enable = false }
        }
        release {
            // R8：代码压缩 + 资源收缩
            optimization { enable = true }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests.isReturnDefaultValues = true }
}

// APK 命名带版本号，CI 依赖这个格式解析版本（Jenkinsfile 里 replaceFirst(/^.*-v/)）
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val versionName = output.versionName.getOrElse("1.0.0")
            val versionCode = output.versionCode.getOrElse(1)
            output.outputFileName.set("${rootProject.name}-${variant.name}-v${versionName}-${versionCode}.apk")
        }
    }
}

dependencies {
    implementation(project(":lib:core"))
    implementation(project(":lib:shared"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android)

    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.compiler)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)

    testImplementation(libs.junit)
    testImplementation("io.ktor:ktor-client-mock:3.2.3")
    testImplementation(testFixtures(project(":lib:core")))

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
