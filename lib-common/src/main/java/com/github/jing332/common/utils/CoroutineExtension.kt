package com.github.jing332.common.utils

import kotlinx.coroutines.job
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

object CoroutineExtension {
    suspend fun onCanceled(block: (e: CancellationException) -> Unit) {
        val job = coroutineContext.job // this fails if there is no job
        job.invokeOnCompletion { cause ->
            if (cause is CancellationException) {
                block(cause)
            }
        }
    }
}