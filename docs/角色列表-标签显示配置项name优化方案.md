# 角色列表：标签显示发音人真实 voice 优化方案

> 目标：让角色列表的发音人标签显示当前绑定 voice 的 displayName（如"晓晓"），voice 切换后标签跟着变；用底层英文 voice 作唯一校验键，删除 voice 后能⚠。
> 关联文档：`docs/角色列表-标签性格机制与自动刷新.md`
> 状态：**方案 A 已实施**（2026-08-04，显示 tag），C 待定（显示真实 voice/displayName）

---

## 一、现状与真实架构

### 1.1 三层数据结构（核对自代码 + 用户确认）

| 层 | 字段 | 含义 | 例子 | 是否变 |
|---|---|---|---|---|
| **tag** | 朗读规则分类键 | `女青年01` | 不变（朗读规则内部） |
| **voice** | 底层英文 TTS voice | TTS 引擎的真实发音人标识 | `zh-CN-XiaoxiaoNeural` | 可切换，唯一身份 |
| **displayName** | voice 的中文显示名 | 给人看的名字 | `晓晓` | 跟随 voice |

**关键**：一个 tag 可绑定不同 voice，用户随时切换。如 `女青年01` 可绑 `XiaoxiaoNeural`(晓晓) 或 `YunyangNeural`(云扬)。

### 1.2 当前数据流（问题所在）

```
配置项(DB system_tts_v2)
  ├─ displayName      = "晓晓"          ← 配置项名（用户可改）
  ├─ config.voice     = "zh-CN-XiaoxiaoNeural"  ← 底层英文 voice（可切换）
  └─ speechRuleInfo
       ├─ tag         = "女青年01"      ← 朗读规则分类键
       └─ tagData     = {personality:"晓晓", ...}
            │
            │ app SpeechRuleEngine.handleText()
            │   只传 tag + tagData 给 JS，【不传底层 voice】
            ▼
JS handleText(text, tagsData)
  └─ tagsData key = tag ("女青年01")
       │ detectAvailableVoices: availableVoices[tag] = true
       │ assignVoice: 返回 tag
       ▼
  record.voice = tag ("女青年01")     ← characterRecords 存的是 tag，不是底层 voice！
       │ fayinren.json: 存 availableVoices 的 key = tag
       ▼
插件 JS generateVoiceTag(record)
  └─ 显示 record.voice = "女青年01"   ← 显示 tag，不是 displayName
```

### 1.3 三个问题的根因

| 问题 | 根因 |
|---|---|
| 标签显示 `女青年01` 而非 `晓晓` | character.voice 存的是 tag，不是底层 voice，无从映射到 displayName |
| voice 切换后标签不变 | 标签存的是 tag（不变），底层 voice 根本没进入角色管理链路 |
| 删除 voice 后无法⚠ | fayinren.json 存 tag，删除的是底层 voice，tag 仍在 → 查得到 → 不⚠ |

**核心矛盾**：底层英文 voice 从未进入朗读规则 JS 和角色管理插件链路。整个链路只处理 tag，所以无法感知 voice 的切换和删除。

### 1.4 已有的映射机制（可复用）

插件 JS 已有 voice↔displayName 双向映射，但数据源是 `fayinren_personality_summary.json`，存的是 `[tag, personality]` 而非 `[voice, displayName]`：

