package com.github.jing332.tts_server_android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.jing332.script.withRhinoContext
import com.github.jing332.script.simple.ext.JsExtensions
import com.github.jing332.tts_server_android.model.rhino.direct_link_upload.DirectUploadEngine
import org.junit.Test
import org.junit.runner.RunWith
import splitties.init.appCtx

@RunWith(AndroidJUnit4::class)
class DirectUploadEngineTest {

    @Test
    fun catBox() {
        val ext = JsExtensions(app, "")

        val form = mutableMapOf<String, Any>()

        form["reqtype"] = "fileupload"
        form["fileToUpload"] = mutableMapOf<String, Any>().also {
            it["fileName"] = "ccc.json"
            it["body"] = """ {"1":"1", "2":"2"} """
            it["contentType"] = "application/json"
        }
//        form["file"] = mutableMapOf<String, Any>().apply {
//            put("file", mutableMapOf<String, String>().apply {
//                put("fileToUpload", """ {"1":"1", "2":"2"} """)
//            })
//            put("fileName", "config.json")
//            put("contentType", "application/json")
//        }

        val resp = ext.httpPostMultipart(
            "https://catbox.moe/user/api.php",
            form
        )
        println(resp.body?.string())
    }

    @Test
    fun testJS() {
        val code = """
            let DirectUploadJS = {
                "XX网盘(永久有效)": function(config){
                    println("from js: " + config)
                    return {'url':'https://xxx.com/111.json', 'summary':'永久有效'}                                                                                        
                },
            }
        """.trimIndent()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val engine = DirectUploadEngine(context, code = code)
        val list = engine.obtainFunctionList()
        println(list)
        withRhinoContext { cx->
            list.forEach {
                it.invoke(""" {"name":"123"} """)
            }
        }
    }

}