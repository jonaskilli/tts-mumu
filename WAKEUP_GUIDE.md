# TTS Server Android 唤醒指南

本文档记录项目核心配置、常见问题修复和构建流程，用于快速唤醒开发环境并修复问题。

---

## 🔴🔴🔴 致命配置警告（最高优先级）🔴🔴🔴

### ❌ 绝对禁止删除的配置项

**`gradle.properties` 中的以下配置是应用正常运行的生命线，删除会导致Release版本功能崩溃：**

```properties
# ========== 混淆安全区 - 严禁删除或修改 ==========
android.enableR8.fullMode=false
android.nonTransitiveRClass=true
android.nonFinalResIds=false
kotlin.incremental=true
kotlin.caching.enabled=true
# ================================================
```

**⚠️ 后果：**
- `android.enableR8.fullMode=false` 被删除 → **R8全模式启用** → 混淆过度激进 → **代码编辑器无法打开/无响应**
- `android.nonFinalResIds=false` 被删除 → 资源ID可能被错误优化
- `kotlin.incremental/caching` 被删除 → 编译速度下降

**✅ 安全做法：**
- 保持 `gradle.properties` 中的上述配置完整
- 如需国内镜像，在 `settings.gradle` 中配置，**不要**在 `gradle.properties` 中添加无效的仓库配置

**📋 基准文件：** `Search` 分支20小时前的版本是已验证的安全配置

---

## 📋 项目关键配置

### 签名配置
**文件位置：** `/workspace/local.properties`

```properties
KEYSTORE_FILE=release.jks
KEY_ALIAS=TTSServer
KEY_PASSWORD=Ktouls123456
STORE_PASSWORD=Ktouls123456
```

**签名文件：** `/workspace/release.jks`

### 构建配置文件
- `.cnb.yml` - CNB云原生配置（8核CPU）
- `gradle.properties.cnb` - 云环境Gradle配置（国内镜像）
- `.ide/Dockerfile` - Docker容器配置（包含国内镜像）
- `CNB_SETUP.md` - 详细云环境搭建文档

---

## 🐛 已知问题及修复方法

### 问题9: 保活设置界面闪退 (2026-02-04)

**症状：** 点击"保活设置"进入界面后立即闪退

**根本原因：** `KeepAliveSettingsActivity` 直接在 `remember { }` 中委托访问 `SystemTtsConfig` 的状态属性，而该配置依赖 `app` 对象初始化，在某些情况下可能导致空指针异常

**修复文件：**
- `app/src/main/java/com/github/jing332/tts_server_android/compose/settings/KeepAliveSettingsActivity.kt`

**修复要点：**
1. 使用本地 `mutableStateOf()` 替代直接委托给 `SystemTtsConfig`
2. 在 `LaunchedEffect` 中安全加载配置值（使用 `runCatching` 包装）
3. 在开关回调中使用 `runCatching` 保存配置值

```kotlin
// 修复前 - 直接委托，可能导致崩溃
var isKeepAliveEnabled by remember { SystemTtsConfig.isKeepAliveEnabled }

// 修复后 - 本地状态 + 安全加载/保存
var isKeepAliveEnabled by remember { mutableStateOf(false) }
LaunchedEffect(Unit) {
    runCatching {
        isKeepAliveEnabled = SystemTtsConfig.isKeepAliveEnabled.value
    }
}
SwitchPreference(
    checked = isKeepAliveEnabled,
    onCheckedChange = { enabled ->
        isKeepAliveEnabled = enabled
        runCatching { SystemTtsConfig.isKeepAliveEnabled.value = enabled }
        // ... 其他逻辑
    }
)
```

---

### 问题1: TTS插件语言和声音列表不刷新

**症状：** 编辑插件并保存预览后，语言和声音列表不更新

**修复文件：**
- `app/src/main/java/com/github/jing332/tts_server_android/compose/systts/list/ui/PluginEditorViewModel.kt`
- `app/src/main/java/com/github/jing332/tts_server_android/plugin/TtsPluginEngineManager.kt`

**修复要点：**
1. `TtsPluginEngineManager.get()` 方法添加代码对比检测变化
2. `PluginEditorViewModel.updateCode()` 清除缓存并重建引擎
3. `PluginEditorViewModel.updateSource()` 强制重新加载数据

