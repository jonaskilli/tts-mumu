# U·TTS Server

> 一个深度定制的 Android TTS 应用，基于 [jing332/tts-server-android](https://github.com/jing332/tts-server-android) 二次开发。

## 来源声明

**本项目并非原创作品**，而是在 [jing332/tts-server-android](https://github.com/jing332/tts-server-android) 基础上二次开发的衍生版本，原名 TTS Server，原作者为 **jing332**。本项目保留其全部版权与署名，并在此致谢。

> 如果你要的是原版，请前往 [上游仓库](https://github.com/jing332/tts-server-android)。

## 这是什么

**U·TTS Server**，即 **Universal TTS Server** —— "U" 取 Universal 之意：把多家在线语音服务、自定义接口与本地引擎聚合成一个系统级入口，接谁的接口都行，供什么 App 调用都行。

Android 平台的系统级 TTS 应用：

- 作为系统 TTS 引擎，供「阅读」(Legado) 等 App 直接调用朗读
- 支持自定义 HTTP 接口，接入任意在线语音合成服务
- 支持导入第三方 TTS 插件与本地 TTS 引擎
- 依据朗读规则把文本分段、打标签，按角色 / 场景匹配不同音色

> 包名为 `com.mumutts.app`，与上游原版、本分支旧版均互不冲突，可同时安装。

## 与上游的主要差异

> 各版本的用户向更新说明见本仓库的 [Releases](https://github.com/jonaskilli/tts-mumu/releases) 发布说明。

- **界面**：全面 Material Design 3 化，多套主题配色，导航栏自绘
- **密钥管理**：按「网址 + 密钥」分组统一管理模型，支持从接口一键拉取模型列表
- **备份与恢复**：应用级备份 / 恢复 + 完整备份中心（覆盖式还原）
- **角色管理**：内置角色列表页，由朗读规则在运行时生成，支持就地换声
- **JRead 兼容**：导入 JRead（墨听）语音配置时，自动映射标签、分池与补绑
- **音频参数**：多层增益 / 语速 / 音调，试听与朗读走同一链路、实时生效
- **朗读日志**：日志行内就地换声、调参、试听，与角色表互通
- **朗读规则**：随包附改造版多角色朗读规则，支持导出 / 导入

## 致谢

除了上游作者 **jing332**，本项目还参考了以下成果：

| 来源 | 贡献 |
|---|---|
| **mingwuyan**（命無言） | 朗读规则与角色管理插件（`ruleId = mingwuyan`）—— 标签体系与角色识别的核心 · [cnb.cool/mingwuyan](https://cnb.cool/mingwuyan/TTS_Server_Android) |
| **Ktouls** | TTS Server 衍生实现（混元版） · [cnb.cool/Ktouls/TTS_Server_Android2](https://cnb.cool/Ktouls/TTS_Server_Android2) |
| **墨听** | [JRead-VoiceEngine](https://github.com/jwoo1982217-eng/JRead-VoiceEngine) —— 语音引擎分支，本项目 JRead 兼容层的参照 |
| **阅读 Sigma** | [Rimchars/legado](https://github.com/Rimchars/legado) —— 本项目响度均衡等设计的参照 |

以上仅为成果归属说明；各组件请遵循其各自仓库的许可。

## AI 协助

本项目的代码实现、问题排查与文档整理过程中，使用了大语言模型作为辅助工具：

| 模型 | 提供方 |
|---|---|
| **GLM-5.2** / **GLM-5.3-Flash** | 智谱 |
| **DeepSeek-V4.1-Flash** | 深度求索 |

AI 参与的是辅助环节（编码、查证、文案整理）；项目的目标、设计取舍与最终验收由本项目维护者负责。

## 许可

本项目以 **GNU General Public License v3.0**（GPL-3.0）分发，全文见 [`LICENSE`](LICENSE)。

- 基于 [jing332/tts-server-android](https://github.com/jing332/tts-server-android)（原作者 jing332，MIT）二次开发，原作者的版权声明一并保留
- 参考了 [Rimchars/legado](https://github.com/Rimchars/legado)（阅读 Sigma，GPL-3.0）与 [JRead-VoiceEngine](https://github.com/jwoo1982217-eng/JRead-VoiceEngine)（墨听，GPL-3.0）的部分实现

分发或再修改时，请同样以 GPL-3.0 开放源代码。
