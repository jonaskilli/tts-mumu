# 构建与打包说明（BUILD）

本文说明 TTS Server Android 项目从源码打包成 APK 所需的依赖与打包方法。

版本相关常量定义在 `app/build.gradle`：

```gradle
def version = "1.26.062507"   // versionName，决定产物文件名
def gitCommits = 1509         // versionCode
```

产物文件名规则（见 `app/build.gradle`）：`TTS-Server-v${versionName}.apk`，即 `TTS-Server-v1.26.062507.apk`。

---

## 一、环境依赖

### 1. JDK
- **JDK 21**。主模块 `app` 使用 `kotlin { jvmToolchain(21) }`（见 `app/build.gradle:154`）。
- Android Gradle Plugin 8.8.1 要求编译环境至少为 **JDK 17**，推荐使用 21。
- 各子模块 `sourceCompatibility` 要求不同（1.8 / 11 / 17），统一使用 JDK 21 可全部满足。

### 2. Android SDK
在 `gradle/libs.versions.toml` 中定义：

| 项目 | 值 |
|------|-----|
| compileSdk | 36 |
| minSdk     | 21 |
| targetSdk  | 35 |

需要安装：
- **Android SDK Platform 36**（含编译所需平台）
- **Android SDK Build-Tools**（任意较新版本，如 34+）
- **Android SDK Platform-Tools**（含 `adb`、`apksigner`）

SDK 路径通过 `local.properties` 的 `sdk.dir` 指定，例如 `sdk.dir=/root/Android/Sdk`。

### 3. Gradle
- **Gradle 8.10.2**，由项目 `gradle/wrapper/gradle-wrapper.properties` 指定，通过仓库提供的 `gradlew` / `gradlew.bat` 自动下载，**无需手动安装**。
- 镜像已配置为腾讯云：`https://mirrors.cloud.tencent.com/gradle/gradle-8.10.2-bin.zip`。

### 4. 其余系统依赖
- **无需 NDK / CMake**（无 `externalNativeBuild` 或 `ndkVersion` 配置）。
- **无需额外系统库**。Gradle 内存要求在 `gradle.properties` 中已设为 `-Xmx4096m`，请确保机器可用内存 ≥ 4GB。

### 5. 第三方库依赖
依赖版本集中在 `gradle/libs.versions.toml`，关键项：

| 类别 | 版本 |
|------|------|
| Kotlin | 2.1.10 |
| Android Gradle Plugin | 8.8.1 |
| KSP | 2.1.10-1.0.29 |
| Compose BOM | 2025.02.00 |
| Room | 2.6.1 |
| OkHttp | 4.12.0 |
| ktor-server | 3.0.3 |
| about-libraries | 10.9.2 |

仓库镜像（阿里云）已在 `gradle.properties` 的 `pluginManagement` / `dependencyResolutionManagement` 中配置好，国内环境可直接联网拉取。

---

## 二、签名配置

打包 release 需要签名。配置从项目根目录的 `local.properties` 读取（见 `app/build.gradle:69` 的 `signingConfigs.release`）：

```properties
KEY_PATH=/workspace/release.jks      # 签名文件绝对路径
KEY_PASSWORD=Ktouls123456            # keystore 密码
ALIAS_NAME=TTSServer                 # 别名
ALIAS_PASSWORD=Ktouls123456          # 别名密码
```

- 仓库已自带 `release.jks`（别名 `TTSServer`，证书 `CN=Modder Hub`），可直接用于打包。
- 若 `local.properties` 不存在或未配置 `KEY_PATH`，构建将**自动回退到 debug 签名**（`~/.android/debug.keystore`），此时产物为 debug 签名，无法作为正式发布版本。

---

## 三、打包方法

### 方式 A：命令行（推荐）

在项目根目录执行：

```bash
# 正式版（release，带 R8 混淆 + 资源压缩）
./gradlew assembleAppRelease

# 调试版（debug）
./gradlew assembleAppDebug
```

> 说明：任务名 `assembleAppRelease` 由 productFlavor `app`（dimension `version`）+ buildType `release` 组成。另有 `dev` flavor（`assembleDevRelease` / `assembleDevDebug`）。

如果 Gradle daemon 占用导致终端输出卡住，可加 `--no-daemon`：

```bash
./gradlew assembleAppRelease --no-daemon
```

构建完成后产物位于：

```
app/build/outputs/apk/app/release/TTS-Server-v1.26.062507.apk
```

### 方式 B：后台 / 无人值守构建

如需长时间后台构建（release 含 R8 混淆较耗时，约 3~5 分钟），可用 detached 方式启动并将日志写入文件：

```bash
setsid bash -c './gradlew assembleAppRelease --no-daemon > _build_rel_log.txt 2>&1' & disown
# 轮询进度
tail -f _build_rel_log.txt
```

### 方式 C：Android Studio

1. 打开项目后，在根目录 `local.properties` 写入上面的签名四项配置。
2. 菜单 **Build → Generate Signed Bundle / APK → APK**，选择 `release.jks` 并填入别名/密码。
3. 选择 `appRelease` 变体，等待 `assembleAppRelease` 完成。

### 方式 D：GitHub Actions（CI 自动打包）

通过仓库 Secrets 注入签名：
- `ALIAS_NAME`、`ALIAS_PASSWORD`、`KEY_PASSWORD`、`KEY_STORE`（签名文件 Base64 内容）
- 对签名文件无换行 Base64 编码：`openssl base64 < key.jks | tr -d '\r\n'`

详见 `release.yml` 工作流与上游博客说明。

---

## 四、验证产物

使用 SDK `build-tools` 中的 `apksigner` 校验签名：

```bash
APKS=$(find $ANDROID_HOME/build-tools -name apksigner -type f | head -1)
"$APKS" verify --print-certs \
  app/build/outputs/apk/app/release/TTS-Server-v1.26.062507.apk
```

`BUILD SUCCESSFUL` 且 `apksigner verify` 通过即表示正式版打包成功、签名有效。

---

## 五、注意事项

- **不要提交构建中间产物**：`app/build/`、`lib-*/build/`、`.gradle/` 下的文件均为构建生成，已超出源码范围，提交时忽略。
- `local.properties` 含签名密码，属于敏感文件，请勿提交到公开仓库。
- release 开启 `minifyEnabled=true` + `shrinkResources=true`（R8 全量混淆），若新增通过反射/动态加载的类，需在 `app/proguard-rules.pro` 中保留，否则可能被误删。
- 版本号变更请同时修改 `app/build.gradle` 中的 `version` 与 `gitCommits`（versionCode）。
