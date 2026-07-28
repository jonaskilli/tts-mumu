#!/bin/bash
###############################################################################
# TTS Server Android 构建环境一键安装脚本
#
# 用途：在新环境中快速安装打包 APK 所需的全部依赖（JDK 21 + Android SDK）
# 用法：bash 依赖/setup_env.sh
#
# 两种模式：
#   1. 在线模式（默认）：通过阿里云镜像 apt 在线装 JDK，sdkmanager 在线装 SDK
#   2. 离线模式：用本文件夹里的 deb 包和 zip 离线安装（--offline 参数）
#
# 环境变量（安装完成后写入 /etc/profile.d/tts_build_env.sh）：
#   JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
#   ANDROID_HOME=/root/Android/Sdk
###############################################################################
set -e

# 脚本所在目录（用于定位离线包）
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 安装目标
JAVA_HOME_PATH="/usr/lib/jvm/java-21-openjdk-amd64"
ANDROID_SDK_DIR="/root/Android/Sdk"
PROFILE_FILE="/etc/profile.d/tts_build_env.sh"

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 是否离线模式
OFFLINE=false
if [ "$1" == "--offline" ] || [ "$1" == "-o" ]; then
    OFFLINE=true
    info "离线模式：使用本地包安装"
fi

# 需要 root 权限
if [ "$(id -u)" -ne 0 ]; then
    error "请使用 root 权限运行：sudo bash 依赖/setup_env.sh"
    exit 1
fi

###############################################################################
# 第1步：安装 JDK 21
###############################################################################
install_jdk() {
    # 检查是否已安装
    if [ -d "$JAVA_HOME_PATH" ] && "$JAVA_HOME_PATH/bin/java" -version 2>&1 | grep -q "21\."; then
        info "JDK 21 已安装，跳过"
        return 0
    fi
    info "开始安装 JDK 21..."

    if [ "$OFFLINE" = true ]; then
        # 离线：用本地 deb 包
        info "离线模式：使用本地 deb 包安装"
        cd "$SCRIPT_DIR"
        if ls openjdk-21-*.deb 1>/dev/null 2>&1; then
            dpkg -i openjdk-21-*.deb 2>/dev/null || true
            # 补齐缺失依赖（可能需要网络，若完全离线且缺依赖会失败）
            apt-get install -f -y 2>/dev/null || warn "离线安装可能有依赖未满足，请检查报错"
        else
            error "离线模式但未找到 openjdk-21-*.deb 包，无法安装"
            exit 1
        fi
    else
        # 在线：配置阿里云镜像 + apt 安装
        info "配置 APT 阿里云镜像..."
        if [ ! -f /etc/apt/sources.list.d/debian.sources.bak ]; then
            cp /etc/apt/sources.list.d/debian.sources /etc/apt/sources.list.d/debian.sources.bak 2>/dev/null || true
        fi
        # 检测是否已配置阿里云
        if ! grep -q "mirrors.aliyun.com" /etc/apt/sources.list.d/debian.sources 2>/dev/null; then
            cat > /etc/apt/sources.list.d/debian.sources << 'EOF'
Types: deb
URIs: https://mirrors.aliyun.com/debian
Suites: trixie trixie-updates
Components: main
Signed-By: /usr/share/keyrings/debian-archive-keyring.pgp

Types: deb
URIs: https://mirrors.aliyun.com/debian-security
Suites: trixie-security
Components: main
Signed-By: /usr/share/keyrings/debian-archive-keyring.pgp
EOF
            info "APT 镜像已配置为阿里云"
        fi
        apt-get update -qq
        info "apt 安装 openjdk-21-jdk-headless..."
        apt-get install -y openjdk-21-jdk-headless
    fi

    # 验证
    if "$JAVA_HOME_PATH/bin/java" -version 2>&1 | grep -q "21\."; then
        info "JDK 21 安装成功"
        "$JAVA_HOME_PATH/bin/java" -version
    else
        error "JDK 21 安装失败"
        exit 1
    fi
}

