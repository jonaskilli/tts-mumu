#!/bin/bash
# 统一构建脚本：自动判断是否已有离线依赖(build-deps/)
#   - 有 build-deps/  → 离线模式，直接复用，免联网
#   - 无 build-deps/  → 首次模式，先装环境(联网)再构建
# 用法： bash /workspace/build.sh          # 自动选择模式
#       bash /workspace/build.sh --fresh  # 强制重新安装环境(联网)
exec > /workspace/_build.log 2>&1
set -e
cd /workspace

FRESH=0
[ "$1" = "--fresh" ] && FRESH=1

if [ -d build-deps/jdk ] && [ -d build-deps/android-sdk ] && [ "$FRESH" -eq 0 ]; then
  echo "### 离线模式：使用 build-deps/ 里的依赖（免联网）"
  export JAVA_HOME=/workspace/build-deps/jdk
  export ANDROID_HOME=/workspace/build-deps/android-sdk
  export GRADLE_USER_HOME=/workspace/build-deps/gradle-home
else
  echo "### 首次/强制模式：安装 JDK+SDK（联网）"
  bash 依赖/setup_env.sh
  source /etc/profile.d/tts_build_env.sh 2>/dev/null || true
  export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
  export ANDROID_HOME=/root/Android/Sdk
fi
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

echo "===== BUILD START $(date) ====="
java -version
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --version 2>&1 | head -1

echo "### 确保 gradle.properties 含编译所需开关"
GP=/workspace/gradle.properties
grep -q "android.suppressUnsupportedCompileSdk" "$GP" || echo "android.suppressUnsupportedCompileSdk=36" >> "$GP"
grep -q "kotlin.daemon.jvmargs" "$GP" || echo "kotlin.daemon.jvmargs=-Dfile.encoding=UTF-8" >> "$GP"

echo "### gradle assembleAppDebug"
chmod +x gradlew
./gradlew assembleAppDebug --no-daemon --build-cache
echo "### rc=$?"

echo "### APK 产物:"
ls -la app/build/outputs/apk/app/debug/ 2>&1
echo "===== BUILD END $(date) ====="
