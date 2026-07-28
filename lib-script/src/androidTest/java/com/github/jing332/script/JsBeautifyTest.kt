package com.github.jing332.script

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JsBeautifyTest {
    @Test
    fun invoke() {
        val code = """
           const prettier = require('https://unpkg.com/prettier@1.5.0/standalone.js')
           const formatted = prettier.format("let OBJ={ }; ", {
              parser: "estree",
           });
        """.trimIndent()

        val b = JsBeautify(InstrumentationRegistry.getInstrumentation().targetContext)
        println(b.format(code))


//        val engine = RhinoScriptEngine(RhinoScriptRuntime())
//        engine.execute(code)

    }
}