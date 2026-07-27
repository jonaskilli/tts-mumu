# Android开发环境 - 基于Ubuntu
FROM ubuntu:22.04

# 避免交互式提示
ENV DEBIAN_FRONTEND=noninteractive

# 配置阿里云APT镜像源加速
RUN sed -i 's|http://archive.ubuntu.com|https://mirrors.aliyun.com|g' /etc/apt/sources.list && \
    sed -i 's|http://security.ubuntu.com|https://mirrors.aliyun.com|g' /etc/apt/sources.list

# 安装基础工具
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    git \
    unzip \
    zip \
    openssh-server \
    openjdk-17-jdk \
    && rm -rf /var/lib/apt/lists/*

# 设置JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

# 下载并安装Android命令行工具
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/34.0.0

RUN mkdir -p $ANDROID_HOME/cmdline-tools

# 使用清华大学镜像加速下载Android SDK命令行工具
RUN wget https://mirrors.tuna.tsinghua.edu.cn/android/android-repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip /tmp/cmdline-tools.zip -d $ANDROID_HOME/cmdline-tools && \
    mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# 接受Android SDK许可
RUN yes | sdkmanager --licenses || true

# 配置Android SDK镜像加速（腾讯云镜像）
RUN mkdir -p $ANDROID_HOME/.android && \
    echo "### User Sources for Android SDK Manager" > $ANDROID_HOME/.android/repositories.cfg && \
    echo "count=0" >> $ANDROID_HOME/.android/repositories.cfg

# 使用国内镜像安装Android SDK组件
RUN sdkmanager \
    --proxy=http \
    --proxy_host=mirrors.neusoft.edu.cn \
    --proxy_port=80 \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;34.0.0" || true

# 如果镜像失败，使用官方源重试
RUN sdkmanager \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;34.0.0" || true

# 安装code-server和常用插件
RUN curl -fsSL https://code-server.dev/install.sh | sh \
  && code-server --install-extension cnbcool.cnb-welcome \
  && code-server --install-extension redhat.vscode-yaml \
  && code-server --install-extension dbaeumer.vscode-eslint \
  && code-server --install-extension mhutchie.git-graph \
  && code-server --install-extension donjayamanne.githistory \
  && echo done

# 指定字符集支持中文
ENV LANG C.UTF-8
ENV LANGUAGE C.UTF-8

WORKDIR /workspace
