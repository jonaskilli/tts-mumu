# 角色列表：标签显示配置项 name 优化方案

> 目标：让角色列表的发音人标签直接显示配置项 name，省去配置项里单独填写「性格(personality)」字段，并根除「被标记为没有发音人(⚠)」的现象。
> 关联文档：`docs/角色列表-标签性格机制与自动刷新.md`
> 状态：**方案 A 已实施**（2026-08-04），B/C 待定

---

## 一、现状与根因

### 1.1 数据流（核对自代码）

```
配置项(DB system_tts_v2)
  ├─ name            = "晓晓"            ← 用户可见的配置项名
  ├─ tag             = "女青年01"        ← 朗读规则内部发音人分类键
  └─ tagData.personality = "晓晓"
        │ 朗读规则 JS: getTagName(tag, tagData)
        ▼
  tagName = "女青年01晓晓"                ← 写入 characterRecords.json 的 record.voice
        │ 同时：fayinren.json 存的是纯 tag（availableVoices 的 key）
        ▼
  插件 JS: splitVoiceDisplay(record.voice)  把 "女青年01晓晓" 拆回
        ├─ tag="女青年01"  → isVoiceTagValid() 查 fayinrenList
        └─ persona="晓晓"
```

### 1.2 两个现象的根因

| 现象 | 根因 | 代码位置 |
|---|---|---|
| 标签偶尔变成配置项 name | `getTagName` 的「其他标签」分支 `return this.tags[tag] \|\| "旁白"`，未走 GENSHIN/duihua 或未填 personality 时 tagName 退化为映射名/旁白 | 朗读规则 JS `getTagName` 3173-3176 行 |
| 被标记为没有发音人(⚠) | `fayinren.json` 存的是 `tag`，而显示用 `voice`(=tagName)拆出的 tag 去查；当显示文本≠tag 时查不到 → ⚠ | 插件 JS `isVoiceTagValid` 1523 行、`generateVoiceTag` 1534 行 |

**核心矛盾**：存储键(`tag`)、voice 值(`tagName`)、期望显示文本(`name`)三者不一致。只要不一致，⚠ 就无法根除。

### 1.3 涉及文件

| 层 | 文件 | 作用 |
|---|---|---|
| app | `app/.../compose/systts/list/ui/PluginDescriptor.kt` | 列表配置项显示 |
| app | `app/.../conf/SpeechRuleConfig.kt` | tagName 计算相关配置 |
| app | `lib-.../TagNameUtils`（computeTagName） | tag+tagData → tagName |
| 朗读规则 JS | `tts配套文件/ttsrv-speechRule-多角色朗读2.87【加速版+1修复2.2】.js` | getTagName / fayinren.json 生成 / character.voice 赋值 |
| 角色管理插件 JS | `tts配套文件/ttsrv-plugin-角色管理_桥接试听v7.js` | generateVoiceTag / isVoiceTagValid / splitVoiceDisplay |

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

### 方案 B：朗读规则 JS 统一存储键为 name（根除 ⚠，改动大）

**改哪**：动 `ttsrv-speechRule-多角色朗读2.87【加速版+1修复2.2】.js`。

- `fayinren.json` 生成处（约 4478-4505 行）：保存 availableVoices 时，key 从 `tag` 改为对应配置项 `name`（需建立 tag→name 映射，来源是 `getEnabledListForSort(TAG)` 返回的配置项）。
- `character.voice` 赋值处：所有 `record.voice = ...`（多处，如 1982/1997/2014/2922 等）改为写 name。
- `getTagName`：可保留原逻辑（tagName 仍可拼接性格），但 voice 最终落 name；或简化为直接返回 name。

**效果**：
- 存储键、voice 值统一为 name，`isVoiceTagValid` 用 name 查 fayinrenList 必然命中，⚠ 根除。
- 配置项无需填 personality。

**代价 / 风险**：
- `availableVoices` 内部以 tag 为 key 是核心数据结构（GENSHIN/duihua 映射、roleToRootIdMap 等都依赖 tag），全面替换为 name 易引入 bug。
- 改动点分散（voice 赋值有十几处），回归测试成本高。
- 朗读规则 JS 是用户可导入替换的脚本，改后需重新分发。

**回滚**：还原 JS 文件。

---

### 方案 C：app 侧 tagName=name + 朗读规则 JS fayinren=name（全链路统一）

**改哪**：跨 app + JS 两层。

