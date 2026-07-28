-keep class io.netty.**{*;}

# Ktor Server - 保留所有类和成员，防止反射获取构造函数时 single() 崩溃
-keep class io.ktor.server.engine.** { *; }
-keep class io.ktor.server.netty.** { *; }
-keep class io.ktor.server.application.** { *; }
-keep class io.ktor.server.routing.** { *; }
-keep class io.ktor.server.plugins.** { *; }
-keep class io.ktor.server.config.** { *; }
-keep class io.ktor.server.http.** { *; }
-keep class io.ktor.events.** { *; }

# 保留 Ktor 内部反射使用的类和构造函数
-keepclassmembers class io.ktor.server.engine.** {
    <init>(...);
}
-keepclassmembers class io.ktor.server.netty.** {
    <init>(...);
}

# 保留 kotlinx.serialization 相关类（TtsParams 等使用 @Serializable）
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.* <fields>;
}
