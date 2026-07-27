package com.github.jing332.tts_server_android.compose.codeeditor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jing332.common.LogEntry
import com.github.jing332.script.JsBeautify
import com.github.jing332.server.script.ScriptRemoteServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.net.BindException
import java.net.SocketException

class CodeEditorViewModel(val app: Application) : AndroidViewModel(app) {
    companion object {
        const val TAG = "CodeEditorViewModel"

        const val SYNC_ACTION_DEBUG = "debug"
    }

    private var server: ScriptRemoteServer? = null

    private var mError = MutableStateFlow<Error>(Error.Empty)
    internal val error get() = mError.asSharedFlow()

    // 代码同步服务器
    fun startSyncServer(
        port: Int,
        onPush: (code: String) -> Unit,
        onPull: () -> String,
        onDebug: () -> Unit,
        onAction: (name: String, body: ByteArray?) -> Unit
    ) {
        if (server != null) return
        server = ScriptRemoteServer(port, object : ScriptRemoteServer.Callback {
            override fun pull(): String = onPull()

            override fun push(code: String) = onPush(code)

            override fun action(name: String) {
                if (name == SYNC_ACTION_DEBUG)
                    onDebug()
                else onAction.invoke(name, null)
            }

            override fun log(): List<LogEntry> = emptyList()

        }).apply {
            try {
                start(wait = false)
            } catch (e: BindException) {
                mError.tryEmit(Error.PortConflict)
            } catch (e: SocketException) {
                mError.tryEmit(Error.Socket(e.localizedMessage ?: e.toString()))
            } catch (e: Exception) {
                mError.tryEmit(Error.Other(e))
            } finally {
//                server = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        server?.stop()
        server = null
    }

    private val beautifier by lazy { JsBeautify(app) }
    fun formatCode(code: String): String {
        return beautifier.format(code)
    }
}

internal sealed interface Error {
    data object Empty : Error
    data object PortConflict : Error
    data class Socket(val message: String) : Error
    data class Other(val e: Exception) : Error
}