package com.github.jing332.tts_server_android.compose.systts.account

import android.os.Bundle
import androidx.activity.compose.setContent
import com.github.jing332.tts_server_android.compose.ComposeActivity
import com.github.jing332.tts_server_android.compose.theme.AppTheme

/**
 * Jet Hub 插件 UI 的「新建账号」登录宿主（10-10）。
 *
 * 为什么单独起一个 Activity：插件前端点「+新建账号」要的是「拿一个 loginUrl + 轮询」，
 * 而我们 13 个渠道的登录形态各不相同（WebView / 设备码 / 凭据直填 / 扫码 / 短信），
 * 这些流程在 `AccountPoolScreen` 里已经全部接好。与其在桥里重写一遍，不如把这个
 * 现成页面挂到一个独立 Activity 上——用户在这里走完任意渠道的登录，回到插件 UI 后
 * 账号列表自动刷新（桥的 login.poll 以「账号集合变化」为完成判据）。
 */
class JetHubLoginActivity : ComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                AccountPoolScreen(onBack = { finishAfterTransition() })
            }
        }
    }
}
