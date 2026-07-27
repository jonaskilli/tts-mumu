package com.github.jing332.common

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.jing332.common.utils.NetworkUtils

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class UtilsTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.github.jing332.lib_common.test", appContext.packageName)
    }

    @Test
    fun uploadLog() {
        val log = """
            testaaaaaaaaaaaaaaaaaaaaaaa
        """.trimIndent()

        println(NetworkUtils.uploadLog(log))
    }
}