- app `TagNameUtils.computeTagName`：直接返回配置项 `name`，不再拼接 personality。
- 朗读规则 JS：`fayinren.json` 存 name（同方案 B 的存储改动）。
- 插件 JS：`generateVoiceTag` 显示 voice（=name），`isVoiceTagValid` 用 name 查 fayinrenList。
- 配置列表 `PluginDescriptor` 等显示 tagName 的地方同步跟随。

**效果**：
- 全链路统一为 name：配置项名 = 标签 = voice = 显示文本。
- 配置项完全不用填 personality，省一步操作。
- ⚠ 根除，且语义自洽。

**代价 / 风险**：
- 改动跨层，影响面最大。
- `computeTagName` 被 app 多处依赖（配置列表、导出、调试等），改返回值需全量排查调用点。
- 现有已填 personality 的配置项数据需兼容（可忽略 personality 字段，不报错即可）。
- 朗读规则 JS 仍需重新分发。

**回滚**：还原 app + JS 两层文件。

---

## 三、推荐路线

**分两步走，先收益后彻底：**

1. **第一步（方案 A）**：只改插件 JS 两个函数，立即让显示变 name、⚠ 基本消失。风险最低，可快速验证观感是否符合预期。
2. **第二步（方案 C）**：确认观感后，做全链路统一。配置项彻底去掉 personality 依赖，fayinren.json 存 name，⚠ 根除且语义自洽。

方案 B（纯朗读规则 JS 改动）作为 C 的子集，若第一步后觉得「校验变弱」可接受，可跳过 B 直接做 C；若想保留严格校验又暂不动 app，可先做 B 过渡。

---

## 四、实施前的待确认项

1. **方案 A 的「校验变弱」是否可接受**：删配置项后角色列表不再 ⚠，是否需要保留某种弱提示（如 voice 为空才⚠）？
2. **历史 personality 数据兼容**：方案 C 下，已填 personality 的配置项是否需要迁移脚本清理，还是直接忽略该字段？
3. **GENSHIN/duihua 特殊分支**：这两类标签的 voice 赋值有特殊逻辑（如 1982 行按性别分配 duihuaA/duihuaB），方案 B/C 改 voice 落 name 时需单独处理，不能一刀切。
4. **朗读规则 JS 分发**：方案 B/C 改了朗读规则 JS，需确认用户侧更新机制（手动导入 / 应用内更新）。

---

## 五、关键代码位置索引

### 插件 JS（`ttsrv-plugin-角色管理_桥接试听v7.js`）
- `isVoiceTagValid` —— 1523-1531 行
- `generateVoiceTag` —— 1534-1568 行
- `splitVoiceDisplay` —— 1580-1589 行
- `fayinrenList` 加载 —— 176-202 行

### 朗读规则 JS（`ttsrv-speechRule-多角色朗读2.87【加速版+1修复2.2】.js`）
- `getTagName` —— 3068-3177 行（GENSHIN / duihua / 其他 三分支）
- `fayinren.json` 生成 —— 4478-4505 行
- `character.voice` 赋值 —— 1982 / 1997 / 2014 / 2922 等多处
- `fayinren_personality_summary.json` —— 4682 行附近

