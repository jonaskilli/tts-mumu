package com.github.jing332.tts_server_android.compose.systts.account

import android.os.Bundle
import androidx.activity.compose.setContent
import com.github.jing332.tts_server_android.compose.ComposeActivity
import com.github.jing332.tts_server_android.compose.theme.AppTheme

/**
 * Jet 页「+ 新建账号」登录宿主（10-10 轻量化+直连，用户定稿流程）：
 * Jet 页选中的供应商 → 新建 → **直进该渠道登录流程**（不弹渠道选择框）→
 * 成功回 Jet 页（账号区直接显示）。
 *
 * 旧实现 setContent { AccountPoolScreen } 拉起整个原生池页——分组头/签到/续期全在，
 * 与 Jet 页（WebView 插件版）两层 UI 打架（用户截图吐槽）。现在只渲染登录链
 * （JetHubLoginHost），全量管理仍在设置入口的账号池页。
 *
 * EXTRA_PROVIDER：Jet 页当前选中的供应商（app 侧 id，如 codebuddy/workbuddy）。
 * 缺省（外部直接拉起本 Activity）时 JetHubLoginHost 退回渠道选择弹窗。
 */
class JetHubLoginActivity : ComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val provider = intent.getStringExtra(EXTRA_PROVIDER).orEmpty()
        setContent {
            AppTheme {
                JetHubLoginHost(
                    context = this,
                    initialProvider = provider,
                    onFinished = { finishAfterTransition() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_PROVIDER = "jet_login_provider"
    }
}
