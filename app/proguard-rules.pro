# ══════════════════════════════════════════════════════════════
# testDemo 脚手架 R8 规则
#
# Room / Hilt / Compose 自带 consumer 规则，这里只补需要手写的部分。
# ══════════════════════════════════════════════════════════════

# ── 通用：保留注解与泛型签名（反射/序列化依赖）──
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# ── 保留行号，崩溃栈才可读 ──
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ══════════════════════════════════════════════════════════════
# kotlinx.serialization
# @Serializable 类靠伴生对象里的 serializer() 工作，
# R8 会把没被直接引用的伴生对象删掉。
# ══════════════════════════════════════════════════════════════
-keepclassmembers class kotlinx.serialization.json.** { *; }
-dontnote kotlinx.serialization.**

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# 路由表 AppRoute 的子类不能被删/改名（深链、路由匹配依赖）
-keep class * implements com.example.testdemo.lib.router.AppRoute { *; }

# ══════════════════════════════════════════════════════════════
# Ktor / OkHttp / Okio（Ktor 在 Android 用 OkHttp 引擎）
# ══════════════════════════════════════════════════════════════
-dontwarn io.ktor.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.slf4j.**

-keepclassmembers class io.ktor.** { volatile <fields>; }

# ══════════════════════════════════════════════════════════════
# kotlinx.coroutines
# ══════════════════════════════════════════════════════════════
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keepclassmembers class **$WhenMappings { <fields>; }

# ══════════════════════════════════════════════════════════════
# Room / Hilt 补充（其余靠自带 consumer 规则）
# ══════════════════════════════════════════════════════════════
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# ══════════════════════════════════════════════════════════════
# 保留行号信息给崩溃上报（Bugly 等）
# ══════════════════════════════════════════════════════════════
-keep class com.example.testdemo.lib.log.** { *; }
