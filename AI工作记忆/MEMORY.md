# 项目记忆（E:\TTS）

## 历史记忆库位置（重要）
zcode 桌面端的完整历史记忆已随目录迁移到本工作区：
- 记忆目录：`E:\TTS\.zcode\cli\memories\projects\tts-2d9b00a952de759c\memory\`
  - `MEMORY.md` 是索引（20 条），其余是分主题记忆（备份体系/音频参数/角色管理/插件桥接/各插件行为等）
  - 需要"前情"时先读索引再定点读文件，不要全读
- 会话数据库（可查任意历史对话原文）：`E:\TTS\.zcode\cli\db\db.sqlite`
  - 表 `session`（id/title/time_updated）、`message`(data 含 role)、`part`(data.type=text 是正文)
  - 同一份对话另有 jsonl 日志：`E:\TTS\.zcode\cli\log\zcode-YYYY-MM-DD.jsonl`（运行日志，非对话）

## 工程主线
- 仓库：`E:\TTS\tts-mumu`，分支 **`mumu_tts_github`**（唯一分析依据；`legacy-hunyuan_old_github` 仅历史背景，不可用于推导当前设计）
- Android TTS 应用，Compose + Kotlin，JRead 插件生态，十几个声音插件
- 用户身份：目目，中文，深度使用者兼维护者，日常 1-2 倍速听书，不开转发器

## 文档归口（2026-09-09 起，变动）
- **技术文档已迁出仓库**，统一放 `E:\TTS\技术文档\`（**17 篇**，2026-09-10 新增《音频参数三层体系与调参UI.md》总纲：其余为兜底体系架构/字段关联架构/日志栏架构/角色列表标签机制/朗读规则系列/响度均衡/替换规则/插件形态与播放链/插件音色分类映射/采样率/播放管线/插件提交判定等）
- 仓库内 `tts-mumu/技术文档/` 已删除（commit 733b7ad）；**旧交接文档里 `tts-mumu/技术文档/*.md` 路径全部失效**，一律以 `E:\TTS\技术文档\` 为准
- 根目录 `trae_session-*.md` / `trae-会话交接-*.md` 是跨工具交接文档（在仓库外）
- **CI 触发条件**：`test.yml` 有 paths 白名单（lib-*/app/src/build-logic/gradle 等），**纯文档改动不触发编译验证**，别干等

