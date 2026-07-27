package com.github.jing332.tts

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.jing332.common.audio.AudioUtils
import com.github.jing332.common.audio.ExoAudioPlayer
import com.github.jing332.tts.speech.local.AndroidTtsEngine
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidTtsEngineTest {
    private suspend fun engine(): AndroidTtsEngine {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val engine = AndroidTtsEngine(context)
//        engine.init("org.nobody.multitts")
        engine.init("com.google.android.tts")
        return engine
    }

    @Test
    fun play() {
        runBlocking {
            val engine = engine()
            engine.play("Hello 你好")
        }
    }

    @Test
    fun getFile() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val player = ExoAudioPlayer(context)

            val engine = engine()
            engine.getFile("Hello 你好").onFailure {
                throw RuntimeException("Engine failed")
            }.onSuccess {
                println(it.absolutePath)
                player.play(it.inputStream())
            }
        }
    }

    @Test
    fun getStream() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val player = ExoAudioPlayer(context)
            val engine = engine()
            withTimeout(2000) {
                engine.getStream(text = "Hello 你好").onSuccess {
                    player.play(it.readBytes())
                }
            }
            player.release()
        }
    }

    @Test
    fun getAudio() {
        runBlocking {
            val track = AudioUtils.createAudioTrack(16000)
            val engine = engine()

            track.play()
            engine.getAudio(text = "Hello 你好", listener = object : AndroidTtsEngine.Listener {
                override fun start() {
                    println("Start")
                }

                override fun available(audio: ByteArray) {
                    track.write(audio, 0, audio.size)
                }

                override fun done() {
                    println("Done")
                }

            }).onFailure {
                throw RuntimeException("Engine failed")
            }
            track.release()
        }
    }
}