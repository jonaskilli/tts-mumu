package com.github.jing332.tts_server_android.compose

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.jing332.common.utils.SyncLock
import com.github.jing332.common.utils.toast
import kotlinx.coroutines.launch

class DebugSystemTtsActivity : ComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialText = """
            “大哥。”二长老上前一步，“何必为这叛徒神伤？”
        """.trimIndent()

        setContent {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                var text by remember { mutableStateOf(initialText) }

                var log by remember { mutableStateOf("") }

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Text") }
                    )


                    suspend fun tts() {
                        val syncLock = SyncLock()
                        val tts = TextToSpeech(this@DebugSystemTtsActivity, {
                            if (it == TextToSpeech.SUCCESS) {
                                toast("初始化成功")
                            } else
                                toast("初始化失败")

                            syncLock.cancel()
                        }, context.packageName)

                        syncLock.await()
                        tts.setOnUtteranceProgressListener(object :
                            UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                log += "onStart: $utteranceId\n"
                            }

                            override fun onDone(utteranceId: String?) {
                                log += "onDone: $utteranceId\n"
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                            }

                        })
                        tts.voices.forEach {
                            log += "voice: ${it.name}\n"

                            if (it.name.startsWith("晓晓"))
                                tts.voice = it
                        }
                        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                    LaunchedEffect(key1 = Unit) {
                        tts()
                    }
                    OutlinedButton(onClick = { scope.launch { tts() } }) {
                        Text(text = "Re Speak")
                    }

                }
            }
        }
    }
}
