#!/bin/bash
# 未来构建：直接复用仓库里的 build-deps 离线依赖，无需联网下载
# 用法：在「云原生开发」终端执行  bash /workspace/restore_build.sh
exec > /workspace/_build.log 2>&1
set -e
cd /workspace

# 关键：把三个环境变量指到仓库内的离线包，Gradle 缓存路径保持一致即可移植
export JAVA_HOME=/workspace/build-deps/jdk
export ANDROID_HOME=/workspace/build-deps/android-sdk
export GRADLE_USER_HOME=/workspace/build-deps/gradle-home
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

echo "===== RESTORE BUILD START $(date) ====="
echo "### env check:"
java -version
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --version 2>&1 | head -1

echo "### 确保 gradle.properties 含编译所需开关"
GP=/workspace/gradle.properties
grep -q "android.suppressUnsupportedCompileSdk" "$GP" || echo "android.suppressUnsupportedCompileSdk=36" >> "$GP"
grep -q "kotlin.daemon.jvmargs" "$GP" || echo "kotlin.daemon.jvmargs=-Dfile.encoding=UTF-8" >> "$GP"

echo "### gradle assembleAppDebug (含今天改动)"
chmod +x gradlew
./gradlew assembleAppDebug --no-daemon --build-cache
echo "### rc=$?"

echo "### APK 产物:"
ls -la app/build/outputs/apk/app/debug/ 2>&1
echo "===== RESTORE BUILD END $(date) ====="
