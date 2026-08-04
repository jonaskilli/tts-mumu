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
- `TagNameUtils.computeTagName` —— 待定位（tagName 计算入口）
- `PluginDescriptor.kt` —— 配置列表显示
- `SpeechRuleConfig.kt` —— 已新增 `lastRoleSig`（自动刷新签名，见关联文档）