```kotlin
// TtsPluginEngineManager.kt - 添加代码对比
fun get(context: Context, plugin: Plugin): TtsPluginUiEngineV2 {
    val cachedEngine = cache.get(plugin.pluginId)
    if (cachedEngine != null && cachedEngine.plugin.code == plugin.code) {
        return cachedEngine
    }
    val engine = TtsPluginUiEngineV2(context, plugin)
    engine.eval()
    cache.put(plugin.pluginId, engine)
    return engine
}
```

---

### 问题3: TTS插件声音风格选项初始不显示

**症状：** 插件加载后，语言和声音能正常显示，但声音的风格选项等自定义UI不显示，需要手动切换声音才会出现

**修复文件：**
- `app/src/main/java/com/github/jing332/tts_server_android/compose/systts/list/ui/PluginTtsViewModel.kt`

**修复要点：**
1. `load()` 方法在加载声音列表后，如果已选择声音则调用 `updateCustomUI()`
2. 确保 `onVoiceChanged()` 被触发以加载风格选项等自定义UI

```kotlin
// PluginTtsViewModel.kt - 初始加载时触发 onVoiceChanged
suspend fun load(
    context: Context,
    plugin: Plugin?,
    source: PluginTtsSource,
    linearLayout: LinearLayout,
) =
    withIO {
        withMain { isLoading = true }
        try {
            initEngine(plugin, source)
            engine.onLoadData()

            withMain {
                linearLayout.removeAllViews()
                engine.onLoadUI(context, linearLayout)
            }

            updateLocales()
            updateVoices(source.locale)

            // 初始加载时如果已选择声音，触发 onVoiceChanged 以加载风格选项等自定义UI
            if (source.voice.isNotBlank() && source.locale.isNotBlank()) {
                updateCustomUI(source.locale, source.voice)
            }
        } catch (t: Throwable) {
            throw t
        } finally {
            withMain { isLoading = false }
        }
    }
```

---

### 问题2: 转发器日志不显示在UI中

**症状：** 转发器服务运行正常，但UI中看不到日志输出

**修复文件：**
- `app/src/main/java/com/github/jing332/tts_server_android/service/forwarder/SystemTtsForwardServer.kt`
- `app/src/main/java/com/github/jing332/tts_server_android/service/forwarder/SysTtsForwarderService.kt`

**修复要点：**
1. 将 `Log.d/i/e()` 替换为 `callback.log()` 
2. 添加 `sendLog()` 调用将日志发送到UI

```kotlin
// 使用 callback.log() 而不是 Log.d()
callback.log(message)
```

---

### 问题4: 音频参数试听不生效 (2026-01-29)

**症状：** 在底部调节面板修改语速/音量/音高后，点击试听仍然是默认参数

**根本原因：** `PluginTtsProvider` 使用 `source.speed`（旧字段）而非 `params.speed`（新 audioParams）

**修复文件：**
- `lib-tts/src/main/java/com/github/jing332/tts/speech/plugin/PluginTtsProvider.kt`

**修复要点：**
```kotlin
// 修改前：使用 source.speed（过时）
val speed = if (source.speed == 0f) params.speed else source.speed

// 修改后：直接使用 params（已包含 audioParams）
val speed = if (params.speed != 1f) params.speed else if (source.speed == 0f) 1f else source.speed
```

**相关迁移：** `SystemTtsMigration.kt` - 旧数据自动迁移到 audioParams

---

### 问题5: 日志系统增强 - 插件/朗读规则调试 (2026-01-29)

**需求：** 开发者需要查看插件和朗读规则的调试日志，普通用户不需要

**实现方案：**

1. **LogEntry 扩展** - 添加标记字段
```kotlin
data class LogEntry(
    // ...
    val isPluginLog: Boolean = false,
    val isSpeechRuleLog: Boolean = false
)
```

2. **Console 日志来源区分**
```kotlin
class Console(val source: LogSource = LogSource.PLUGIN) {
    enum class LogSource { PLUGIN, SPEECH_RULE }
    
    companion object {
        var globalPluginLogListener: ((LogEntry) -> Unit)? = null
        var globalSpeechRuleLogListener: ((LogEntry) -> Unit)? = null
    }
}
```

