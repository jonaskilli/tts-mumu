package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.jing332.tts_server_android.compose.systts.ConfigExportBottomSheet
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.database.entities.systts.GroupWithSystemTts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

@Composable
fun ListExportBottomSheet(onDismissRequest: () -> Unit, list: List<GroupWithSystemTts>) {
    // 第9项: 将大 JSON 序列化移到 IO 线程, 避免主线程阻塞导致导出页卡顿/慢。
    var json by remember(list) { mutableStateOf<String?>(null) }
    LaunchedEffect(list) {
        json = withContext(Dispatchers.IO) {
            AppConst.jsonBuilder.encodeToString(list)
        }
    }
    val jStr = json
    if (jStr == null) {
        Box(
            Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
        ) {
            CircularProgressIndicator()
        }
    } else {
        ConfigExportBottomSheet(
            json = jStr,
            onDismissRequest = onDismissRequest,
            fileName = "ttsrv-list.json"
        )
    }
}
