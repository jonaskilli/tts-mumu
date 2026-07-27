#!/bin/bash
# 一次性烘焙：把构建依赖打包进 build-deps/ 并推送到仓库
# 前置：在「云原生开发」里先 `bash 依赖/setup_env.sh` 且成功构建过一次
# 用法： bash /workspace/bake_deps.sh          # 完整：jdk + sdk + gradle缓存
#       bash /workspace/bake_deps.sh --lite    # 精简：只 jdk + sdk（跳过最大的 gradle 缓存，省 LFS 配额）
set -e
cd /workspace

LITE=0
[ "$1" = "--lite" ] && LITE=1

echo "### 复制 JDK 21"
rm -rf build-deps/jdk && cp -r /usr/lib/jvm/java-21-openjdk-amd64 build-deps/jdk

echo "### 复制 Android SDK (android-36)"
rm -rf build-deps/android-sdk && cp -r /root/Android/Sdk build-deps/android-sdk

if [ "$LITE" -eq 0 ]; then
  echo "### 复制 Gradle 缓存 (GRADLE_USER_HOME：wrapper 分发包 + maven 依赖)"
  rm -rf build-deps/gradle-home && cp -r ~/.gradle build-deps/gradle-home
fi

echo "### 体积预估（推送前确认 LFS 配额是否够用）:"
du -sh build-deps 2>/dev/null

echo "### 用 git-lfs 跟踪大文件，避免仓库对象膨胀"
git lfs install >/dev/null 2>&1 || true
git lfs track 'build-deps/jdk/**' 'build-deps/android-sdk/**'
[ "$LITE" -eq 0 ] && git lfs track 'build-deps/gradle-home/**'

echo "### 提交并推送"
git add .gitattributes build-deps
git commit -m "chore: 烘焙构建依赖(离线包) [$([ "$LITE" -eq 1 ] && echo lite || echo full)]"
git push origin mumu
echo "### 烘焙并推送完成 ✅  若以后 LFS 配额紧张，可用 bash bake_deps.sh --lite 只烘焙 jdk+sdk"
