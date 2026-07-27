package com.github.jing332.common.audio.exo

import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceException

@UnstableApi
data class InputStreamDataSourceException(
    val reason: Int = PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
    override val message: String?,
    override val cause: Throwable?
) : DataSourceException(message, cause, reason)