## 硬约束（违反即返工）
1. **本机不编译**：无 JDK/JAVA_HOME（java/javac 均找不到），禁止本地 gradlew 构建，改完直接推 CI 验证。**但 adb 例外且可用（2026-09-10 实测）**：`E:\platform-tools-latest-windows\platform-tools\adb.exe`（1.0.41 / 37.0.1），daemon 能正常起，可做真机只读诊断（logcat/screencap/dumpsys/装 APK），**不能本地打包**；另有 `E:\AndroidSDK`（build-tools/cmdline-tools/platforms）但无 JDK 故不可用于构建
2. **发布分工**：AI 只提交+推送+等 CI；打 tag/触发打包默认用户自己来（用户明示"编译成功就触发打包"时可代打，规格 1.26.月日时分）。**触发方式首选 tag 推送**（release.yml `on.push.tags: '1.26.*'`）——用户 2026-09-11 明示"通过提交触发打包我喜欢，能看到版本号"，优于 workflow_dispatch（手动触发版本号不直观、且默认还带 dev 共存版）。注意 tag 名与 Release 版本名可能差 1 分钟：versionName 是构建时刻 `1.26.${MMddHHmm}`，与 tag 无关
3. **推送节奏**：连续问题只 commit 不 push，等用户说「推」；CI 用 `gh run watch <id> --exit-status --interval 30` 挂后台
4. **CI 状态要跟手**：用户问就立刻 `gh run view/list` 直查，不要回"在等通知"；定时自动化盯 CI 已被否决
5. **记忆时机**：迭代期不逐轮写记忆，只在收尾/定稿时写
6. **拒绝改造播放链插件 JS**（数量多）；UI 类插件（角色管理 v10）可改
7. **重复反馈零容忍**：用户重报的问题必须追根修到底，并说明上次为何没修全
8. 附件常传不到 → 让用户存 `E:\TTS\` 再报文件名
9. **UI/交互改动先提案后动手**（2026-09-09 用户明示）：先出方案（可带示意图/预览），用户同意后才改代码；不能当场直接改。非 UI 的 bug 修复不受此限。**09-12 深夜强化（两次返工教训：排序弹窗、选择分组）**：提案**默认只给最小改方案**（原版结构+最小增量），结构级重写/去控件重排默认不做、除非用户明确要；方案必须配示意图+大白话，禁用"守卫/回调/软槽"等术语
10. **MD3 规范约定（2026-09-11 用户裁定，永久有效）**：全 app UI 一律官方 MD3 默认，规范全文见《E:\TTS\技术文档\MD3 UI 规范约定.md》——要点：①组件只用 material3 官方默认形态，禁自绘视觉/禁叠 containerColor/shape/宽度覆盖（AppDialog 垫片永不加样式）；②字号只用 typography token 禁硬编码 sp；③颜色只用 colorScheme 槽位禁 Color(0x)；④间距 4dp 网格（禁 2/3/5/6/7dp）；⑤触摸区 ≥48dp；⑥AlertDialog 的 confirmButton 是必选槽（无按钮传空槽）；⑦UI 改动提交前 grep 自查硬编码 sp/Color/弹窗覆盖。特例许可须注释说明理由（先例：混元页长按手势按钮）
11. **文案精简（2026-09-12 用户明示，永久有效）**：菜单/弹窗里的解释要**精简准确**——长括号解释（如「（把配置项改指向另一个插件，发音人等字段保持原值）」）是减分项，能删就删；标题必须涵盖功能全部维度（反例：「批量修改来源字段」只涵盖"来源插件"一类，实际能改 启用/采样率/来源三类 → 改「批量修改配置」）。改文案仍属 UI 改动，走"先提案后动手"。

## 已否决、勿再提的提案
M3 质感对齐 / 配色自动切换 / 单色图标 / app 顶栏🎨 / 切逗号 / bingfa / JRead 本体侧坏缓存自愈 / 备份合并插入主键撞车防御 / 删 tagName / **插件接管判定与「由插件处理」开关**（2026-09-10 用户裁定：软件分辨不出插件是否特殊型，不再让使用者或维护者去识别；一律本机处理，只要能播放+能调速+能调音量即可；三层调节本来就全生效，勿再提议加开关/路由表/自动探测）/ **音频参数弹窗折叠手风琴**（09-10 当日试当日撤，弹窗与日志面板排版必须同款）——注意：**「弹窗顶部发音人行、终值行、试听行⚡入口」已于 09-10 晚被用户反转，三者全部恢复（9dbf0e1），勿再按"已裁撤"处理**；卡片⋮保留总弹窗形态是用户明确要求

## 音频参数定案（09-10 收官 + 09-11 深夜大修 52d492c，详见《技术文档/音频参数三层体系与调参UI.md》）
- **全部维度恒显示、恒可调、恒走本机 Sonic**；终值 = 配置×插件×全局（`multiplyParam`，FOLLOW=1）；接管判定废除（dc5dd22），音高也进插件/全局层；盘点 28 插件仅背景
- **两级分段（09-11 修订）**：**第一级（父，仅日志面板有）「更换发音人/音频参数」= 重形态描边胶囊 + 宽度随文字，字号已降回 14sp**（09-11 用户指认 16sp 太大；父子区分只靠形态：父=描边、子=软槽）。**第二级（子，三处共用）= `SoftSegmentedTextToggle` 无描边浅底槽（surfaceVariant/圆角8dp/高36dp/等分撑满），09-11 起选中项浮起 primaryContainer 胶囊 + onPrimaryContainer 粗体**（纯文字变色"看不出选了哪个"被否）。**下划线标签页形态 09-10 已否，勿再改回**。`SegmentedTextToggle` 可选参数 labelFontSize(默认14sp)/minHeight(默认40dp) 不动
- **调参 UI 三入口**：①卡片⋮弹窗=平铺（顶部发音人+▶+终值行；主体=维度软槽+该维三层滑杆+重置/应用；软槽顶距 8dp）②编辑页「音频参数」=`AudioParamsDimRows` **同款软槽一行三项**（09-11 定稿：16sp、槽内=维度名+该维终值 toParamText、**选中=正在展开浮起胶囊、默认展开语速、永不全收起（方案A：浮块常驻即可点性示范，与弹窗"永远有一维选中"行为一致）**；顶距 12dp 融入试听行下方；展开区=该维三层滑杆+重置/应用**无底框平铺**，终值行与▶试听一律不放，试听用 `AuditionTextField` 的 🎧）。三处接线：PluginTtsUI/LocalTtsUI/TtsEditContainerScreen。旧三键 FilterChip 行与单维弹窗两版已撤销（文件留空占位，禁 `git rm`）③日志快捷面板平铺不变
- **滑杆轨道（09-11 定案 52d492c，推翻 09-10 晚细线）**：`LabelSlider` 轨道改**胶囊画法 10dp**（圆角胶囊+未选中段尾端小圆点+竖条 thumb 4×22dp）——3.5dp 纯细线被否（"看着差太多、不如原来美观"）；标签仍 14sp、加减 48dp/22dp 图标、三层行距 4dp；**labelMinWidth 44dp 已撤（0177c22）**——定宽是 3 字「发音人」时代遗留，改「本项」后天然等宽，撤掉省 16dp 死空间，标签列不定宽；**层名「发音人」改「本项」**（audio_params_tag_config，英文 Config）；**改尺寸只能走 LayerSlider 参数，禁止改 LabelSlider 默认值**（其余 32 处滑杆不受影响）
- **间距定案（09-11 上午，7a676f3/f752e9e/0177c22）**：日志面板左右统一 16dp；终值行上下各 4dp；软槽→滑杆 top=4dp（三处一致）；顶部「当前发音人」行重排=小标签独占一行、**▶ 试听键与发音人名同一行**（否掉圆形填充键，保持文字键；用户明确要同行）、名字加 Ellipsis；日志面板 ▶ 试听**维持硬编码**不接试听文本框；英文层名=Config（Item 被用户指正）
- **按维应用**=该维三层一起落库（配置层双写页面内存防覆盖、插件层失效卡片缓存、全局写 SysTtsConfig、notifyUpdateConfig 立即生效）
- **试听链草稿覆盖**（994080b）：`TaggedTtsPreviewPlayer.play` 加 pluginParamsOverride/globalParamsOverride 可选参数，▶=三层草稿全带入，不用先应用即听完整终值；混响已接入试听链（同一 ReverbAudioProcessor，单声道/尾音截断预览可接受）；所有试听同念 AppConfig.testSampleText（清空回落默认句；旧装机存的默认文本需手动清框）
- 首发包 tag `1.26.09100404`（CI 34398533612 success 后代打，用户明示授权）

## MD3 化已完结（09-11 终版，全推送 CI 绿）
- **今日 8 笔收官链**：77e955b（滑条/分段/复选框）→ 5a9d7ff（父级分段）→ 9e51e16（8 处漏网弹窗）→ 435afc9（confirmButton 修复）→ 7634e23（LoadingDialog）→ f92b460（字号 11 处归 token）→ e0baff6（静态 Bold 12 处撤）→ 96e76d2（圆角 6 处归 shapes token）。用户已自打包（指向 96e76d2）。
- **现行唯一标准：《E:\TTS\技术文档\MD3 UI 规范约定.md》**（硬约束第 10 条）。09-11 旧定案（胶囊轨道 10dp/软槽浮块/32dp 触摸区/labelFontSize/字号随手值/静态 Bold）全部作废。
- 保留的例外（有裁定/理由，勿再动）：± 加减键、混元页长按按钮+12sp、选中态 Bold×4、FontWeight.Medium、豆绿主题、网格外间距 ~30 处、elevation 2 处、SettingsSearch 填充式搜索框。
- **列表类弹窗宽度=宽版（09-11 晚 21:39 用户终裁，`d56d2e4`）**：BatchTagDialog 0.92 / GroupEditContentDialog 0.9 / GroupTreePickerDialog 0.9 / ListManagerScreen 四处 0.92，用 `properties=DialogProperties(usePlatformDefaultWidth=false)` + `Modifier.fillMaxWidth(0.9x)`；**圆角维持官方 28dp**。其余弹窗（走 AppDialog 垫片的 29 处，含日志面板/各确认弹窗）维持官方默认宽度——它们从未宽过。**勿再把这 7 处当"自定义样式"撤掉**。
- **手调字号白名单（09-11 终裁"一并回原版"，3ef36f7）——这些处的硬编码 sp 是用户偏好的原版观感，勿再 token 化**：GroupItem 18sp、SubGroupHeader 17/16/16sp、list/Item 描述行 13sp、replace/Item 15sp、SettingsWidgets 副标题 15sp、PluginManagerScreen 名称与提示 15sp、AuditionDialog 试听分类 Chip 12sp、LabelSlider 标签 14sp（2459691 拍板）。
- **09-11 晚终裁：整体回退 1400 观感**（目标 commit `5a9d7ff` = 包 1.26.09111400）——用户判定后续"查缺补漏大力改"改坏了。逐文件核对后：**1400 与现状主体观感本就一致**（今晚多笔就是往回捞字号字重），实际只差 2 项，已撤（`9b80506`）：①圆角 6 处 token→手调值（LogScreen 回 6dp，其余数值本就相同）；②主题层 titleMedium=700→官方 500，卡片标题/删除确认正文改回**自身** `fontWeight=Bold`（=1400 写法）。**「主题层 titleMedium=700」就此作废，勿再改回**。
- 回退时按用户拍板**保留**：9 处弹窗官方化（9e51e16）、5 项缺陷修复（卡片长标题穿透/加载弹窗居中/搜索匹配配置项名/0项分类隐藏/候选列表屏高40%）、4 项微调（父级分段均分撑满/滑杆手柄24dp+标签14sp/搜索提示placeholder/弹窗选择列表14sp）。
- **21:32 字号补刀（`3ef36f7`）**：f92b460 撤的 11 处手调 sp 全部回原版（设置页 15sp、插件管理 15sp×2、试听 Chip 12sp；"参数区"=AudioParamsDimensionSection 只删失活 import 无实码；SpeechRuleEditScreen `labelLarge.copy(14.sp)` 零视觉差保留不写回冗余）。**手调字号=原版观感，白名单见上，勿再收 token**。

## 待拍板（用户未确认，勿擅自实施）
- 日志面板终值行 `padding(top=4.dp)` vs 配置项弹窗 `top=2.dp/bottom=4.dp`（差 2dp，小事未统一）。

## 编辑页问答/标签机制补充（09-11，52d492c）
- **内心独白标签还原**：勾选=记下原标签再设 INNER_THOUGHT_TAG；取消=还原原标签（不再清空）。局限：勾选→保存→退出→重进再取消，原标签无从得知，回落清空（remember 会话内）
- **问号解说**：作为备用（文案已精简）/心声混响/内心独白 三处问号弹窗（values + values-zh 各一份，新增 systts_reverb_help*、systts_inner_thought_help*）；复选框方块视觉偏移 -15→-10dp、文字左拉近 8dp、作为备用问号撤 +12dp 偏移

## 已结案索引
- 原「日志快捷面板换发音人+调语速音量」「三层音频参数可视化」09-07~09-08 结案；试听链三层草稿覆盖 09-10 结案；滑杆尺寸 09-10 晚结案（9dcd115+e755b60）、09-11 深夜改胶囊 10dp（52d492c）；编辑页三项行形态 09-11 结案（软槽方案A，FilterChip 已废）。