3. **UI 添加调试开关** - `LogFilterDialog` 新增两个选项
   - 显示插件日志
   - 显示朗读规则日志

**关键技巧：** 使用全局监听器模式解决模块依赖问题（lib-script → app）

---

### 问题6: 首页重启键改进 - 完全重启应用 (2026-02-04)

**问题：**
1. 原重启键只重启 TTS 服务，不重启转发器服务
2. 重启后转发器状态显示不正确（图标显示关闭但实际运行中）
3. 日志出现 "Array has more than one element" 错误

**解决方案：**
- 新增 `restartApp()` 方法，完全重启应用进程
- 使用 `AlarmManager` 延迟启动 MainActivity
- 调用 `Process.killProcess()` 结束当前进程

**与 `restartService()` 的区别：**
| 特性 | restartService | restartApp |
|------|----------------|------------|
| TTS服务 | 重启 | 重启 |
| 转发器服务 | 保持原状态 | 重启 |
| 应用进程 | 保持 | 重新创建 |
| 内存状态 | 保留 | 清空 |

**相关文件：**
- `app/src/main/java/com/github/jing332/tts_server_android/service/systts/SystemTtsService.kt`
- `app/src/main/java/com/github/jing332/tts_server_android/compose/systts/list/ListManagerScreen.kt`

---

### 问题7: 首页搜索功能增强 (2026-02-04)

**问题：** 首页搜索键的选项无效，只能按名称搜索

**修复方案：**
- 新增 `SearchType` 枚举：NAME, TAG, PLUGIN, GROUP
- `GroupSearchType` 枚举（编辑分组使用）：NAME, TAG, PLUGIN
- 修改 `ListManagerViewModel` 支持按类型过滤

**搜索类型说明：**
- **名称**：按配置名称搜索
- **标签**：按标签名称、标签值搜索  
- **插件**：按插件ID或插件名称搜索
- **分组**：按分组名称搜索（首页特有）

**相关文件：**
- `app/src/main/java/com/github/jing332/tts_server_android/compose/systts/list/SearchTextField.kt`（新建）
- `app/src/main/java/com/github/jing332/tts_server_android/compose/systts/list/ListManagerScreen.kt`
- `app/src/main/java/com/github/jing332/tts_server_android/compose/systts/list/ListManagerViewModel.kt`

---

### 问题7: 日志搜索框样式优化 (2026-01-29)

**需求：** 搜索框字体大小统一 + 美观的圆角透明样式

**修复文件：** `TtsLogScreen.kt`

**实现：**
```kotlin
OutlinedTextField(
    // ...
    textStyle = MaterialTheme.typography.bodyLarge,  // 统一字体
    placeholder = { Text("搜索日志", style = MaterialTheme.typography.bodyLarge) },
    colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,    // 透明背景
        unfocusedContainerColor = Color.Transparent
    )
)
```

---

### 问题8: 日志上限自动清空 (2026-01-31)

**需求：** 日志无单条限制，但满50万条自动清空防止内存溢出

**修复文件：** `TtsLogViewModel.kt`

**实现：**
```kotlin
companion object {
    // 日志总上限，达到后自动清空
    const val MAX_LOGS_BEFORE_CLEAR = 500000
}

fun addLog(entry: LogEntry) {
    runOnUI {
        // 达到上限时自动清空日志
        if (logs.size >= MAX_LOGS_BEFORE_CLEAR) {
            logs.clear()
            logs.add(LogEntry(
                level = LogLevel.WARN,
                message = "日志达到上限，已自动清空"
            ))
        }
        logs.add(entry)
    }
}
```

**特点：**
- 日志持续追加，无单条类型限制
- 达到50万条时自动清空并提示
- 用户仍可手动清空

---

## 🔨 构建流程

### 本地构建
```bash
# 构建正式版
./gradlew assembleAppRelease

# 构建开发版
./gradlew assembleDevRelease

# 同时构建两个版本
./gradlew assembleAppRelease assembleDevRelease --build-cache --parallel
```

### 从零配置构建环境（Linux）