###############################################################################
# 第2步：安装 Android command-line tools
###############################################################################
install_cmdline_tools() {
    local TOOLS_DIR="$ANDROID_SDK_DIR/cmdline-tools/latest"
    if [ -f "$TOOLS_DIR/bin/sdkmanager" ]; then
        info "Android cmdline-tools 已安装，跳过"
        return 0
    fi
    info "开始安装 Android command-line tools..."

    mkdir -p "$ANDROID_SDK_DIR/cmdline-tools"
    local ZIP_FILE="$SCRIPT_DIR/cmdline-tools.zip"

    if [ -f "$ZIP_FILE" ]; then
        info "使用本地压缩包：$ZIP_FILE"
    else
        if [ "$OFFLINE" = true ]; then
            error "离线模式但未找到 cmdline-tools.zip，无法安装"
            exit 1
        fi
        info "本地无压缩包，在线下载..."
        curl -sL -o /tmp/cmdline-tools-download.zip \
            "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
        ZIP_FILE="/tmp/cmdline-tools-download.zip"
    fi

    # 解压（logback 的 zip 解压后顶层是 cmdline-tools 目录）
    unzip -q "$ZIP_FILE" -d "$ANDROID_SDK_DIR"
    if [ -d "$ANDROID_SDK_DIR/cmdline-tools" ] && [ ! -d "$ANDROID_SDK_DIR/cmdline-tools/latest" ]; then
        # 把解压出的 cmdline-tools 重命名为 latest
        mv "$ANDROID_SDK_DIR/cmdline-tools" "$ANDROID_SDK_DIR/cmdline-tools-tmp"
        mkdir -p "$ANDROID_SDK_DIR/cmdline-tools/latest"
        mv "$ANDROID_SDK_DIR/cmdline-tools-tmp"/* "$ANDROID_SDK_DIR/cmdline-tools/latest/"
        rm -rf "$ANDROID_SDK_DIR/cmdline-tools-tmp"
    fi

    if [ -f "$TOOLS_DIR/bin/sdkmanager" ]; then
        info "Android cmdline-tools 安装成功"
    else
        error "Android cmdline-tools 安装失败"
        exit 1
    fi
}

###############################################################################
# 第3步：用 sdkmanager 安装 SDK 组件
###############################################################################
install_sdk_components() {
    local TOOLS_BIN="$ANDROID_SDK_DIR/cmdline-tools/latest/bin/sdkmanager"
    export JAVA_HOME="$JAVA_HOME_PATH"
    export PATH="$JAVA_HOME/bin:$PATH"

    local NEED_INSTALL=false
    [ ! -d "$ANDROID_SDK_DIR/platforms/android-36" ] && NEED_INSTALL=true
    [ ! -d "$ANDROID_SDK_DIR/build-tools/36.0.0" ] && NEED_INSTALL=true
    [ ! -f "$ANDROID_SDK_DIR/platform-tools/adb" ] && NEED_INSTALL=true

    if [ "$NEED_INSTALL" = false ]; then
        info "Android SDK 组件已安装，跳过"
        return 0
    fi

    info "接受 SDK licenses..."
    yes | "$TOOLS_BIN" --licenses > /dev/null 2>&1 || true

    if [ "$OFFLINE" = true ]; then
        error "离线模式无法用 sdkmanager 安装 SDK 组件（需要在线下载）"
        error "请使用在线模式运行，或手动将已安装的 SDK 拷贝到 $ANDROID_SDK_DIR"
        exit 1
    fi

    info "安装 platform-tools + platforms;android-36 + build-tools;36.0.0..."
    "$TOOLS_BIN" "platform-tools" "platforms;android-36" "build-tools;36.0.0"

    info "Android SDK 组件安装完成"
    info "  platforms: $(ls $ANDROID_SDK_DIR/platforms/)"
    info "  build-tools: $(ls $ANDROID_SDK_DIR/build-tools/)"
    info "  platform-tools: $(ls $ANDROID_SDK_DIR/platform-tools/ | head -1)..."
}

###############################################################################
# 第4步：写入环境变量
###############################################################################
write_env() {
    info "写入环境变量到 $PROFILE_FILE..."
    cat > "$PROFILE_FILE" << EOF
# TTS Server Android 构建环境
export JAVA_HOME=$JAVA_HOME_PATH
export ANDROID_HOME=$ANDROID_SDK_DIR
export PATH=\$JAVA_HOME/bin:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$PATH
EOF
    chmod +x "$PROFILE_FILE"
    source "$PROFILE_FILE" 2>/dev/null || true
    info "环境变量已写入，新终端会自动加载。当前终端请执行: source $PROFILE_FILE"
}

###############################################################################
# 主流程
###############################################################################
echo "========================================"
echo " TTS Server Android 构建环境安装"
echo " 模式: $([ "$OFFLINE" = true ] && echo "离线" || echo "在线")"
echo "========================================"

install_jdk
install_cmdline_tools
install_sdk_components
write_env

echo ""
echo "========================================"
info "全部安装完成！"
echo "========================================"
echo ""
echo "环境变量："
echo "  JAVA_HOME=$JAVA_HOME_PATH"
echo "  ANDROID_HOME=$ANDROID_SDK_DIR"
echo ""
echo "验证："
echo "  java -version"
echo "  sdkmanager --version"
echo ""
echo "打包 APK："
echo "  source $PROFILE_FILE"
echo "  cd /workspace && ./gradlew assembleAppRelease --no-daemon -x lint"
