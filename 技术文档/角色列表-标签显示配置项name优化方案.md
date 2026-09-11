# 角色列表：标签显示发音人真实 voice 优化方案

> 目标：让角色列表的发音人标签显示当前绑定 voice 的 displayName（如"晓晓"），voice 切换后标签跟着变；用底层英文 voice 作唯一校验键，删除 voice 后能⚠。
> 关联文档：`docs/角色列表-标签性格机制与自动刷新.md`
> 状态：**方案 B-1 已实施**（2026-08-04，底层 voice 进入链路，标签显示 displayName，⚠ 恢复）；方案 A 已废弃（文件已删除）；C 待定（app 侧 computeTagName 统一）
>
> **2026-08-08 更新**：兜底体系改造（详见 `docs/兜底体系架构.md`）。assignVoice 不再返回 null，改为按性别直接返回 `duihuaA`/`duihuaB`/`duihua` 兜底 tag，并输出日志。因新增日志和兜底逻辑，本文档中引用的 JS 行号可能已偏移，请以实际代码为准。

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

- replaceFayinrenName(tag)（角色管理插件JS第57行） → 返回 personality（如"晓晓"）
- reverseReplaceFayinrenName(displayName)（角色管理插件JS第67行） → 返回 tag
- 缓存来自 fayinren_personality_summary.json（朗读规则JS第4510行），二维数组 `[[tag, personality], ...]`

这套机制若改数据源为 `[voice, displayName]`，即可复用。

### 1.5 涉及文件

