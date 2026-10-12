package com.github.jing332.common

/**
 * 强调绿（用户 10-13 令：「关于那个成功的绿色，以及咱们这个软件里边使用到这个颜色的绿色
 * 都应该改成方框对勾那个强调绿，同时它也是朗读规则日志插件日志的绿色」）。
 *
 * 取值来源：M3 `Checkbox` 勾选态填充色。绿色主题下勾选框实测 = `#01A050`，
 * 反解到 sRGB 源码值 ≈ `#109D58`（＝ `Color2.green_seed`，即绿主题 primary）。
 * 截图管线把 sRGB 原值重编码过，所以截图值不等于源码值——以源码 `#109D58` 为准。
 *
 * 这里只放 **Long ARGB**：lib-common 不依赖 compose-ui，Compose 侧用 `Color(ACCENT_GREEN)`。
 * 全站「成功 / 通过 / 正向」语义统一取这一支，勿再各写各的绿（历史上岔出过
 * `#4CAF50` / `#2E7D32` / `#2E7D46` 三支）。
 */
const val ACCENT_GREEN: Long = 0xFF109D58
