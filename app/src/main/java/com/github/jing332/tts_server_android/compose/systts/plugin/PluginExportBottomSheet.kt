package com.github.jing332.tts_server_android.compose.systts.plugin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.widgets.TextCheckBox
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.ConfigExportBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PluginExportBottomSheet(
    onDismissRequest: () -> Unit,
    fileName: String,
    onGetJson: (isExportVars: Boolean, isJReadFormat: Boolean) -> String,
) {
    var isExportVars by remember { mutableStateOf(false) }
    var isJReadFormat by remember { mutableStateOf(false) }
    // 第9项: 序列化移到 IO 线程, 避免大插件列表导出时主线程卡顿。
    // 切换 isExportVars/isJReadFormat 时也会重新在 IO 线程序列化。
    var json by remember(isExportVars, isJReadFormat) { mutableStateOf<String?>(null) }
    LaunchedEffect(isExportVars, isJReadFormat) {
        json = withContext(Dispatchers.IO) { onGetJson(isExportVars, isJReadFormat) }
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
            fileName = fileName,
            json = jStr,
            onDismissRequest = onDismissRequest,
            content = {
                TextCheckBox(modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 4.dp),
                    text = { Text("JRead 格式") },
                    checked = isJReadFormat,
                    onCheckedChange = { isJReadFormat = !isJReadFormat })
                TextCheckBox(modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 4.dp),
                    text = { Text(stringResource(id = R.string.export_vars)) },
                    checked = isExportVars,
                    onCheckedChange = { isExportVars = !isExportVars })
            }
        )
    }
}
