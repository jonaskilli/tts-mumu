package com.github.jing332.tts.speech.plugin.engine

object PackageImporter {
    val default by lazy {
        line(
            listOf(
                "com.github.jing332.tts.speech.plugin.engine.type.ws",
                "com.github.jing332.tts.speech.plugin.engine.type.ui",
                "android.view",
                "android.widget",
            )
        )
    }

    private fun line(packages: List<String>): String {
        val s = packages.joinToString(separator = ";") { "importPackage($it)" }
        return "$s;"
    }
}