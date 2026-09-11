# MD3 UI 规范约定（mumutts，2026-09-11 起生效）

> 背景：2026-09-09~09-11 三轮"完全 MD3 版"打磨后，用户裁定**所有 UI 一律按官方标准来，自定义观感不再保留**。
> 本文档是长期约定：**今后新增/修改的任何界面，必须先满足本规范，不允许再引入新的自定义观感**，避免反复回调。
> 维护：AI 在每次 UI 改动时对照本清单自查；违规项须在提交说明中列出理由。

## 一、组件：只用官方，禁止自绘视觉

- 一律使用 `androidx.compose.material3` 官方组件的**默认形态**（AlertDialog、ModalBottomSheet、Slider、SegmentedButton、Checkbox、Switch、FilterChip、TabRow、SearchBar、DropdownMenu、CircularProgressIndicator 等）。
- **禁止自绘视觉**：不手画轨道/ thumb/胶囊/描边容器，不叠 `containerColor`/`shape`/`fillMaxWidth(0.9x)` 覆盖官方默认。
- 特例许可（有功能理由，需注释说明）：
  - 手势包装：官方按钮 clickable 会吃掉长按手势时，可用 pointerInput 包装，但视觉仍用官方按钮槽位（先例：混元太极页 COutBtn）。
  - 兼容垫片：AppDialog 等签名垫片**永远不再加自定义样式**——要改观感走主题或等 MD3 演进。
- 新需求先找官方组件；官方没有的（如加载弹窗）用官方件组合（AlertDialog + CircularProgressIndicator），组合方式在注释里写明。

## 二、字号/字重：只用 Typography 标尺（MD3 有此标准）

MD3 类型标尺 15 个 token，**禁止硬编码 sp**（如 13sp、14.sp 直写）：

| token | 用途（本 app 实际对应） |
|---|---|
| `headlineSmall` | 弹窗标题（官方 AlertDialog 默认 24sp） |
| `titleLarge` / `titleMedium` / `titleSmall` | 页面大标题 / 卡片标题 / 行标题 |
| `bodyLarge` / `bodyMedium` / `bodySmall` | 正文 / 弹窗正文（官方 text 槽默认 16sp）/ 次要说明 |
| `labelLarge` / `labelMedium` / `labelSmall` | 按钮 / 小标签 / 12sp 微标签 |

- 取字号时一律 `MaterialTheme.typography.xxx`，不许 `fontSize = 14.sp`。
- **字重随 token，禁止静态 `FontWeight.Bold` 覆盖**（2026-09-11 用户裁定）：title/label 系官方即 Medium(500)，强调层级换更重的 token（如 bodyMedium→titleSmall），不给正文叠 Bold。允许的两类例外：①**选中态** `if (selected) FontWeight.Bold else Normal`（状态表达，列表排序/标签切换等 4 处先例）；②官方字重值 `FontWeight.Medium`（500，如标签卡片）。
- 整页缩放（如混元页 -2sp）属于特例许可，勿扩散。

## 三、颜色：只用 colorScheme 槽位

- 一律 `MaterialTheme.colorScheme.primary/surface/onSurface/...` 槽位取色。
- **禁止硬编码 `Color(0x...)`**（`theme/Color2.kt` 调色板定义与 systemBars 系统色除外）。
- 现存两处功能层豁免（滚动条灰、代码折叠 gutter），不新增。

## 四、间距：4dp 基线网格（MD3 无 spacing token，网格是唯一软标准）

- 所有 padding/spacing 取 **4 的倍数**：4 / 8 / 12 / 16 / 24 / 32…；**禁止** 2dp、3dp、5dp、6dp、7dp 等网格外值。
- 常用档位（减少随意性）：
  - 组件内部留白：8dp；相关组件间距：8~12dp；区块之间：16~24dp；页面水平边距：16dp。
  - 弹窗内边距用官方自带 24dp，**不叠加**（AppDialog 垫片已撤自定义 padding，新弹窗照此）。
- 特殊压紧需求先考虑换排版 token（labelSmall 等），而不是挤间距。

## 五、形状/圆角：用 shapes token 或官方默认

- 官方组件自带圆角（弹窗 28dp etc.），不覆盖。
- 自定义容器圆角只用 `MaterialTheme.shapes.small/medium/large`（8/12/16dp）。

## 六、触摸目标：≥48dp

- 官方默认即 48dp，**不缩**（32dp 复选框时代已废弃）；图标可用 24dp 视觉 + 官方默认触摸区。

## 七、提交前自查（AI 作业规则）

UI 改动提交前对改动文件 grep 一遍：

```
grep -n "\.sp\b" <改动文件>       # 应只剩 typography 引用，无直写 sp
grep -n "Color(0x" <改动文件>     # 应无（豁免文件除外）
grep -n "containerColor\|shape = RoundedCornerShape\|fillMaxWidth(0\." <改动文件>  # 弹窗类应为空
```

不符合且无注释许可理由的，当场修掉再提交。

## 八、历史包袱备忘

- 已裁撤勿改回：胶囊轨道滑条、软槽浮块、32dp 触摸区、16dp 弹窗圆角、白底弹窗、自绘渐变转圈、SegmentedTextToggle/SoftSegmentedTextToggle 自绘画法（0b36ac8 → 7634e23 链路）。
- 死代码占位（零调用，勿 git rm）：ButtonToggleGroup、LoadingAnimation、MoveToSubGroupDialog、SegmentedTextToggle。
- 待拍板遗留：日志面板终值行 4dp vs 配置弹窗 2dp（网格违规项，统一时取 4dp）。