| 层 | 文件 | 作用 |
|---|---|---|
| app | [TagNameUtils.kt](file:///workspace/app/src/main/java/com/github/jing332/tts_server_android/compose/systts/list/TagNameUtils.kt) | computeTagName（当前算 tagName，不含 voice） |
| app | [SpeechRuleEngine.kt](file:///workspace/app/src/main/java/com/github/jing332/tts_server_android/model/rhino/speech_rule/SpeechRuleEngine.kt) | handleText 传 tagsData（**当前不传 voice**） |
| app | [SystemTtsV2.kt](file:///workspace/lib-database/src/main/java/com/github/jing332/database/entities/systts/SystemTtsV2.kt) | 配置项实体（displayName + config，voice 在 config 内） |
| 朗读规则 JS | 朗读规则JS | handleText / detectAvailableVoices / assignVoice / fayinren.json 生成 |
| 插件 JS | 角色管理插件JS | generateVoiceTag / isVoiceTagValid / replaceFayinrenName |

---

## 二、优化方案对比

### 方案 A：仅改插件 JS（最小改动，先立显示）

**改哪**：只动角色管理插件JS的两个函数。

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

1. ~~第一步（方案 A，已废弃）~~：插件 JS 显示 tag，⚠ 暂时消失。已被方案 B-1 取代，不再使用。
2. **第二步（方案 B-1，已实施）**：让底层 voice 进入链路，标签显示 displayName，⚠ 根除且跟随 voice 切换。详见第六章实施记录。
3. **第三步（方案 C，可选）**：app 侧 computeTagName 统一用 displayName，全链路一致。

方案 B-1 是关键转折点——它让 voice 真正参与角色管理。C 是锦上添花。

---

## 四、实施前的待确认项

1. **app 如何把 voice 传给 JS**：当前 `SpeechRuleEngine.handleText` 只传 tag+tagData。需在 tagData 里加 `_voice` 和 `_displayName`，还是单独加参数？（建议加 tagData 字段，JS 改动最小）
2. **duihua 动态标签的 voice 来源**：duihuaA/duihuaB/duihua 是按性别动态分配的，它们的底层 voice 怎么定？是否用首个匹配的配置项 voice？还是固定映射？
3. **GENSHIN 特殊分支**：朗读规则JS第1982行 按性别分配 duihuaA/duihuaB，voice 落值需单独处理，不能一刀切
4. **fayinren.json 格式兼容**：格式从 tag 数组改为 voice 数组后，旧插件 JS 读会出错——必须 app + 朗读规则JS + 插件JS 三者同步更新
5. **personality 字段去留**：方案 C 下 computeTagName 不再读 personality，配置项编辑页 UI 是否还显示该输入框？（建议隐藏，DB 字段保留兼容旧数据）

---

## 五、关键代码位置索引

### 插件 JS（角色管理插件JS）
- `isVoiceTagValid` —— 1524-1527 行（方案A已放宽为非空即有效）
- `generateVoiceTag` —— 1532-1545 行（方案A已改为显示 voice 原值）
- `replaceFayinrenName` —— 57 行（tag→personality，需改为 voice→displayName）
- `reverseReplaceFayinrenName` —— 67 行（反向映射）
- `_fayinrenMapCache` 初始化 —— 约 70-90 行
- `fayinrenList` 加载 —— 176-202 行

### 朗读规则 JS（朗读规则JS）
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

## 六、方案 B-1 实施记录（已完成，2026-08-04）

> 方案 B-1 = 方案 B 的完整实施。核心：让底层英文 voice 进入 JS 链路，record.voice 统一存底层 voice，标签显示 displayName，删除 voice 后⚠。
> **环境限制**：Kotlin 改动无法本地编译（Java 25 + 无 Android SDK），已通过静态代码审查核验（3文件全通过，8处构造调用+23处copy()调用兼容）；JS 需真机导入测试。

### 6.1 设计原则（三层语义）

| 标识 | 用途 | 例子 |
|---|---|---|
| **voiceTag**（朗读规则分类键） | `isVoiceAvailable` 入参、序号排序 `info.voice.match(/\d+$/)`、duihua 候选池、GENSHIN 反查 | `女青年01` |
| **底层voice**（TTS引擎真实标识） | `usedVoiceMap`/`voiceUsageMap`/`usedVoices` 的 key、`candidates[].voice`、所有 `return`、`record.voice`、`fayinren.json` 元素 | `zh-CN-XiaoxiaoNeural` |
| **displayName** | 桥接插件内存 record.voice、fayinrenList 元素、角色列表标签显示 | `晓晓` |

转换关系：`底层voice = this.voiceTagToVoice[voiceTag] || voiceTag`（兜底用 voiceTag 自身）。

### 6.2 app 侧改动（3 文件）

**① [SpeechRuleInfo.kt](file:///workspace/lib-database/src/main/java/com/github/jing332/database/entities/systts/SpeechRuleInfo.kt)**
新增两个 `@Transient` 字段（不进 DB/JSON 序列化，运行时由 init 填充）：
```kotlin
@Transient var voice: String = "",
@Transient var displayName: String = "",
```
- import `kotlinx.serialization.Transient`（非 androidx.room.Transient）
- 与既有 `HttpTTS.kt` 同模式（只 `@Transient` 不 `@IgnoredOnParcel`），跨 Parcel 保留旧值但每次 init 覆盖，无害

**② [TextProcessor.kt#L61-L75](file:///workspace/app/src/main/java/com/github/jing332/tts_server_android/service/systts/help/TextProcessor.kt#L61-L75)**
init() 中填充 voice/displayName：
```kotlin
val systts = cfg.tag as? SystemTtsV2
cfg.copy(
    speechInfo = cfg.speechInfo.copy(
        configId = entry.key,
        voice = cfg.source.voice,           // TextToSpeechSource.abstract val
        displayName = systts?.displayName ?: "" // SystemTtsV2.displayName
    )
)
```
- `cfg.tag` 由 `TtsRepository.getAllTts()` 通过 `.copy(tag = systts)` 设置为 `SystemTtsV2` 实例

**③ [SpeechRuleEngine.kt](file:///workspace/app/src/main/java/com/github/jing332/tts_server_android/model/rhino/speech_rule/SpeechRuleEngine.kt)**
handleText 中把 voice/displayName 传入 JS 的 `tagsDataMap`，特殊 key `_voice`/`_displayName`：
```kotlin
if (info.voice.isNotEmpty()) {
    tagsDataMap[info.tag]!!["_voice"]!!.add(mapOf("id" to info.configId.toString(), "value" to info.voice))
}
if (info.displayName.isNotEmpty()) {
    tagsDataMap[info.tag]!!["_displayName"]!!.add(mapOf("id" to info.configId.toString(), "value" to info.displayName))
}
```

### 6.3 朗读规则 JS 改动（朗读规则JS）

**① detectAvailableVoices（朗读规则JS第1050-1094行）**
- `availableVoices` 的 key 改为底层 voice（从 `tagsData[voiceTag]._voice[0].value` 取，取不到回退 voiceTag）
- 新增 `voiceTagToVoice` / `voiceTagToDisplayName` 映射表

**② isVoiceAvailable（朗读规则JS第1096-1100行）**
入参仍是 voiceTag，内部转底层 voice 后查 `availableVoices`

**③ assignVoice 内部 6 处（朗读规则JS第1121-1290行）**
统一用底层 voice 做去重 key 和返回值，voiceTag 仅用于 `isVoiceAvailable` 校验和序号排序：
- duihua 分支：`uv = voiceTagToVoice[voiceTag]||voiceTag`，去重/返回用 uv
- 核心候选：`candidates.push({voice: uv})`
- sameTypeAvailableMap：key 用底层 voice（与 record.voice 一致）
- allSameTypeVoices / allSameGenderVoices：voice 用底层，seq 仍用 voiceTag 数字后缀
- 年龄降级：同核心候选

**④ GENSHIN 反查（朗读规则JS第2044-2053行）**
record.voice 已是底层 voice，需把 GENSHIN 的 voiceTag 转底层后再比：
```js
var uv = (cm && cm.voiceTagToVoice && cm.voiceTagToVoice[GENSHIN_CHARACTERS[key].voice]) || GENSHIN_CHARACTERS[key].voice;
if (uv === targetMainRecord.voice) { ... }
```

**⑤ 映射文件生成（朗读规则JS第4733-4737行 和 朗读规则JS第4775-4779行）**
`fayinren_personality_summary.json` 从 `[voiceTag, voiceTag+personality]` 改为 `[底层voice, displayName]`，duihua 分支和硬编分支同步改。

### 6.4 桥接插件改动（角色管理插件JS）

**核心机制：双层闭环（无需改 replaceFayinrenName）**
- 内存 `characterRecords`：voice 存 **displayName**（读取时 `replaceFayinrenName` 转换）
- 存盘 `characterRecords.json`：voice 存 **底层voice**（保存时 `reverseReplaceFayinrenName` 反向转换）
- `fayinrenList`：元素经 `replaceFayinrenName`，存 **displayName**

**① isVoiceTagValid（角色管理插件JS第1525-1531行）**
校验 record.voice(displayName) 是否在 fayinrenList(displayName集合) 中，不在则无效：
```js
function isVoiceTagValid(voice) {
    if (!voice) return false;
    for (var i = 0; i < fayinrenList.length; i++) {
        if (fayinrenList[i] === voice) return true;
    }
    return false;
}
```

**② generateVoiceTag（角色管理插件JS第1535-1557行）**
直接显示 character.voice（已是 displayName），无效时追加红色 ⚠：
```js
if (!isValid) {
    ssb.append(" ⚠");
    ssb.setSpan(new ForegroundColorSpan(CLR_WARN), ...);
}
```

**③ getVoiceByTag 闭环（app 侧 [TtsEngineContext.kt#L118](file:///workspace/lib-tts/src/main/java/com/github/jing332/tts/speech/plugin/engine/TtsEngineContext.kt#L118)）**
桥接 `buildList`/`refreshVoiceTagsOnly` 调 `ttsrv.getVoiceByTag(charName)` 获取实时生效的发音人，app 返回 `match.displayName`——恰好是 displayName，与内存 record.voice 语义一致，闭环自洽。

### 6.5 自检发现的衍生问题（已评估，按需处理）

| 问题 | 位置 | 严重性 | 处理 |
|---|---|---|---|
| ~~assignVoice 返回 null~~ → **已改为兜底分配** | ~~L2032~~（行号已偏移） | 低 | **已更新（2026-08-08）**。assignVoice 不再返回 null，改为按性别直接返回 `duihuaA`/`duihuaB`/`duihua` 兜底 tag 并输出 `[SpeechRule] 【兜底分配】` 日志。详见 `docs/兜底体系架构.md` |
| duihua roleValue（如"青年20"）混入 availableVoices/fayinren.json | 朗读规则JS第4522行、朗读规则JS第4601行 | 低 | **保留**。duihua 动态标签无底层voice映射，roleValue 即其标识；assignVoice 的 duihua 分支 `uv=voiceTagToVoice[voiceTag]\|\|voiceTag` 让返回值与 key 一致（都是 roleValue），自洽；桥接 replaceFayinrenName 查不到 roleValue 原样显示，合理 |
| 朗读规则JS内部命令显示英文voice | 朗读规则JS第5208行（qjs统计）、朗读规则JS第5238行（setFixedVoice）、printAvailableVoices | 中 | **暂不处理**。属朗读规则JS内部命令（非角色列表标签），不在本次核心目标范围；用户已确认暂不需要 |
| 桥接「系统TTS重新分配」逻辑依赖 `displayName.match(/^(.+?)\d/)` 分类 | 角色管理插件JS第4665行 | 中 | **暂不处理**。方案B下 displayName 是"晓晓"非"女青年01"，正则失效；但这是「未开转发器+系统TTS重分配」边缘场景，需单独适配 |

### 6.6 验证清单（真机/模拟器）

- [ ] Kotlin 编译通过（推 GitHub Actions 用 JDK 17 构建）
- [ ] 多角色朗读正常，characterRecords.json 的 voice 字段为底层 voice
- [ ] 角色列表标签显示 displayName（如"晓晓"）
- [ ] 切换某 tag 绑定的 voice 后，标签同步更新
- [ ] 删除某 voice 后，对应角色标签显示 ⚠
- [ ] 发音人去重正常（同一底层 voice 不被重复分配）
- [ ] fayinren.json 存底层 voice 数组
- [ ] fayinren_personality_summary.json 存 `[底层voice, displayName]` 二维数组

### 6.7 分发说明

- app 侧改动（SpeechRuleInfo/TextProcessor/SpeechRuleEngine）打包进新 APK
- 分发 tts配套文件/ 目录下的两个 JS 文件给用户导入
- **强调**：app + 朗读规则JS + 插件JS 必须同步更新，旧版任一不匹配都会出错

---

## 七、方案 B 风险矩阵与决策点（实施后复盘）

| 风险 | 影响 | 实施后状态 |
|---|---|---|
| app 取底层 voice 的方式因 TTS 引擎而异 | 微软/Edge/本地 TTS 的 voice 字段位置不同 | **已解决**。`TextToSpeechSource.voice` 是 abstract val，子类 LocalTtsSource/PluginTtsSource 均 override，统一通过 `cfg.source.voice` 抽取 |
| duihua 动态标签的 voice 来源不明 | assignVoice 转 voice 时找不到对应 | **已解决（自洽混合）**。duihua 动态标签无底层voice映射，roleValue 即其标识；assignVoice 的 duihua 分支 `uv=voiceTagToVoice[voiceTag]\|\|voiceTag` 让返回值与 availableVoices key 一致 |
| fayinren.json 格式不向后兼容 | 旧插件读新格式出错 | **已缓解**。强制三件套同步更新，发版说明强调（见 6.7） |
| replaceFayinrenName 缓存未刷新 | 标签显示旧 personality | **已解决**。双层闭环设计，`_initFayinrenMapCache(true)` 强制刷新 |
| assignVoice 内部 tag↔voice 映射建立 | 转换失败导致分配异常 | **已解决**。`voiceTagToVoice` 映射表在 detectAvailableVoices 建立，兜底 `voiceTag` 保证不报错 |

### 决策点结论（已确认）

1. **底层 voice 的统一抽取方式**：✅ 用 `TextToSpeechSource.voice`（abstract val），无需额外封装
2. **duihua 动态标签的 voice**：✅ duihua 标签用 roleValue 自身作标识，不强行转底层 voice（见 6.5 衍生问题2）
3. **personality 字段 UI 去留**：✅ 朗读规则JS的 tag 分类功能保留（用户无需感知 tag 序号，仅内部用于分配和去重）；personality 字段 UI 隐藏属方案 C 范围，待定

### 方案 C（可选，方案 B-1 之上）

方案 B-1 完成后，若要进一步统一 app 侧 computeTagName 显示 displayName：
- `computeTagName` 不再调 JS getTagName 拼 personality，直接返回配置项 displayName
- 调用点排查：[ListManagerScreen.kt](file:///workspace/app/src/main/java/com/github/jing332/tts_server_android/compose/systts/list/ListManagerScreen.kt)、[BatchTagDialog.kt](file:///workspace/app/src/main/java/com/github/jing332/tts_server_android/compose/systts/list/BatchTagDialog.kt) 等
- 隐藏配置项编辑页的 personality 字段 UI（DB 字段保留兼容旧数据）
- 方案 C 是锦上添花，方案 B-1 已解决核心痛点
