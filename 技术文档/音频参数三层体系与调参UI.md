# 音频参数三层体系与调参 UI（2026-09-10 定稿）

> 本文是音频参数（语速/音量/音高/混响）的现行架构总纲，覆盖 09-07~09-10 两轮改造的最终结论。
> 旧文档《插件提交判定指导.md》的"人工判定勾选"方法已废止，以本文为准。

## 一、三层模型（唯一口径）

终值 = **配置层 × 插件层 × 全局层**（`resolveTtsPlayback` 的 `multiplyParam`，`FOLLOW(0f)` 视为 1）。

- **接管判定已废除**（dc5dd22，09-10）：不再区分"插件是否自己消费参数"，删除人工已知表与 UI 隐藏链。所有维度**恒显示、恒可调、恒走本机 Sonic/服务端**，调节不生效时以实际听感为准。
- 插件层无来源（系统 TTS 直连）时按 1.0 计。
- 音高也进插件/全局层（09-10 翻掉 09-07"音高不出现在插件/全局层"的旧决定）。
- 28 个语音插件盘点见《插件提交判定指导.md》附录（仅作背景，不作为改动依据）。
- 本机不编译的验证流程、发布规格（1.26.月日时分 tag → release.yml 自动打包仅 app）见仓库 workflow。

## 二、调参 UI 三入口（09-10 定稿）

共用滑杆组件 `AudioParamsDimensionSection`（维度名共享 `audioParamsDimNames`，`LayerSlider` internal，`snap()` 去噪）。

**层滑杆尺寸（09-10 晚定案，三处统一）**：标签 14sp（与维度选择区 `SegmentedTextToggle` 的 labelLarge 14sp 齐平，避免子项压住父项；"最早"是 16sp 会倒挂，已否）、加减 48dp 触摸区/24dp 图标（恢复改造前 M3 默认）、轨道 3.5dp（比最早 4dp 细、比 09-08 的 3dp 粗）、thumb 3.5×22dp。
实现：这 5 项是 `LabelSlider` 的**可选参数**`labelFontSize/buttonSize/iconSize/trackHeight/thumbSize`，由 `LayerSlider` 统一传值；`LabelSlider` 默认值仍是 13sp/32dp/20dp/3dp/3×20dp，故其余 32 处调用方（朗读规则、替换规则等）不受影响——**改尺寸只能走参数，禁止改默认值**。

### 1. 卡片 ⋮ 菜单 →「音频参数」弹窗（`AudioParamsDialog`）
- **顶部**（09-10 晚用户要求恢复，同 8220d5b）：当前发音人 + ▶试听 + 终值行（三维恒显，`audio_params_final`，实时跟随草稿）。▶=草稿试听，念 `AppConfig.testSampleText`，三层草稿经 `pluginParamsOverride`/`globalParamsOverride` 全带入，**不用先应用**即得完整终值效果；▶→…→■，播完复位。
- **主体平铺**（09-10 晚恢复）：维度分段（语速/音量/音高）+ 该维 发音人→插件→全局 三层滑杆同屏 + 重置/应用；脏维度应用键带 ●。
- 排版与日志快捷面板音频参数区**同款**，仅少面板顶部的「更换发音人/音频参数」两区切换（本入口唯一，无换声诉求）。
- 09-10 当天曾试过的**折叠手风琴**（`collapsedAccordion`）已撤销并删除代码，勿再引入。
- 09-10 当天一度裁掉的**顶部发音人/终值行/试听键已恢复**（用户裁定），勿再按"已裁撤"处理。

### 2. 编辑页「三键直出」（`AudioParamsDimChipsRow` + `AudioParamsDimDialog`，新路径）
- 试听文本行（🎧）正下方直接列三个 FilterChip：**语速/音量/音高，键上带该维终值**（配置×插件×全局，实时跟随草稿）。
- 点键开**单维弹窗**：标题即维度名，该维 配置→插件→全局 三层滑杆 + 重置/应用（无插件源自动只有两层）。
- **▶试听键在弹窗左下角**：▶→…(合成中)→■(播放中)，再点停止，播完复位。
- 旧试听行 ⚡ 按钮已删（`AuditionTextField` 只剩 🎧）。

### 3. 日志快捷面板（`LogQuickPanel`）
- 平铺三层（`collapsedAccordion=false`，不受手风琴影响），带发音人跟随/换声逻辑（f26a2b3），本批未动。

