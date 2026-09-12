# 项目记忆（E:\TTS）— 精简版 2026-09-12

## 工程主线
- 仓库 `E:\TTS\tts-mumu`，分支 **mumu_tts_github**（唯一分析依据）；Android TTS 应用，Compose+Kotlin，JRead 插件生态
- 用户：目目，中文，深度使用者兼维护者，日常 1-2 倍速听书
- 技术文档归口 `E:\TTS\技术文档\`（17 篇，含 MD3 规范/音频参数三层体系/字段关联架构等——**UI 细节定案以文档为准，此处不重复**）；仓库内技术文档目录已删（733b7ad），旧交接文档里 `tts-mumu/技术文档/*` 路径全部失效
- CI 触发有 paths 白名单，**纯文档改动不触发编译验证**，别干等

## 硬约束（违反即返工）
1. 本机不编译（无 JDK），改完直接推 CI；adb 可用（E:\platform-tools-latest-windows\...\adb.exe），可真机只读诊断
2. 发布分工：AI 提交+推送+等 CI；打 tag 触发打包默认用户来（规格 1.26.月日时分，tag 推送优于 workflow_dispatch）
3. 推送节奏：迭代期只 commit 不 push，等用户说「推」；CI 挂等 `gh run watch <id> --exit-status --interval 30`
4. CI 状态要跟手：用户问立刻 `gh run view/list` 直查
5. 记忆时机：迭代期不逐轮写，只在收尾/定稿写
6. 拒改播放链插件 JS；UI 类插件（角色管理 v10）可改
7. 重复反馈零容忍：重报问题追根修到底
8. 附件传不到 → 让用户存 E:\TTS\ 再报文件名
9. **UI/交互先提案后动手**；提案默认**最小改方案**+示意图+大白话，禁术语；结构级重写默认不做
10. **MD3 官方默认**（全文见《技术文档\MD3 UI 规范约定.md》）：官方组件形态、字号走 typography token、颜色走 colorScheme、4dp 网格、触摸≥48dp、AlertDialog confirmButton 必选槽、提交前 grep 自查。手调字号白名单/宽弹窗 7 处等例外见文档
11. **文案精简**：弹窗/菜单解释能删就删；标题须涵盖功能全部维度

## 关键教训（数据口径）
- **匹配口径终版=tag id**：fayinren.json/characterRecords 的 voice 字段存 tag id（"女青年01"）非引擎 voice；tagName 是显示名不参与匹配。改 tag/tagName/voice 匹配前必读 `tts配套文件/多角色朗读*.js` +《字段关联架构》
- **gengxin.json 信箱**：规则每轮朗读开头消费（替换内存角色表后删）；面板 rebind 必须同步写，否则规则内存旧数据会覆盖面板修改
- TtsRepository 性别兜底：男*→duihuaA、女*→duihuaB、未知→duihuaA（duihua 弃用）
- voice_marks.json：`{tag:["like","neutral","bad"]}`，在 `Download/chajian/<tagRuleId>/` 下；ttsrv.readTxtFile 的 getFile 同样落该目录

## 已否决、勿再提
M3 质感对齐/配色自动切换单色图标/app 顶栏🎨/切逗号/bingfa/JRead 本体坏缓存自愈/备份合并主键撞车防御/删 tagName/**插件接管判定与开关**（一律本机处理，勿再提开关/路由表/自动探测）/**音频参数弹窗折叠手风琴**；「弹窗顶部发音人行/终值行/试听⚡」已恢复勿按裁撤处理；卡片⋮保留总弹窗形态

## 近况
- 09-12：第四批 25 笔已推（c244e0e，CI 绿）；候选行 ⋮ 角色管理互通本地 3 笔待推（59835cf VoiceMarksFile+removeFromPool / d34586e 行内平铺+v10标记缓存修复 / 99595b9 **删除只删试听那一条**——删除类默认删"当前指向的那一条"不放大）；非绑定分支未加此功能
- 历史记忆库：`E:\TTS\.zcode\cli\memories\projects\tts-2d9b00a952de759c\memory\`（MEMORY.md 索引+分主题文件，先读索引）；会话原文 `E:\TTS\.zcode\cli\db\db.sqlite`（session/message/part）
- 待拍板（勿擅自实施）：日志面板终值行 top=4dp vs 配置项弹窗 top=2dp/bottom=4dp 未统一
