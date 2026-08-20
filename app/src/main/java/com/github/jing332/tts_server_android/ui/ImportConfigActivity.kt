package com.github.jing332.tts_server_android.ui

import android.content.ContentResolver
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.drake.net.utils.fileName
import com.github.jing332.common.utils.FileUtils.readAllText
import com.github.jing332.common.utils.longToast
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.ComposeActivity
import com.github.jing332.tts_server_android.compose.systts.LocalImportFilePath
import com.github.jing332.tts_server_android.compose.systts.LocalImportRemoteUrl
import com.github.jing332.tts_server_android.compose.theme.AppTheme
import com.github.jing332.tts_server_android.ui.systts.ImportConfigFactory
import com.github.jing332.tts_server_android.ui.systts.ImportConfigFactory.gotoEditorFromJS
import com.github.jing332.tts_server_android.ui.systts.ImportType


class ImportConfigActivity : ComposeActivity() {
    companion object {
        const val TAG = "ImportConfigActivity"
    }

    private var type = mutableStateOf("")
    private var url = mutableStateOf("")
    private var path = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val sheet = remember(type.value) {
                    ImportConfigFactory.getBottomSheet(
                        type = type.value,
                        onBadFormat = {
                            // type 非空：ttsrv 链接携带了未知类型，提示
                            if (type.value.isNotEmpty()) {
                                longToast(R.string.import_config_type_unknown_msg)
                            }
                            // 无论哪种情况（无效 scheme/空 URL/未知类型）本页都无内容可展示，
                            // 直接关闭，避免残留空白页
                            finish()
                        },
                    )
                }

                CompositionLocalProvider(
                    LocalImportRemoteUrl provides url,
                    LocalImportFilePath provides path
                ) {
                    sheet { finish() }
                }


            }
        }

        importConfigFromIntent(intent)
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        importConfigFromIntent(intent)
    }

    private fun importConfigFromIntent(intent: Intent?) {
        intent?.data?.let {
            when (it.scheme) {
                ContentResolver.SCHEME_CONTENT -> importFileFromIntent(intent)
                ContentResolver.SCHEME_FILE -> importFileFromIntent(intent)
                "ttsrv" -> importUrlFromIntent(intent)
                else -> longToast(getString(R.string.invalid_scheme_msg))
            }
        }
    }

    private fun importUrlFromIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            if (uri.scheme == "ttsrv") {
                val path = uri.host ?: ""
                val url = uri.path?.removePrefix("/") ?: ""
                if (url.isBlank()) {
                    longToast(getString(R.string.invalid_url_msg, url))
                    intent.data = null
                    return
                }

                type.value = path
                this.url.value = url

                intent.data = null
            }
        }
    }

    private fun importFileFromIntent(intent: Intent?) {
        if (intent?.data != null) {
            if (intent.data?.fileName()?.endsWith("js", true) == true) {
                val txt = intent.data?.readAllText(this)
                if (txt.isNullOrBlank() || !gotoEditorFromJS(txt)) {
                    longToast(R.string.js_file_type_not_recognized)
                }
                // JS 已交给编辑器（或提示无法识别），本页无内容可展示，直接关闭，
                // 避免从编辑器返回后残留空白页
                finish()
            } else {
                // 非 JS 文件：直接交给统一的自动导入流程（doAutoImport）识别和导入，
                // 不在此重复识别，识别不出时也由统一流程给出原因提示，与内部导入一致。
                // 注意：type 必须置为 LIST，底部面板由 type 决定——
                // ListImportBottomSheet(autoImport=true) 才会消费 LocalImportFilePath
                // 预设路径并自动识别真实类型（列表/插件/替换规则/朗读规则）；
                // 若不设置，getBottomSheet("") 返回 bad-format 占位，页面空白且不导入。
                path.value = intent.data!!.toString()
                type.value = ImportType.LIST.id
            }

            intent.data = null
        }
    }


}