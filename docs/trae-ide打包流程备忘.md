# Trae IDE 本地编译/打包流程备忘

## 环境信息

- 仓库：`E:\TTS\tts-mumu`
- JDK 17：`D:\软件安装\JDK`（通过 junction `D:\JDK` 访问，避免中文路径）
- JDK 21：`E:\TTS\jdk-21.0.12+8`（jvmToolchain(21) 需要）
- Android SDK：`E:\TTS\AndroidSDK`（platform-tools + build-tools;35.0.0 + platforms;android-36）
- Git 认证：Git Credential Manager (GCM) + GitHub OAuth

## 工作约定

- **GitHub 下载**：遇到 GitHub 下载超时（JDK、依赖等），不要自动尝试下载，给用户链接让用户自己下载
- **代理**：不主动设置代理，需要时提醒用户

## 已知问题与解决方案

### 1. JDK 中文路径导致 Kotlin 编译失败

**问题**：`JAVA_HOME=D:\软件安装\JDK`，Kotlin 编译守护进程因中文路径编码出错。

**解决**：创建无中文的 junction 链接：
```powershell
New-Item -ItemType Junction -Path "D:\JDK" -Target "D:\软件安装\JDK" -Force
```
编译时设置：
```powershell
$env:JAVA_HOME = "D:\JDK"
```

### 2. JDK 21 toolchain 配置

**问题**：`app/build.gradle` 中 `jvmToolchain(21)` 需要 JDK 21，本机只有 JDK 17。

**解决**：手动下载 Temurin JDK 21 zip 解压到 `E:\TTS`，然后在 `~/.gradle/gradle.properties` 中指定：
```
org.gradle.java.installations.paths=E:/TTS/jdk-21.0.12+8
```

### 3. Git push 认证

**问题**：push 卡住或认证失败。

**解决**：GCM 设备码模式 + 浏览器授权：
```powershell
git config --global credential.https://github.com.useDevDeviceCode true
git push origin HEAD:mumu_tts_github
```
首次会输出授权链接，浏览器打开授权后即可。授权长期有效，除非手动撤销。

### 4. 本地分支名与远程不一致

**问题**：本地 `experiment/mumu` 跟踪远程 `mumu_tts_github`，`git push` 报错。

**解决**：显式指定远程分支：
```powershell
git push origin HEAD:mumu_tts_github
```

### 5. PowerShell 不支持 heredoc

**问题**：`git commit -m "$(cat <<'EOF' ... EOF)"` 在 PowerShell 中报错。

**解决**：用多个 `-m` 参数：
```powershell
git commit -m "标题" -m "详情行1" -m "详情行2"
```

### 6. Gradle 内存配置（4GB 机器）

`~/.gradle/gradle.properties`（用户级，不影响 CI）：
```
org.gradle.jvmargs=-Xmx1024m -XX:MaxMetaspaceSize=384m -XX:+UseParallelGC -Dfile.encoding=UTF-8
kotlin.daemon.jvmargs=-Xmx512m -Dfile.encoding=UTF-8
org.gradle.daemon=true
org.gradle.parallel=false
org.gradle.caching=true
org.gradle.configureondemand=false
```

## CI 构建配置

### test.yml（自动编译验证）
- 触发：push 到 `mumu_tts_github`（过滤了源码路径，改文档不触发）
- 只跑 `compileAppDebugKotlin`，不打包 APK
- 约 2-3 分钟

### release.yml（手动打包发布）
- 触发：手动
- matrix 构建两个 flavor：`app`（原版）+ `dev`（共存版）
- 产出两个 APK，发布到 GitHub Release
- 约 8-9 分钟

## IDE 构建配置

`.vscode/tasks.json` 提供以下任务：
- `Ctrl+Shift+B`：构建并安装到真机 (app debug)
- 构建共存版 / 只构建 APK / 编译检查 / 查看 adb 设备 / 查看 logcat

`.vscode/launch.json` 提供以下配置：
- 构建安装并启动应用 (app debug / dev 共存版)
- 查看 logcat 日志

## 快速命令速查

```powershell
# 提交并推送
git add -A; git commit -m "msg"; git push origin HEAD:mumu_tts_github

# 查看 CI 状态
# 浏览器打开 https://github.com/jonaskilli/tts-mumu/actions

# 本地编译检查
$env:JAVA_HOME = "D:\JDK"
.\gradlew.bat compileAppDebugKotlin --build-cache --warning-mode all

# 构建安装到真机
.\gradlew.bat installAppDebug
```
