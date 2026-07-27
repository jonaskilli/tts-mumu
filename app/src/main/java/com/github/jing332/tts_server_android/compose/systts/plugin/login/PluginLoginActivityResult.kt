package com.github.jing332.tts_server_android.compose.systts.plugin.login

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

data class LoginData(val url: String, val binding: String, val description: String, val ua: String)

object PluginLoginActivityResult : ActivityResultContract<LoginData, String>() {
    override fun createIntent(
        context: Context,
        input: LoginData,
    ): Intent = Intent(context, PluginLoginActivity::class.java).apply {
        putExtra(PluginLoginActivity.ARG_LOGIN_URL, input.url)
        putExtra(PluginLoginActivity.ARG_BINDING, input.binding)
        putExtra(PluginLoginActivity.ARG_DESC, input.description)
        putExtra(PluginLoginActivity.ARG_UA, input.ua)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): String {
        return if (resultCode == PluginLoginActivity.OK)
            intent?.getStringExtra(PluginLoginActivity.RESULT) ?: ""
        else ""
    }
}