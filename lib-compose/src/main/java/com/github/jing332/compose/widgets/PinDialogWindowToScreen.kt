package com.github.jing332.compose.widgets

import android.view.Gravity
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

/**
 * 把 Compose Dialog 窗口钉成「全屏 + 底部对齐」。
 *
 * 09-17 实机根因（09172233 包像素扫描实锤）：只设 `usePlatformDefaultWidth=false` +
 * `decorFitsSystemWindows=false` 时窗口高是 WRAP_CONTENT，窗口管理器会把整窗排版到
 * **屏幕下方约一个导航栏高**（窗口底伸出屏幕外）——面板 92% 比例、按钮行贴底全部跟着
 * 出屏，「保存」只剩个头，且内容区底部留出一段按屏幕坐标算不出来的空档。
 * 之前四轮改比例（估算预留→BoxWithConstraints→fillMaxHeight）全是无效手术，
 * 因为锚点（窗口）本身是歪的。
 *
 * 显式 `MATCH_PARENT×MATCH_PARENT + Gravity.BOTTOM` 后窗口 = 屏幕本体：
 * fillMaxHeight / navigationBarsPadding / imePadding 全部按真实屏幕生效。
 * 必须在 Dialog 的 content 里调用（LocalView 要取弹窗自己的 view 才能拿到窗口）。
 */
@Composable
fun PinDialogWindowToScreen() {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        window.setGravity(Gravity.BOTTOM)
    }
}
