package com.github.jing332.tts_server_android.compose.nav

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jing332.tts_server_android.R

sealed class NavRoutes(
    val id: String,
    @StringRes val strId: Int,
    val icon: @Composable () -> Unit = {},
) {


    data object MainPager : NavRoutes("main", R.string.system_tts, icon = {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = R.drawable.ic_tts),
            contentDescription = stringResource(R.string.system_tts)
        )
    })

    // =============
    data object TtsEdit : NavRoutes("tts_edit", 0) {
        const val DATA = "data"
    }
}