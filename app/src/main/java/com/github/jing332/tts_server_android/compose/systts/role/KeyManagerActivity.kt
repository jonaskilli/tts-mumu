package com.github.jing332.tts_server_android.compose.systts.role

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.github.jing332.tts_server_android.compose.ComposeActivity
import com.github.jing332.tts_server_android.compose.theme.AppTheme

/**
 * 密钥管理独立页面（原 Dialog 左右各留 24dp、内容区仅约 272dp 太窄 →
 * 改为全屏页面，照「替换管理 / 插件管理 / LibrariesActivity」同一模式）。
 *
 * 角色管理页顶栏 🔑 入口 startActivity 进入；返回键/左上返回箭头 finish 回列表。
 * 页面内子弹窗（新增 / 改名 / 接口表单 / 拉取模型 / 导入）仍是 Dialog，不受影响。
 */
class KeyManagerActivity : ComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tagRuleId = intent?.getStringExtra(EXTRA_TAG_RULE_ID).orEmpty()
        setContent {
            AppTheme {
                KeyManagerScreen(
                    tagRuleId = tagRuleId,
                    onBack = { finishAfterTransition() },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_TAG_RULE_ID = "tag_rule_id"

        fun start(context: Context, tagRuleId: String) {
            context.startActivity(
                Intent(context, KeyManagerActivity::class.java)
                    .putExtra(EXTRA_TAG_RULE_ID, tagRuleId)
            )
        }
    }
}
