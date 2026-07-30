package com.github.jing332.tts_server_android.compose.systts.speechrule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.jing332.tts_server_android.compose.systts.ConfigExportBottomSheet
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.database.entities.SpeechRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeechRuleExportBottomSheet(onDismissRequest: () -> Unit, list: List<SpeechRule>) {
    // 第9项: 将大 JSON 序列化移到 IO 线程, 避免主线程阻塞导致导出页卡顿/慢。
    // 第1项: 用 compactJsonBuilder 关闭 prettyPrint, 减小体积+加速序列化。
    var json by remember(list) { mutableStateOf<String?>(null) }
    LaunchedEffect(list) {
        json = withContext(Dispatchers.IO) {
            AppConst.compactJsonBuilder.encodeToString(list)
        }
    }
    val jStr = json
    if (jStr == null) {
        // 加载状态放进 ModalBottomSheet 内, 避免全屏灰色遮罩
        ModalBottomSheet(onDismissRequest = onDismissRequest) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .wrapContentSize(Alignment.Center)
            ) {
                CircularProgressIndicator()
            }
        }
    } else {
        ConfigExportBottomSheet(
            onDismissRequest = onDismissRequest,
            json = jStr,
            fileName = if (list.size == 1) "ttsrv-speechRule-${list[0].name}.json" else "ttsrv-speechRules.json"
        )
    }
}