- [replaceFayinrenName(tag)](file:///workspace/tts配套文件/ttsrv-plugin-角色管理_桥接试听v7_方案A已改.js#L57) → 返回 personality（如"晓晓"）
- [reverseReplaceFayinrenName(displayName)](file:///workspace/tts配套文件/ttsrv-plugin-角色管理_桥接试听v7_方案A已改.js#L67) → 返回 tag
- 缓存来自 [fayinren_personality_summary.json](file:///workspace/tts配套文件/ttsrv-speechRule-多角色朗读2.87【加速版+1修复2.2】_原版未改动态扩展.js#L4510)，二维数组 `[[tag, personality], ...]`

这套机制若改数据源为 `[voice, displayName]`，即可复用。

### 1.5 涉及文件

| 层 | 文件 | 作用 |
|---|---|---|
| app | [TagNameUtils.kt](file:///workspace/app/src/main/java/com/github/jing332/tts_server_android/compose/systts/list/TagNameUtils.kt) | computeTagName（当前算 tagName，不含 voice） |
| app | [SpeechRuleEngine.kt](file:///workspace/app/src/main/java/com/github/jing332/tts_server_android/model/rhino/speech_rule/SpeechRuleEngine.kt) | handleText 传 tagsData（**当前不传 voice**） |
| app | [SystemTtsV2.kt](file:///workspace/lib-database/src/main/java/com/github/jing332/database/entities/systts/SystemTtsV2.kt) | 配置项实体（displayName + config，voice 在 config 内） |
| 朗读规则 JS | [ttsrv-speechRule...js](file:///workspace/tts配套文件/ttsrv-speechRule-多角色朗读2.87【加速版+1修复2.2】_动态扩展已改.js) | handleText / detectAvailableVoices / assignVoice / fayinren.json 生成 |
| 插件 JS | [ttsrv-plugin...js](file:///workspace/tts配套文件/ttsrv-plugin-角色管理_桥接试听v7_方案A已改.js) | generateVoiceTag / isVoiceTagValid / replaceFayinrenName |

---

## 二、优化方案对比

### 方案 A：仅改插件 JS（最小改动，先立显示）

**改哪**：只动 `ttsrv-plugin-角色管理_桥接试听v7.js` 的两个函数。

- `generateVoiceTag(character)`：不再调 `splitVoiceDisplay` 拆分，整段显示 `character.voice`；不再拼接 tag+persona 两色，统一单色（或保留蓝色）。
- `isVoiceTagValid(tag)`：放宽校验——`fayinrenList` 为空时通过；非空时只要 `voice` 非空即视为有效，不强制匹配 fayinrenList。

**效果**：
- 标签直接显示 voice（即 tagName，多数情况含 name 语义）。
- ⚠ 基本消失（除非 voice 为空）。

**代价 / 风险**：
- 校验变弱：删了某个配置项后，角色列表不再 ⚠ 提示该发音人已失效（因为不再查 fayinrenList）。
- fayinren.json 仍存 tag，存储语义未统一，后续若要恢复严格校验需再改。
- 若 voice 仍是 "女青年01晓晓" 这种拼接形式，显示的就是拼接串而非纯 name；要纯 name 还需配合方案 C。

**回滚**：还原两个函数即可，零副作用。

---

### 方案 B：让底层 voice 进入 JS 链路（仅改 JS，根除⚠，标签跟随 voice 切换）

**改哪**：动朗读规则 JS + 插件 JS，不改 app。

核心：app 已传 `tagsData`（key=tag），但 tag→voice 的映射 app 侧已知（配置项 config.voice）。方案 B 需让 app 把 voice 也传进来，或在 JS 内建立 tag→voice 映射。

- **app 侧**（轻改）：`SpeechRuleEngine.handleText` 构造 tagsDataMap 时，把每个配置项的底层 voice 也带入（如在 tagData 里加 `"_voice": "zh-CN-XiaoxiaoNeural"` 和 `"_displayName":"晓晓"`）
- **朗读规则 JS**：
  - `detectAvailableVoices`：availableVoices 的 key 改用底层 voice（从 tagData._voice 取），不再用 tag
  - `assignVoice`：返回底层 voice（内部仍用 tag 做 GENSHIN/duihua 匹配，最后转成 voice 返回）
  - `record.voice =` 赋值：存底层 voice
  - `fayinren.json` 生成：存底层 voice 数组
  - `fayinren_personality_summary.json`：改为 `[[voice, displayName], ...]`
- **插件 JS**：
  - `isVoiceTagValid`：用 voice 查 fayinrenList（现存 voice），删除 voice 后查不到→⚠
  - `generateVoiceTag`：显示 `replaceFayinrenName(record.voice)`（voice→displayName），即"晓晓"
  - `replaceFayinrenName` 数据源已是 `fayinren_personality_summary.json`，内容改为 `[voice, displayName]` 后自动生效

**效果**：
- 标签显示 displayName（晓晓），voice 切换后标签跟着变
- 删除 voice 后⚠（fayinren.json 存 voice，删了查不到）
- tag 仍作朗读规则内部键，不影响 GENSHIN/duihua 逻辑

**代价 / 风险**：
- 需改 app 侧传 voice（轻改 SpeechRuleEngine），不能纯 JS
- assignVoice 内部 tag↔voice 转换需建立映射，duihua 动态标签的 voice 来源需确认
- fayinren.json / personality_summary 格式变化，旧插件不兼容

**回滚**：还原 app + JS 文件

---

### 方案 C：app 侧 computeTagName 也用 voice/displayName（全链路统一）

在方案 B 基础上，进一步统一 app 侧 tagName 显示：

- app `computeTagName`：不再调 JS getTagName 拼 personality，直接用配置项 `displayName`（或 voice→displayName 映射）
- 列表配置项标签显示 displayName，与角色管理栏一致
- personality 字段彻底退出显示链路（DB 保留兼容）

**效果**：全链路统一 voice（校验键）+ displayName（显示），tag 退居朗读规则内部

**代价**：改动最大，computeTagName 调用点全量排查

**回滚**：还原 app + JS 两层

---

## 三、推荐路线

1. **第一步（方案 A，已实施）**：插件 JS 显示 tag，⚠ 暂时消失。快速验证观感。
2. **第二步（方案 B）**：让底层 voice 进入链路，标签显示 displayName，⚠ 根除且跟随 voice 切换。需轻改 app 传 voice。
3. **第三步（方案 C，可选）**：app 侧 computeTagName 统一用 displayName，全链路一致。

方案 B 是关键转折点——它让 voice 真正参与角色管理。C 是锦上添花。

---

## 四、实施前的待确认项

1. **app 如何把 voice 传给 JS**：当前 `SpeechRuleEngine.handleText` 只传 tag+tagData。需在 tagData 里加 `_voice` 和 `_displayName`，还是单独加参数？（建议加 tagData 字段，JS 改动最小）
2. **duihua 动态标签的 voice 来源**：duihuaA/duihuaB/duihua 是按性别动态分配的，它们的底层 voice 怎么定？是否用首个匹配的配置项 voice？还是固定映射？
3. **GENSHIN 特殊分支**：[1982行](file:///workspace/tts配套文件/ttsrv-speechRule-多角色朗读2.87【加速版+1修复2.2】_原版未改动态扩展.js#L1982) 按性别分配 duihuaA/duihuaB，voice 落值需单独处理，不能一刀切
4. **fayinren.json 格式兼容**：格式从 tag 数组改为 voice 数组后，旧插件 JS 读会出错——必须 app + 朗读规则JS + 插件JS 三者同步更新
5. **personality 字段去留**：方案 C 下 computeTagName 不再读 personality，配置项编辑页 UI 是否还显示该输入框？（建议隐藏，DB 字段保留兼容旧数据）

---

## 五、关键代码位置索引

### 插件 JS（`ttsrv-plugin-角色管理_桥接试听v7_方案A已改.js`）
- `isVoiceTagValid` —— 1524-1527 行（方案A已放宽为非空即有效）
- `generateVoiceTag` —— 1532-1545 行（方案A已改为显示 voice 原值）
- `replaceFayinrenName` —— 57 行（tag→personality，需改为 voice→displayName）
- `reverseReplaceFayinrenName` —— 67 行（反向映射）
- `_fayinrenMapCache` 初始化 —— 约 70-90 行
- `fayinrenList` 加载 —— 176-202 行

### 朗读规则 JS（`ttsrv-speechRule-..._动态扩展已改.js`）
- `handleText` —— 入口，接收 tagsData
- `detectAvailableVoices` —— 1050 行（availableVoices key=tag，需改 voice）
- `assignVoice` —— 1079-1089 行（返回 tag，需改返回 voice）
- `getTagName` —— 3068-3177 行（GENSHIN / duihua / 其他 三分支）
- `character.voice` 赋值 —— 1982 / 1997 / 2014 / 2922 等多处
- `fayinren.json` 生成 —— 4478-4505 行
- `fayinren_personality_summary.json` —— 4682 行附近（存 [tag, personality]，需改 [voice, displayName]）

### app 侧
- [TagNameUtils.kt#L19](file:///workspace/app/src/main/java/com/github/jing332/tts_server_android/compose/systts/list/TagNameUtils.kt#L19) —— computeTagName（方案C改返回 displayName）
- [SpeechRuleEngine.kt#L26](file:///workspace/app/src/main/java/com/github/jing332/tts_server_android/model/rhino/speech_rule/SpeechRuleEngine.kt#L26) —— getTagName 调 JS
- [SpeechRuleEngine.kt#L90](file:///workspace/app/src/main/java/com/github/jing332/tts_server_android/model/rhino/speech_rule/SpeechRuleEngine.kt#L90) —— handleText 构造 tagsDataMap（**方案B在此加 voice**）
- [SystemTtsV2.kt](file:///workspace/lib-database/src/main/java/com/github/jing332/database/entities/systts/SystemTtsV2.kt) —— 配置项实体（displayName + config，voice 在 config 内）
- [SpeechRuleConfig.kt](file:///workspace/app/src/main/java/com/github/jing332/tts_server_android/conf/SpeechRuleConfig.kt) —— 已新增 lastRoleSig

---

## 六、方案 B 详细实施清单（关键转折点）

> 方案 B 让底层 voice 进入链路，是解决「标签不跟随 voice 切换」「删除 voice 不⚠」的核心。
> **执行顺序**：按步骤编号依次，每步验证后再进下一步。
> **环境限制**：Kotlin 改动无法本地编译（Java 25 致 Gradle 失败），需推 GitHub Actions 用 JDK 17 构建；JS 需真机导入测试。

### 步骤 0：准备

- [ ] 0.1 新建 JS 副本：`ttsrv-speechRule-..._方案B已改.js`、`ttsrv-plugin-..._方案B已改.js`（不覆盖现有文件）
- [ ] 0.2 确认 `SystemTtsV2.config` 能取到底层 voice（如微软 TTS 的 voiceName）
- [ ] 0.3 备份真机 `fayinren.json`、`fayinren_personality_summary.json`、`characterRecords.json`

### 步骤 1：app 侧 — handleText 传 voice（轻改）

**文件**：[SpeechRuleEngine.kt](file:///workspace/app/src/main/java/com/github/jing332/tts_server_android/model/rhino/speech_rule/SpeechRuleEngine.kt#L90)

**改动**：构造 tagsDataMap 时，给每个 tagData 项加 `_voice` 和 `_displayName`：
```kotlin
info.tagData.forEach {
    tagsDataMap[info.tag]!![it.key]!!.add(
        mapOf(
            "id" to info.configId.toString(),
            "value" to it.value
        )
    )
}
// 新增：把配置项的底层 voice 和 displayName 也带入
// 需从 SystemTtsV2.config 取 voice，displayName 直接用 SystemTtsV2.displayName
```
具体取 voice 的方式需确认 TtsConfigurationDTO 结构（微软 TTS 在 source.voice）。

**风险**：tagsDataMap 结构变化，需确认 JS 侧遍历 tagData 时不会因新增 `_voice`/`_displayName` key 出错（JS 遍历的是 tagData 的 key，`_voice` 前缀加下划线避免和正常 key 冲突）

**验证**：GitHub Actions 构建，真机运行朗读，检查 JS 能否拿到 `_voice`

**回滚**：还原 SpeechRuleEngine.kt

### 步骤 2：朗读规则 JS — availableVoices / assignVoice 改用 voice

**文件**：`ttsrv-speechRule-..._方案B已改.js`

**改动点**：
- `detectAvailableVoices`（[1050行](file:///workspace/tts配套文件/ttsrv-speechRule-多角色朗读2.87【加速版+1修复2.2】_原版未改动态扩展.js#L1050)）：availableVoices 的 key 从 tag 改为底层 voice（从 tagData._voice 取）
- `assignVoice`（[1079-1089行](file:///workspace/tts配套文件/ttsrv-speechRule-多角色朗读2.87【加速版+1修复2.2】_原版未改动态扩展.js#L1079)）：内部仍用 tag 做 GENSHIN/duihua 匹配，返回前把 tag 转成底层 voice
- `record.voice =` 赋值（1982/1997/2014/2922行）：存底层 voice

**风险**：
- assignVoice 内部 `isVoiceAvailable` 查询用 voice（改后 key 一致）
- duihua 动态标签（duihuaA/duihuaB）的底层 voice 来源：需从 tagData._voice 取，但 duihua 是按性别动态分配的，可能多个配置项映射到同一 duihua 标签——需确认取哪个 voice
- `usedVoiceMap` 去重改用 voice（底层 voice 唯一，比 tag 更准确）

**验证**：真机运行，检查 characterRecords.json 的 voice 字段是否为底层 voice（如 zh-CN-XiaoxiaoNeural）

**回滚**：还原 JS + 恢复备份

### 步骤 3：朗读规则 JS — fayinren.json / personality_summary 改存 voice

**文件**：`ttsrv-speechRule-..._方案B已改.js`

**改动点**：
- `fayinren.json` 生成（[4478-4505行](file:///workspace/tts配套文件/ttsrv-speechRule-多角色朗读2.87【加速版+1修复2.2】_原版未改动态扩展.js#L4478)）：数组元素从 tag 改为底层 voice
- `fayinren_personality_summary.json`（[4682行](file:///workspace/tts配套文件/ttsrv-speechRule-多角色朗读2.87【加速版+1修复2.2】_原版未改动态扩展.js#L4682)）：从 `[[tag, personality], ...]` 改为 `[[voice, displayName], ...]`

**风险**：格式变化，旧插件 JS 读新格式会错——必须同步步骤 4

**验证**：真机检查两个 json 文件内容

**回滚**：还原 JS + 恢复备份

### 步骤 4：插件 JS — generateVoiceTag 显示 displayName，isVoiceTagValid 查 voice

**文件**：`ttsrv-plugin-..._方案B已改.js`

**改动点**：
- `replaceFayinrenName`（[57行](file:///workspace/tts配套文件/ttsrv-plugin-角色管理_桥接试听v7_方案A已改.js#L57)）：数据源已是 personality_summary，内容改为 `[voice, displayName]` 后，函数自动变为 voice→displayName（无需改函数体，只要缓存内容变了）
- `isVoiceTagValid`（[1524行](file:///workspace/tts配套文件/ttsrv-plugin-角色管理_桥接试听v7_方案A已改.js#L1524)）：恢复查询 fayinrenList（现在存 voice，record.voice 也是 voice，能命中；删除 voice 后查不到→⚠）
- `generateVoiceTag`（[1532行](file:///workspace/tts配套文件/ttsrv-plugin-角色管理_桥接试听v7_方案A已改.js#L1532)）：显示 `replaceFayinrenName(record.voice)`（voice→displayName，显示"晓晓"），恢复⚠提示（voice 查不到时）

**风险**：
- fayinrenList 首次为空时 isVoiceTagValid 不能误⚠（保留「空列表不拦截」逻辑）
- duihua 的底层 voice 必须在步骤3写入 fayinren.json，否则误⚠

**验证**：真机进角色管理栏，标签显示"晓晓"；切换某 tag 绑定的 voice 后标签变；删除一个 voice 后该角色⚠

**回滚**：还原插件 JS

### 步骤 5：JS 分发

- 分发 `_方案B已改.js` 两个文件给用户导入
- app 侧改动（步骤1）打包进新 APK
- **强调**：app + 朗读规则JS + 插件JS 必须同步更新，旧版任一不匹配都会出错

---

## 七、方案 B 风险矩阵与决策点

| 风险 | 影响 | 缓解 |
|---|---|---|
| app 取底层 voice 的方式因 TTS 引擎而异 | 微软/Edge/本地 TTS 的 voice 字段位置不同 | 在 SpeechRuleEngine 里统一抽取（参考响度均衡的 `source.voice` 用法） |
| duihua 动态标签的 voice 来源不明 | assignVoice 转 voice 时找不到对应 | 确认 duihua 分配时是否已有配置项上下文，取其 voice |
| fayinren.json 格式不向后兼容 | 旧插件读新格式出错 | 强制三件套同步更新，发版说明强调 |
| replaceFayinrenName 缓存未刷新 | 标签显示旧 personality | 确认缓存随 personality_summary 文件变化刷新 |
| assignVoice 内部 tag↔voice 映射建立 | 转换失败导致分配异常 | 内部维护 tag→voice 映射表（来自 tagData._voice） |

### 待决策点（动手前必须确认）

1. **底层 voice 的统一抽取方式**：微软 TTS 在 `source.voice`，其他 TTS 引擎呢？需在 app 侧统一封装一个 `getVoice()` 方法
2. **duihua 动态标签的 voice**：duihuaA/duihuaB 按性别分配，它们的底层 voice 取哪个配置项的？还是用首个匹配项？
3. **personality 字段 UI 去留**：方案 B 下 personality 不再参与显示，配置项编辑页是否隐藏该输入框？（建议隐藏，DB 保留）

### 方案 C（可选，方案 B 之上）

方案 B 完成后，若要进一步统一 app 侧 computeTagName 显示 displayName：
- `computeTagName` 不再调 JS getTagName 拼 personality，直接返回配置项 displayName
- 调用点排查：[ListManagerScreen.kt](file:///workspace/app/src/main/java/com/github/jing332/tts_server_android/compose/systts/list/ListManagerScreen.kt)、[BatchTagDialog.kt](file:///workspace/app/src/main/java/com/github/jing332/tts_server_android/compose/systts/list/BatchTagDialog.kt) 等
- 方案 C 是锦上添花，方案 B 已解决核心痛点
