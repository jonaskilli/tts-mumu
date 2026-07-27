package com.github.jing332.tts.synthesizer.event

fun interface IEventDispatcher {
    fun dispatch(event: Event)
}