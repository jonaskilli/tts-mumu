package com.github.jing332.tts_server_android.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.github.jing332.common.utils.grantReadWritePermission
import com.github.jing332.common.utils.getBinder
import com.github.jing332.common.utils.toast
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.ComposeActivity
import com.github.jing332.tts_server_android.compose.theme.AppTheme
import com.github.jing332.tts_server_android.help.ByteArrayBinder
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import com.drake.net.utils.withMain

class FilePickerActivity : ComposeActivity() {
    companion object {
        const val KEY_REQUEST_DATA = "KEY_REQUEST_DATA"
    }

    private lateinit var requestData: IRequestData

    private val reqSaveFile: RequestSaveFile
        get() = requestData as RequestSaveFile

    private val reqSelectDir: RequestSelectDir
        get() = requestData as RequestSelectDir

    private val reqSelectFile: RequestSelectFile
        get() = requestData as RequestSelectFile

    private lateinit var docCreate: ActivityResultLauncher<String>

    private val docTreeSelector =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
            it?.grantReadWritePermission(contentResolver)
            resultAndFinish(it)
        }

    private val docSelector =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) {
            it?.grantReadWritePermission(contentResolver)
            resultAndFinish(it)
        }

    private fun resultAndFinish(uri: Uri?) {
        setResult(RESULT_OK, Intent().apply {
            putExtra(KEY_REQUEST_DATA, requestData)
            data = uri
        })
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lp = window.attributes
        lp.alpha = 0.0f
        window.attributes = lp

        requestData = intent.getParcelableExtra(KEY_REQUEST_DATA)!!

        if (requestData is RequestSaveFile) {
            docCreate =
                registerForActivityResult(ActivityResultContracts.CreateDocument(reqSaveFile.fileMime)) { uri ->
                    if (uri == null) {
                        cleanupTempFile()
                        finish()
                        return@registerForActivityResult
                    }
                    uri.grantReadWritePermission(contentResolver)
                    lifecycleScope.launch(Dispatchers.IO) {
                        kotlin.runCatching {
                            // 流式复制，避免一次性载入大文件到内存
                            saveToUri(uri)
                            toast(R.string.save_success)
                        }.onFailure {
                            displayErrorDialog(it)
                        }.onSuccess {
                            withMain {
                                cleanupTempFile()
                                finish()
                            }
                        }
                    }
                }
        }

        setContent {
            AppTheme { }
        }

        doAction()
    }

    private fun doAction() {
        when (requestData) {
            is RequestSaveFile -> {
                // 优先使用临时文件 URI；向后兼容 ByteArrayBinder
                if (reqSaveFile.fileUri == null) {
                    val binder = intent.getBinder()
                    if (binder is ByteArrayBinder) {
                        reqSaveFile.fileBytes = binder.data
                    }
                }
                saveFile()
            }

            is RequestSelectFile -> selectFile()
            is RequestSelectDir -> selectDir()
        }
    }

    /**
     * 流式复制源数据到目标 URI。
     * 优先从临时文件 URI 读取；向后兼容直接写 fileBytes。
     */
    private fun saveToUri(targetUri: Uri) {
        val srcUriStr = reqSaveFile.fileUri
        if (srcUriStr != null) {
            val srcUri = srcUriStr.toUri()
            contentResolver.openInputStream(srcUri).use { input ->
                contentResolver.openOutputStream(targetUri).use { out ->
                    if (input != null && out != null) input.copyTo(out)
                }
            }
        } else {
            val bytes = reqSaveFile.fileBytes
            if (bytes != null) {
                contentResolver.openOutputStream(targetUri).use { it?.write(bytes) }
            }
        }
    }

    /**
     * 清理本次导出产生的临时文件（FileProvider 不支持 delete()，按命名前缀清理）。
     */
    private fun cleanupTempFile() {
        runCatching {
            cacheDir.listFiles { f -> f.name.startsWith("export_") }?.forEach { it.delete() }
        }
    }

    private fun saveFile() {
        kotlin.runCatching {
            docCreate.launch(reqSaveFile.fileName)
        }.onFailure {
            it.printStackTrace()
            toast(R.string.sys_doc_picker_error)
            finish()
        }
    }

    private fun selectFile() {
        kotlin.runCatching {
            val mimes = reqSelectFile.fileMimes.map { if (it == "*") "*/*" else it }
            docSelector.launch(mimes.toTypedArray())
        }.onFailure {
            toast(R.string.sys_doc_picker_error)
            finish()
        }
    }

    private fun selectDir() {
        kotlin.runCatching {
            docTreeSelector.launch(Uri.EMPTY)
        }.onFailure {
            toast(R.string.sys_doc_picker_error)
            finish()
        }
    }


    interface IRequestData : Parcelable {}

    @Parcelize
    data class RequestSaveFile(
        val fileName: String = "ttsrv-file.json",
        val fileMime: String = "text/*",

        // 大数据使用Binder传递 这里只是负责临时存取
        @IgnoredOnParcel
        @Suppress("ArrayInDataClass")
        var fileBytes: ByteArray? = null,

        // 大文件改用临时文件 URI 传递，绕开 Binder 1MB 限制（替代 fileBytes+ByteArrayBinder）
        // 由 AppActivityResultContracts.createIntent 在写入临时文件后填充
        val fileUri: String? = null,
    ) : IRequestData

    @Parcelize
    data class RequestSelectDir(val rootUri: Uri = Uri.EMPTY) : IRequestData

    @Parcelize
    data class RequestSelectFile(val fileMimes: List<String> = listOf("*")) : IRequestData
}
