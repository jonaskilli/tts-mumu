package com.github.jing332.tts_server_android.compose

import androidx.compose.ui.unit.dp

/**
 * 列表左右基准线（v3 边距统一定稿）：M3 compact 窗口官方 gutter = 16dp。
 *
 * - 内容页（日志 / 角色管理）：容器 contentPadding 与裸内容（行文字/圆点/提示行）共同落这条线，
 *   行内不再叠加水平 padding；框（卡/描边框）缘与 16 线同面，从属靠框内 padding（4dp 起步）表达。
 * - 面板页（系统TTS）：整页连片贴边（0）豁免——行自带底色/分隔连成一片，且无裸文字贴边，
 *   页内最贴屏幕的可见元素（折叠箭头/勾选框）自带内在留白。
 * - 卡片层级页（密钥管理）：容器已是 16；组头→卡→名字三级从属缩进（0920 实机校准）整体保留，不动。
 */
internal val ListGutter = 16.dp
