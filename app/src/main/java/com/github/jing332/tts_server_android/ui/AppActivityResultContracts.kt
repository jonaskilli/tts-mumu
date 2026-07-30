package com.github.jing332.tts_server_android.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.FileProvider
import com.github.jing332.tts_server_android.constant.KeyConst
import java.io.File

object AppActivityResultContracts {
    /**
     * 用于传递Parcelable数据
     */
    @Suppress("DEPRECATION")
    fun <T : Parcelable> parcelableDataActivity(clz: Class<out Activity>) =
        object : ActivityResultContract<T?, T?>() {
            override fun createIntent(context: Context, input: T?): Intent {
                return Intent(context, clz).apply {
                    if (input != null) putExtra(KeyConst.KEY_DATA, input)
                }
            }

            override fun parseResult(resultCode: Int, intent: Intent?): T? {
                return intent?.getParcelableExtra(KeyConst.KEY_DATA)
            }
        }

    fun filePickerActivity() =
        object :
            ActivityResultContract<FilePickerActivity.IRequestData, Pair<FilePickerActivity.IRequestData?, Uri?>>() {
            override fun createIntent(
                context: Context,
                input: FilePickerActivity.IRequestData
            ): Intent {
                return Intent(context, FilePickerActivity::class.java).apply {
                    if (input is FilePickerActivity.RequestSaveFile) {
                        // 第9项: 大文件改用临时文件 + FileProvider URI 传递，绕开 Binder 1MB 限制
                        val bytes = input.fileBytes
                        if (bytes != null && bytes.isNotEmpty()) {
                            val tempFile = File(
                                context.cacheDir,
                                "export_${System.currentTimeMillis()}_${input.fileName}"
                            )
                            tempFile.writeBytes(bytes)
                            val authority = "${context.packageName}.fileprovider"
                            val uri = FileProvider.getUriForFile(context, authority, tempFile)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            // 用 URI 代替 bytes 传给 FilePickerActivity；fileBytes 置空避免再次走 Binder
                            val newInput = input.copy(fileUri = uri.toString(), fileBytes = null)
                            putExtra(FilePickerActivity.KEY_REQUEST_DATA, newInput)
                            return@apply
                        }
                    }
                    putExtra(FilePickerActivity.KEY_REQUEST_DATA, input)
                }
            }

            @Suppress("DEPRECATION")
            override fun parseResult(
                resultCode: Int,
                intent: Intent?
            ): Pair<FilePickerActivity.IRequestData?, Uri?> {
                return intent?.getParcelableExtra<FilePickerActivity.IRequestData>(
                    FilePickerActivity.KEY_REQUEST_DATA
                ) to intent?.data
            }

        }
}
