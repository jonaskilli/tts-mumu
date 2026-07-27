package com.github.jing332.script

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.jing332.common.toLogLevelChar
import com.github.jing332.script.engine.RhinoScriptEngine
import com.github.jing332.script.simple.CompatScriptRuntime
import com.github.jing332.script.simple.SimpleScriptEngine
import com.github.jing332.script.simple.ext.JsExtensions
import com.github.jing332.script.source.StringScriptSource
import com.github.jing332.script.source.toScriptSource
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScriptTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun engine() = RhinoScriptEngine(CompatScriptRuntime(JsExtensions(context, "test")))

    private fun eval(code: String, sourceName: String = ""): Any? {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val engine = engine()
        engine.runtime.console.addLogListener {
            println(it.level.toLogLevelChar() + ": " + it.message)
        }
        return engine.execute(StringScriptSource(script = code, sourceName = sourceName))

    }

    private fun evalFromAsset(name: String): Any? {
        return InstrumentationRegistry.getInstrumentation().targetContext.assets.open("test/${name}.js")
            .use {
                val code = it.readBytes().decodeToString()
                eval(code, name)
            }
    }

    @Test
    fun testConsole() {
        evalFromAsset("console")
    }

    @Test
    fun testUuid() {
        evalFromAsset("uuid")
    }

    @Test
    fun websocket() {
        evalFromAsset("websocket")
    }

    @Test
    fun testFile() {
        evalFromAsset("file")
    }

    @Test
    fun testBuffer() {
        evalFromAsset("buffer")
    }

    @Test
    fun testWebview() {
        evalFromAsset("webview")
    }

    @Test
    fun importer() {
        val code = """
            let String = Packages.java.lang.String
            
            println(String.valueOf(1))
            
            importPackage(java.util)
            let encoder = Base64.getEncoder()
        """.trimIndent()
        eval(code)
    }


    @Test
    fun http() {
        evalFromAsset("http_get")
    }

    @Test
    fun httpPost() {
        evalFromAsset("http_post")
    }

    @Test
    fun httpPostMultipart() {
        evalFromAsset("http_post_multipart")
    }

    @Test
    fun scope() {
        val e = engine()
        e.runtime.console.addLogListener {
            println(it.level.toLogLevelChar() + ": " + it.message)
        }

        // 测试两次执行环境是否独立
        // 即每次调用初始化新的scope
        // global 作为 scope.prototype 以复用
        e.execute(StringScriptSource("""a=111; console.log(globalThis.a)"""))
        val ret = e.execute(StringScriptSource("""globalThis.a == undefined"""))
        assert(ret == true)

//        assert(e.execute(StringScriptSource("globalThis.a != undefined")) == true)
    }

    @Test
    fun simpleEngine() {
        val androidContext = InstrumentationRegistry.getInstrumentation().targetContext
        val e = SimpleScriptEngine(androidContext, "simple-engine")
        e.execute(
            """
            ttsrv.writeTxtFile("hello world", "test.txt")
        """.trimIndent().toScriptSource()
        )
    }


    @Test
    fun testRequire() {
        evalFromAsset("require_module")
    }
}