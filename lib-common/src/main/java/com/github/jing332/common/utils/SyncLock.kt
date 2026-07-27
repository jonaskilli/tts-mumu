package com.github.jing332.common.utils

import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class SyncLock {
    private var job: Job? = null

    @Throws(CancellationException::class)
    suspend fun await() = coroutineScope {
        job = launch { awaitCancellation() }
        job?.join()
        job = null
    }

    fun cancel(cause: CancellationException? = null) {
        job?.cancel(cause)
    }
}