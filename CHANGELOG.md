# 相对原版 hunyuan 分支的修改

## 角色管理栏
- 新增"运行朗读规则"快捷键：顶栏 PlayArrow 图标，选择规则后自动进入编辑器并运行
- 新增"仅界面模式"开关：顶栏 Switch，仅 mingwuyan 插件配置项存在时可开启，开启后将插件 UI 嵌入本栏
- 顶栏运行键与开关位置调整：运行键在前，开关在后，中间加 8dp 间距
- 开关无提示文字，只保留 Switch 控件
- 开关只管 ON，已开启时禁用点击，避免误触关闭
- 运行朗读规则后插件 UI 自动刷新（基于 ON_RESUME 生命周期检测 + reloadKey 机制）

## 子分组管理
- 新增子分组菜单：重命名、批量标签、音频参数、转为一级分组、移动子分组
- "移动子分组"对话框：主标题"移动子分组 (x/y)"，副标题"移动子分组到其他一级分组，注意区域可上下滑动"
- 删除"释放全部子分组到根目录"功能
- 删除"释放配置项"功能
- 子分组不置顶，禁止单条与子分组混放根目录
- 子分组释放时同时清除子分组名字（音频参数）

## 底部导航栏
- 改用 NavigationBar 降低高度，比原版 BottomAppBar 更紧凑
- 底部栏复位改为瞬间设置，消除切换页面时 300ms 动画与首次组合叠加导致的掉帧
- 移除页面切换时 heightOffset 复位动画（卡顿源）

## 性能优化
- 日志批量加载：从逐条 add（1500+ 次重组）改为 addAll 一次性加载
- HorizontalPager 配置优化：beyondViewportPageCount 调整为 1，保留相邻页状态避免重建
- 列表项展开/折叠性能优化
- 选中选项（分组/朗读规则/插件/替换规则）消除卡顿，多选用 derivedStateOf 减少重组
- 各界面拖拽排序改 IO 线程批量更新，避免主线程 DB 阻塞
- 导入（发音人/朗读规则/插件/替换规则）改 IO 线程异步执行
- ManagerActivity 与 MainActivity 的 onSave 回调改 IO 线程
- 主线程数据库操作迁移至 IO 线程（分组删除/重命名/展开/启用切换等）

## 配置导入
- JSON 导入流程优化：识别并导入时不再退出界面，保持在导入界面
- 导入弹窗提示优化

## 朗读规则
- 列表项支持内联展开编辑 name/id/author/version，无需进代码编辑器
- 新增运行键一键自动调试
- 代码编辑器新增"从文件导入"菜单项，可从本地 JS 文件覆盖当前代码
- 支持单条导出
- 修复朗读规则导出无扩展名
- 删除调试日志：[测试] 朗读规则引擎初始化完成、[直接测试] globalSpeechRuleLogListener 可用、[脚本返回] 脚本返回值

## 其他修复
- 同标签发音人去重，启用新项时自动禁用同 tag 旧项
- 修复本地 TTS 无法在编辑界面试听
- 修复插件 TTS 附加数据不更新（解决 Azure 插件风格和角色变化问题）
- 修复 Android 8 及以下版本的备份问题
- 修复 OkHttp Response 与 InputStream 资源泄漏
- 修复 LaunchedEffect 内嵌套 scope.launch 的协程泄漏
- 修复 ReplaceRule/Replace SortDialog 主线程阻塞
- 修复 AnimatedVisibility 在 Box 作用域被解析为 ColumnScope 重载，改用全限定名调用
- 修复 SpeechRuleManagerScreen if 条件缺括号导致编译失败
- 修复 gradle-wrapper.jar 缺失导致 CI 无法打包
- 修复 settings.gradle pluginManagement 缺 google() 导致 CI 找不到 Android 插件
- 修复 KSP 插件仅在 Maven Central，阿里云镜像未同步，补 mavenCentral() 仓库
- CI 环境改用官方仓库，阿里云镜像仅本地开发使用
- CI 签名初始化用 printf 替换 echo，避免末尾换行符破坏 jks 二进制
- CI Init Signature 增加空 Secret 校验，避免白跑 14 分钟编译才发现签名失败
- CI Release 改用默认 GITHUB_TOKEN 并加 contents:write 权限，避免创建 Release 403

## 新功能
- 导出大文件改用临时文件 + FileProvider 方案，绕开 Binder 1MB 限制，避免大配置导出崩溃
- 支持由调用者通过 API 指定发音配置