#### 1. 安装 JDK 17
```bash
apt-get update && apt-get install -y openjdk-17-jdk wget unzip
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

#### 2. 安装 Android SDK
```bash
mkdir -p /opt/android-sdk && cd /opt/android-sdk
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mkdir -p cmdline-tools/latest
mv cmdline-tools/* cmdline-tools/latest/ 2>/dev/null || true
rm commandlinetools-linux-11076708_latest.zip

export ANDROID_SDK_ROOT=/opt/android-sdk
export PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools
```

#### 3. 接受许可并安装组件
```bash
yes | sdkmanager --licenses
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

#### 4. 配置签名（local.properties）
```bash
cat > /workspace/local.properties << 'EOF'
KEY_PATH=/workspace/release.jks
KEY_PASSWORD=Ktouls123456
ALIAS_NAME=TTSServer
ALIAS_PASSWORD=Ktouls123456
EOF
```

#### 5. 执行构建
```bash
cd /workspace
./gradlew clean app:assembleAppRelease app:assembleDevRelease --no-daemon
```

### 云环境构建（CNB）

#### 1. 环境准备
```bash
# 检查Java环境
java -version  # 需要 OpenJDK 17+

# 检查Android SDK
ls $ANDROID_HOME/cmdline-tools/latest/bin/

# 接受SDK许可（首次需要）
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
```

#### 2. 安装必要组件（如果缺失）
```bash
# 安装平台工具、平台、构建工具
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager \
  "platform-tools" \
  "platforms;android-35" \
  "build-tools;35.0.0"
```

#### 3. 配置国内镜像（云环境）
```bash
# 复制云环境配置
cp gradle.properties.cnb gradle.properties

# 使用阿里云Maven镜像加速
```

#### 4. 执行构建
```bash
# 清理并构建两个版本
./gradlew clean app:assembleAppRelease app:assembleDevRelease --no-daemon
```

---

## 📂 构建产物位置

**正式版：** `/workspace/app/build/outputs/apk/app/release/TTS-Server-vX.X.XXXXX.apk`

**开发版：** `/workspace/app/build/outputs/apk/dev/release/TTS-Server-vX.X.XXXXX.apk`

---

## 🚀 发布流程

### 1. 创建发布文件夹
```bash
mkdir -p /workspace/newapk
```

### 2. 复制并重命名APK
```bash
# 复制正式版
cp /workspace/app/build/outputs/apk/app/release/*.apk \
   /workspace/newapk/TTS-Server-vX.X.XXXXX-release.apk

# 复制开发版
cp /workspace/app/build/outputs/apk/dev/release/*.apk \
   /workspace/newapk/TTS-Server-vX.X.XXXXX-dev.apk
```

### 3. 提交到Git
```bash
git add newapk/
git commit -m "构建并发布 vX.X.XXXXX 版本

- 新增正式版APK
- 新增开发版APK"
git push origin master
```

---

## ⚙️ 关键依赖版本

- **Gradle:** 8.10.2
- **Kotlin:** 2.1.10
- **Android SDK:** API 35
- **Build Tools:** 35.0.0
- **Java:** OpenJDK 17
- **Compile SDK:** 35
- **Min SDK:** 21
- **Target SDK:** 35

---

## 🔧 云环境优化

### 国内镜像配置
1. **APT镜像** - 阿里云Ubuntu镜像
2. **Android SDK镜像** - 清华大学TUNA镜像
3. **Maven/Gradle镜像** - 阿里云Maven仓库

### Gradle优化参数
- `--build-cache` - 启用构建缓存
- `--parallel` - 并行编译
- `--daemon` - 守护进程模式

---

## 💡 快速唤醒命令

```bash
# 检查环境
echo "Java: $(java -version 2>&1 | head -1)"
echo "Android SDK: $ANDROID_HOME"
echo "Gradle: $(./gradlew --version | grep Gradle)"

# 快速构建（正式版）
./gradlew clean assembleAppRelease --build-cache --parallel

# 快速构建（两个版本）
./gradlew clean assembleAppRelease assembleDevRelease --build-cache --parallel --daemon

# 构建并发布（示例）
./gradlew clean assembleAppRelease assembleDevRelease --build-cache --parallel
mkdir -p newapk
cp app/build/outputs/apk/app/release/*.apk newapk/TTS-Server-release.apk
cp app/build/outputs/apk/dev/release/*.apk newapk/TTS-Server-dev.apk
git add newapk/ && git commit -m "发布新版本" && git push origin master
```

---

## 📞 唤醒AI助手时请提供

1. **问题描述** - 具体的bug或需求
2. **日志信息** - 相关的错误日志或构建日志
3. **当前环境** - 本地/云环境，Java/Gradle版本
4. **期望结果** - 想要达成的目标

**示例唤醒词：**
> "修复TTS插件刷新问题，然后构建正式版和开发版APK并发布到远程仓库"

---

## ⚠️ 关键注意事项 (2026-01-30 更新)

### 🚨🚨🚨 ProGuard/R8 混淆规则 - 生死线配置 🚨🚨🚨

> **警告级别：致命**
> 
> **错误配置后果：Release构建的应用会在运行时崩溃，编辑器无法打开，用户数据可能丢失**

#### 双保险配置（缺一不可）

**第1层保险 - `gradle.properties`（R8开关）：**
```properties
# 这行配置必须存在！删除会导致R8全模式启用，混淆过度激进
android.enableR8.fullMode=false
```

**第2层保险 - `app/proguard-rules.pro`（保留规则）：**
```proguard
# 代码编辑器必须保留（R8会误删这些类）
-keep class io.github.rosemoe.sora.** { *; }
-keepclassmembers class io.github.rosemoe.sora.** { *; }

# 插件引擎必须保留
-keep class com.github.jing332.tts_server_android.plugin.** { *; }
-keepclassmembers class com.github.jing332.tts_server_android.plugin.** { *; }
```

#### ❌ 绝对禁止的操作

| 禁止操作 | 后果 |
|---------|------|
| 删除 `android.enableR8.fullMode=false` | 编辑器崩溃、无法保存 |
| 使用master分支的精简版proguard-rules.pro | 插件系统失效 |
| 在gradle.properties添加`pluginManagement` | 配置无效，且可能覆盖关键配置 |
| 合并分支时覆盖Search分支的proguard-rules.pro | 功能崩溃 |

#### ✅ 安全操作流程

1. **构建前检查** `gradle.properties` 必须包含 `android.enableR8.fullMode=false`
2. **构建前检查** `app/proguard-rules.pro` 文件大小应约16KB（380+行）
3. **Git合并时** 始终保留Search分支的proguard-rules.pro（选择"ours"策略）
4. **验证构建** Release APK安装后必须测试编辑器能否正常打开和保存

**基准安全版本：** `Search` 分支2026-01-29 20:00前的配置

### 📦 新功能移植记录

**本次移植功能：**
1. **分组编辑增强** - 支持按名称/标签/插件搜索并批量移动配置
2. **日志系统升级** - 支持日志搜索和级别筛选
3. **首页搜索增强** - 支持按名称/标签/插件/分组搜索

**移植原则：**
- 只移植功能代码，不移植 master 的 ProGuard 规则
- 保持 Search 分支的稳定性

---

## 🎯 AI 协作工作模式 (2026-01-29)

### 高效开发流程

以下是在本项目中与 AI 协作的最佳实践，可快速推进功能开发：

#### 1. 并行搜索策略
```
同时发起多个搜索请求：
- 搜索文件路径/类名
- 搜索关键函数/变量
- 搜索相关配置

避免串行等待，最大化信息获取效率
```

#### 2. 问题定位三板斧
```
1. 复现问题 → 明确症状
2. 全局搜索 → 找到相关代码
3. 并行读取 → 理解调用链

示例：修复音频参数不生效
- 搜索：PluginTtsProvider、audioParams、speed
- 读取：Provider 实现、数据迁移、UI 绑定
- 定位：试听时使用 source.speed 而非 params.speed
```

#### 3. 模块化解耦技巧
```
问题：lib-script 模块无法直接访问 app 模块的日志系统

解决方案：全局监听器模式
// lib-script 定义全局回调
object Console {
    var globalPluginLogListener: ((LogEntry) -> Unit)? = null
}

// app 模块注册接收
Console.globalPluginLogListener = { logEntry ->
    // 处理日志
}
```

#### 4. 快速构建命令
```bash
# 编译检查（不打包）
./gradlew :app:compileAppReleaseKotlin --no-daemon

# 完整构建正式版+开发版
./gradlew :app:assembleRelease --no-daemon

# 清理终端后构建（解决 daemon 崩溃）
pkill -f gradlew; sleep 2
./gradlew :app:assembleRelease --no-daemon
```

---

## 🎙️ 文心一言TTS插件制作记录

### 制作时间
2026-02-05

### 抓包分析
**工具**: Chrome DevTools + HAR导出
**目标**: 文心一言APP语音合成功能

### 关键发现

#### 1. WebSocket端点
```
wss://tts.baidu.com/ws/sdktts?sn={UUID}
```

#### 2. 请求格式 (TLV)
```
[4字节长度(小端)] [02 01 01 01] [JSON数据]
```

#### 3. 关键JSON字段
```json
{
  "pdt": 10170,
  "key": "com.baidu.newapp",
  "sn": "UUID",
  "tex": "合成文本",
  "per": 4343,        // 音色ID
  "spd": 5,           // 语速 0-10
  "pit": 5,           // 音调 0-10
  "vol": 5,           // 音量 0-10
  "aue": 3,           // 音频格式: 3=MP3
  "rate": 64,         // 比特率
  "platform": "Android"
}
```

#### 4. 响应格式
```
[4字节长度] [00 01 01 50] [JSON元数据] [0A] [MP3音频数据]
```

**注意**: 每个音频块前8字节是自定义头部，需跳过后才是MP3数据

#### 5. 握手结束帧
```
08 00 00 00 00 01 01 5F 00 00 00 00
```
收到此帧表示音频传输完成

### 已知音色ID

| 音色名称 | 音色ID | 采样率 |
|---------|--------|--------|
| 甜美女生 | 4343 | 24000Hz |
| 温柔宁宁 | 4339 | 24000Hz |
| 邻家哥哥 | 4192 | 24000Hz |
| 贴心男大 | 4195 | 48000Hz |
| 清甜少女 | 4196 | 48000Hz |

### 插件特点
- **协议**: WebSocket (wss)
- **音频格式**: MP3 (aue=3)
- **无需登录**: 匿名访问，随机生成设备ID
- **默认语速**: 调快1.2倍（更自然）

---

## 🎙️ 通义千问插件 - 克隆音色抓包教程

### 前置条件
- **工具**：Reqable 或 HttpCanary（小黄鸟）
- **环境**：手机和电脑/抓包设备连接同一 WiFi

### 第一步：配置抓包环境

**Reqable 配置：**
1. 电脑安装 Reqable（https://reqable.com/）
2. 手机 WiFi 设置代理：服务器填电脑IP，端口填 9000
3. 手机浏览器访问 `http://reqable.proxy/ssl` 下载安装证书

**HttpCanary 配置：**
1. 安装 HttpCanary，首次打开时安装证书
2. 设置 → 目标应用 → 选择"通义千问"

### 第二步：抓包获取数据

1. **开启抓包**：Reqable/HttpCanary 点击开始
2. **打开通义千问 APP**
3. **关键步骤**：点击语音按钮，**选择你要抓的克隆音色**（必须是克隆音色，预设音色没有 audio_text/audio_url）
4. **发送任意消息**（如"你好"）
5. **停止抓包**

### 第三步：导出 HAR 文件

- **Reqable**：找到 `speech-tts.qianwen.com` 请求 → 右键 → 导出 → HAR
- **HttpCanary**：长按请求 → 分享 → 导出 HAR

### 第四步：提取三个关键值

**方法：用文本编辑器打开 HAR 文件，搜索以下关键词**

| 搜索关键词 | 提取内容 | 示例 |
|-----------|---------|------|
| `"vcn":"create_voice` | 克隆音色ID | `create_voice_2018904938500661248` |
| `"audio_text":"` | 参考音频文本 | `中国里的小缺幸其实很多呢...` |
| `"audio_url":"` | 参考音频URL | `http://quarklive.oss-cn-zhangjiakou.aliyuncs.com/...` |

**⚠️ 注意事项：**
- `audio_url` 很长，必须复制完整（包含 `?Expires=...` 参数）
- 不要漏掉结尾的 `"`

### 第五步：配置到 TTS Server

**方式一：变量配置（推荐，支持多个音色）**

变量名：`manualCloneVoices`
变量值格式：`音色ID@显示名称@audio_text@audio_url`

示例（多个音色用 `;` 分隔）：
```
create_voice_2018904938500661248@元宝@中国里的小缺幸其实很多呢像吃到一块甜甜的蛋糕，没问题。@http://quarklive.oss-cn-zhangjiakou.aliyuncs.com/xxx.wav;create_voice_2018962051129143296@名称@国人餐桌上必不可少的主食...@http://quarklive.oss-cn-zhangjiakou.aliyuncs.com/yyy.wav
```

**方式二：UI 配置（单个音色）**

1. 选择音色列表中的 **"➕ 添加克隆音色"**
2. 填写三个输入框：
   - **克隆音色ID**：`create_voice_xxx`
   - **参考音频文本**：`audio_text 内容`
   - **参考音频URL**：`audio_url 完整链接`

### 插件内置预设音色

当前插件内置以下预设音色（无需抓包）：

| 音色名称 | 音色ID |
|---------|--------|
| 沐阳 | `zh_female_quarkF531S0_ptts` |
| 若初 | `zh_female_quark_lulu` |
| 苏荷姐姐 | `zh_female_quark_ajiao` |
| 元气草莓 | `zh_female_quark_luoying` |
| 活力嘉蓓 | `zh_female_quark_jiabei` |
| 起司妹妹 | `zh_female_quark_xinshen` |
| 电台华姐 | `zh_female_quark_xiaoning` |
| 彩虹甜豆 | `zh_female_quark_f29` |
| **念念** | `zh_female_quark_xiaoxiao` |
| **方晴师姐** | `zh_female_quark_zheque` |
| 浅吻雾梨 | `longqiang` |
| 午夜甜茶 | `longyan` |
| 皓东 | `zh_male_quark_bb01` |
| 温屿哥哥 | `zh_male_quark_m24` |
| 阿辉 | `zh_male_chengfeng_ICL` |

### 快速检查清单

- [ ] 使用的是克隆音色（不是预设音色）
- [ ] HAR 文件导出成功
- [ ] 提取了完整的三个值（ID、text、URL）
- [ ] URL 包含 `?Expires=` 参数
- [ ] 配置到变量或 UI 后测试发音正常

---

## 📝 修改历史

| 日期 | 版本 | 内容 |
|------|------|------|
| 2026-02-05 | v2.0 | **新增文心一言TTS插件** - 基于抓包实现WebSocket语音合成，支持5种音色，MP3格式输出，无需登录 |
| 2026-02-04 | v1.9 | **修复保活设置闪退** - 使用本地状态管理替代直接委托，添加安全加载/保存机制，解决进入界面闪退问题 |
| 2026-02-04 | v1.8 | **新增克隆音色抓包教程** - 添加通义千问插件配置指南，新增念念、方晴师姐两个预设音色 |
| 2026-02-04 | v1.7 | **修复转发器重启恢复** - 新增 RestartActivity 保存转发器状态，重启后自动恢复 |
| 2026-02-04 | v1.6 | **应用重启功能** - 首页重启键改为完全重启应用，解决转发器状态不同步问题 |
| 2026-02-04 | v1.5 | **搜索功能增强** - 首页搜索支持名称/标签/插件/分组四种类型，编辑分组支持名称/标签/插件三种类型 |
| 2026-01-31 | v1.4 | **更新构建环境** - 添加从零配置构建环境步骤，更新SDK版本为API 35，添加日志上限50万条自动清空功能 |
| 2026-01-30 | v1.3 | **添加致命配置警告** - 强调gradle.properties中R8配置的重要性，避免编辑器崩溃 |
| 2026-01-29 | v1.2 | 添加 AI 协作工作模式，记录音频参数修复和日志系统增强 |
| 2026-01-29 | v1.1 | 添加 ProGuard 规则警告，记录新功能移植注意事项 |
| 2026-01-27 | v1.0 | 初始版本，记录插件刷新、转发器日志、云构建流程 |