### app 侧
- `TagNameUtils.computeTagName` —— [TagNameUtils.kt#L19](file:///workspace/app/src/main/java/com/github/jing332/tts_server_android/compose/systts/list/TagNameUtils.kt#L19)（tagName 计算入口，含 personality 追加逻辑）
- `SpeechRuleEngine.getTagName` —— [SpeechRuleEngine.kt#L26](file:///workspace/app/src/main/java/com/github/jing332/tts_server_android/model/rhino/speech_rule/SpeechRuleEngine.kt#L26)（调用 JS getTagName）
- `PluginDescriptor` —— 配置列表显示（需进一步定位具体文件）
- `SpeechRuleConfig.kt` —— 已新增 `lastRoleSig`（自动刷新签名，见关联文档）

---

## 六、方案 C 详细实施清单

> 本清单为方案 C（全链路统一 name）的分步实施计划，每步含改动点、风险、验证、回滚。
> **执行顺序**：按步骤编号依次进行，每步完成后单独验证再进入下一步。
> **总原则**：app 侧改动的 Kotlin 文件无法在当前环境编译验证（Java 25 致 Gradle 脚本初始化失败），需推到 GitHub Actions 用 JDK 17 构建验证；JS 文件需真机导入测试。

### 步骤 0：准备工作（必做）

- [ ] 0.1 新建 JS 文件副本：`ttsrv-speechRule-..._方案C已改.js`、`ttsrv-plugin-..._方案C已改.js`，基于 `_动态扩展已改.js` / `_方案A已改.js` 修改，**不覆盖现有文件**
- [ ] 0.2 确认配置项 DB 有 `name` 字段且可读（`SystemTtsV2` 实体的 `name`，已确认存在）
- [ ] 0.3 备份当前 `fayinren.json`、`characterRecords.json`（真机上），用于回滚对比

### 步骤 1：app 侧 computeTagName 改返回 name（核心改动）

**文件**：[TagNameUtils.kt](file:///workspace/app/src/main/java/com/github/jing332/tts_server_android/compose/systts/list/TagNameUtils.kt)

**改动**：
- `computeTagName` 不再调用 `engine.getTagName(ruleData.tag, ruleData.tagData)`，直接返回 `ruleData` 对应配置项的 `name`
- 删除 personality 追加逻辑（第 48-57 行）：不再 `base + personality`
- 保留 ⚠ 回退：若拿不到 name，回退到 `fallback`（不再查 `speechRule.tags` 映射）

**问题**：`computeTagName` 入参是 `SpeechRuleInfo`（含 tag/tagData/configId），**不含 name**。需确认 name 怎么取：
- 选项 A：给 `computeTagName` 加 `name: String` 参数，调用方传入
- 选项 B：通过 `configId` 反查 DB（`dbm.systemTtsV2.getById(configId).name`），但增加 DB 查询

**风险**：
- `computeTagName` 调用点需全部排查（一键分配/重排、批量分配标签对话框），确认 name 来源
- 返回值变化影响所有依赖 tagName 的下游（列表显示、导出）

**验证**：GitHub Actions 构建 + 真机看配置列表标签是否显示 name

**回滚**：还原 TagNameUtils.kt

### 步骤 2：朗读规则 JS — fayinren.json 存 name

**文件**：`ttsrv-speechRule-..._方案C已改.js`

**改动点**：[4478-4505 行](file:///workspace/tts配套文件/ttsrv-speechRule-多角色朗读2.87【加速版+1修复2.2】_原版未改动态扩展.js#L4478) fayinren.json 生成处

- `detectAvailableVoices`（[1050行](file:///workspace/tts配套文件/ttsrv-speechRule-多角色朗读2.87【加速版+1修复2.2】_原版未改动态扩展.js#L1050)）保持以 voiceTag 为 key（内部数据结构不动）
- 保存 fayinren.json 时，把 voiceTag 转成 name：需建立 `voiceTag → name` 映射
  - 映射来源：`GENSHIN_CHARACTERS` 里 `chars[name].voice = voiceTag`，反向查即可
  - 但 duihua 动态标签（duihuaA/duihuaB/duihua）不在 GENSHIN_CHARACTERS，需单独处理：duihua 标签的 name 取 `roleToRootIdMap` 对应值或保留原 tag
- 写入 fayinren.json 的数组元素从 voiceTag 改为 name

**风险**：
- duihua 动态标签无对应 name，需 fallback 策略（保留原 tag 或用「对话男/对话女」）
- fayinren.json 格式变化，旧插件 JS 读它会出错——必须同步改插件 JS（步骤 4）

**验证**：真机运行朗读，检查 fayinren.json 内容是否为 name 数组

**回滚**：还原 JS 文件 + 恢复备份的 fayinren.json

### 步骤 3：朗读规则 JS — character.voice 赋值改 name

**文件**：`ttsrv-speechRule-..._方案C已改.js`

**改动点**：所有 `record.voice = ...` 赋值处（[1982/1987/1997/2014/2922行等](file:///workspace/tts配套文件/ttsrv-speechRule-多角色朗读2.87【加速版+1修复2.2】_原版未改动态扩展.js#L1982)）

- `assignVoice()` 返回值从 voiceTag 改为 name：需在 `assignVoice` 内部做 voiceTag→name 转换
- 或在赋值点统一转换：`record.voice = voiceTagToName(assignVoice(...))`
- **特殊处理 duihua 分支**（[1987行](file:///workspace/ttsrv-speechRule-多角色朗读2.87【加速版+1修复2.2】_原版未改动态扩展.js#L1987)）：`duihuaA/duihuaB/duihua` 这些非 GENSHIN 标签，按「对话男/对话女/对话」映射

**风险**：
- `assignVoice` 内部用 voiceTag 做 `isVoiceAvailable` 查询（[1089行](file:///workspace/tts配套文件/ttsrv-speechRule-多角色朗读2.87【加速版+1修复2.2】_原版未改动态扩展.js#L1089)），若返回 name 会破坏查询
- `usedVoiceMap`（[1079行](file:///workspace/tts配套文件/ttsrv-speechRule-多角色朗读2.87【加速版+1修复2.2】_原版未改动态扩展.js#L1079)）以 voice 为 key 去重，改 name 后去重逻辑可能失效（同名配置项）
- **建议**：`assignVoice` 内部仍用 voiceTag，仅在 `record.voice =` 赋值时转 name。这样内部逻辑不变，只改最终存储值

**验证**：真机运行朗读，检查 characterRecords.json 的 voice 字段是否为 name

**回滚**：还原 JS 文件

### 步骤 4：插件 JS — generateVoiceTag / isVoiceTagValid 用 name

**文件**：`ttsrv-plugin-..._方案C已改.js`

**改动点**：
- `isVoiceTagValid`（[1524行](file:///workspace/tts配套文件/ttsrv-plugin-角色管理_桥接试听v7_方案A已改.js#L1524)）：恢复查询 fayinrenList（现在 fayinrenList 存的是 name，voice 也是 name，能命中）
- `generateVoiceTag`（[1532行](file:///workspace/tts配套文件/ttsrv-plugin-角色管理_桥接试听v7_方案A已改.js#L1532)）：保持显示 voice（已是 name），恢复 ⚠ 提示（voice 查不到 fayinrenList 时⚠，即发音人被删的提示恢复）
- `splitVoiceDisplay`：可保留但不再被 generateVoiceTag 调用（其他地方如试听可能还用）

**风险**：
- fayinrenList 加载时机：onLoadUI 时读 fayinren.json，若 fayinren.json 还没生成（首次运行）会空，isVoiceTagValid 需保留「空列表不拦截」逻辑
- duihua 标签的 name（「对话男」等）若不在 fayinrenList，会误⚠——需确保步骤 2 把 duihua 的 name 也写入 fayinren.json

**验证**：真机进角色管理栏，看标签是否显示 name，删除一个发音人后是否⚠

**回滚**：还原插件 JS 文件

### 步骤 5：app 侧列表显示同步

**文件**：PluginDescriptor 等显示 tagName 处（需进一步定位）

**改动**：确认列表配置项显示的标签来源，若直接用 tagName 则跟随步骤 1 自动变化；若有独立显示逻辑需同步改

**验证**：真机看系统TTS列表配置项标签

**回滚**：还原相关文件

### 步骤 6：历史 personality 数据兼容

**处理**：已填 personality 的配置项，`tagData["personality"]` 字段保留不删，但 `computeTagName` 不再读取它（步骤 1 已删读取逻辑）。无需迁移脚本，旧数据静默忽略。

**验证**：导入旧配置项数据，确认不报错、标签显示 name

### 步骤 7：JS 分发

- 将 `_方案C已改.js` 两个文件分发给用户导入
- 确认 app 侧改动已打包进新版本 APK
- 旧版本 app + 旧 JS 不兼容新 fayinren.json 格式——需在发版说明里强调「必须同时更新 app 和两个 JS」

---

## 七、方案 C 风险矩阵与决策点

| 风险 | 影响 | 缓解 |
|---|---|---|
| assignVoice 内部逻辑破坏 | 发音人分配失败/重复 | 内部保持 voiceTag，仅赋值点转 name |
| duihua 标签无 name | fayinren.json 缺项、误⚠ | 步骤 2 单独映射「对话男/女」 |
| 同名配置项去重失效 | usedVoiceMap 失效 | 用 configId 去重而非 voice |
| app+JS 版本不匹配 | fayinren.json 格式不兼容 | 强调同步更新 |
| computeTagName 调用点遗漏 | 部分标签仍显示旧格式 | 全量排查调用点（见下） |

### 待决策点（动手前必须确认）

1. **computeTagName 的 name 来源**：加参数传入 vs 反查 DB？倾向加参数（避免 DB 查询，调用方已有 name）
2. **duihua 标签的 name 映射**：用「对话男/对话女/对话」还是保留 duihuaA/duihuaB？
3. **是否保留 personality 字段在 UI**：配置项编辑页是否还显示 personality 输入框？（建议隐藏，但 DB 字段保留兼容）

### computeTagName 调用点排查清单

需确认以下文件是否调用 computeTagName，改动后是否兼容：
- [ListManagerScreen.kt](file:///workspace/app/src/main/java/com/github/jing332/tts_server_android/compose/systts/list/ListManagerScreen.kt)（一键分配/重排）
- [BatchTagDialog.kt](file:///workspace/app/src/main/java/com/github/jing332/tts_server_android/compose/systts/list/BatchTagDialog.kt)（批量分配标签）
- 其他潜在调用点（grep `computeTagName` 确认）
