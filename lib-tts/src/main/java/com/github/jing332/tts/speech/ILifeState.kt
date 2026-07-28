package com.github.jing332.tts.speech

interface ILifeState {
    suspend fun onInit()
    fun onStop()
    fun onDestroy()
}