## 三、按维度应用语义

「应用」= 该维**三层一起落库**：
1. 配置层写 DB + **双写页面内存**（`onSysttsChange`，防"应用后再保存"被旧内存覆盖）；
2. 插件层写 DB + `PluginDescriptor.invalidatePluginParamsCache`（卡片参数行缓存失效重查）；
3. 全局层写 `SysTtsConfig`；
4. `SystemTtsService.notifyUpdateConfig()` 立即生效，不关弹窗，toast 反馈。

## 四、试听链（`TaggedTtsPreviewPlayer`）

- 静默试听会话，不走 MixSynthesizer（不触发 BGM/朗读规则/朗读队列），但与正式链共用 resolver/provider 路由/解码/响度增益/Sonic 参数。
- **三层草稿覆盖**（994080b，09-10）：`play()` 增加 `pluginParamsOverride`/`globalParamsOverride` 可选参数 → `resolveTtsPlayback` 传 `pluginParamsOverride` 替代 DB 插件层、全局层覆盖替代 `TtsPreviewConfig.globalAudioParamsProvider()`。单维弹窗 ▶ 把该维 配置+插件+全局 三层草稿全传入，**调滑杆→▶听→再调，完整终值效果即时可得，不用先应用**。不传时行为与旧版一致（角色管理 JS、日志面板等调用方不受影响）。
- **混响已接入**：`reverbEnabled` 时编码音频先 `AudioDecoder.doDecode` 解码为 PCM16，过与正式朗读链同一个 `ReverbAudioProcessor`（单声道处理；尾音在输入末尾截断，预览可接受）。混响开关属配置项，`draftEntity()` 保留页面内存值——编辑页切了开关（未保存）▶ 试听立即生效。
- 试听文本同源：所有试听念 `AppConfig.testSampleText`（默认句 = 原句第一句「单击右侧按钮即可测试并播放这段音频。」），清空回落"你好，这是试听语音。"。旧装机存的默认文本不会自动变。

## 五、关键文件

| 文件 | 职责 |
|---|---|
| `lib-tts/.../ResolvedTtsPlayback.kt` | 三层解析（`multiplyParam`/`parameterRoute`），`pluginParamsOverride` |
| `lib-tts/.../TaggedTtsPreviewPlayer.kt` | 试听会话：状态机/草稿覆盖/混响/响度 |
| `app/.../widgets/AudioParamsDialog.kt` | 卡片⋮总弹窗（折叠手风琴） |
| `app/.../widgets/AudioParamsDimDialog.kt` | 三键行 + 单维弹窗（含 ▶） |
| `app/.../widgets/AudioParamsDimensionSection.kt` | 共用三层滑杆区/`snap()`/维度名 |
| `app/.../widgets/AuditionTextField.kt` | 试听文本行（仅 🎧） |
| `app/.../list/ui/PluginTtsUI.kt` / `LocalTtsUI.kt` | 编辑页接线（三键直出） |

## 六、已废止/已否决（勿再提）

- 插件接管判定与「由插件处理」开关（09-10 用户裁定，一律本机处理）。
- 弹窗顶部当前发音人/终值行/⚡入口（逐步裁撤，终值以卡片参数行/日志为准）。
- 音频参数弹窗的折叠手风琴形态（09-10 当日试、当日撤；弹窗与日志面板排版必须同款）。
- M3 质感对齐/配色自动切换/切逗号/bingfa 等历史否决项见工作记忆。

## 版本

- 本篇对应 commit `994080b`，首发 tag `1.26.09100404`（2026-09-10）。
- 09-10 晚 UI 修订三连（均在首发包之后，待推）：
  - `90c71f3`：卡片第二块（音色 id + 最终参数行）字号 bodyMedium 14sp→**13sp**（仍大于下方采样率/格式行 bodySmall 12sp）；配置项音频参数弹窗撤折叠手风琴恢复平铺。
  - `9dbf0e1`：弹窗顶部**恢复**当前发音人 + ▶试听 + 终值行（09-10 一度裁撤，用户要求恢复），▶ 走三层草稿覆盖。
  - `9dcd115`：音频参数三处层滑杆尺寸统一（标签 14sp / 加减 48dp·24dp / 轨道 3.5dp / thumb 3.5×22dp），经 `LabelSlider` 可选参数实现。
