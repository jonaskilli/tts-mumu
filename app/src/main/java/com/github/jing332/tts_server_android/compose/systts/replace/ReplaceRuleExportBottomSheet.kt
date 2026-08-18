package com.github.jing332.tts_server_android.compose.systts.replace

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
import com.github.jing332.database.entities.replace.GroupWithReplaceRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplaceRuleExportBottomSheet(onDismissRequest: () -> Unit, list: List<GroupWithReplaceRule>) {
    // 第9项: 将大 JSON 序列化移到 IO 线程, 避免主线程阻塞导致导出页卡顿/慢。
    // 修复: 导出文件用 prettyPrint (jsonBuilder), 每个配置项独立一行可读。
    var json by remember(list) { mutableStateOf<String?>(null) }
    LaunchedEffect(list) {
        json = withContext(Dispatchers.IO) {
            AppConst.jsonBuilder.encodeToString(list)
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
        val fileName = when {
            list.size == 1 && list[0].list.size == 1 -> {
                val ruleName = list[0].list[0].name
                "替换规则-${ruleName}.json"
            }
            list.size == 1 -> {
                val groupName = list[0].group.name
                val count = list[0].list.size
                "替换规则-${groupName}-${count}项.json"
            }
            else -> {
                val groupCount = list.size
                val totalCount = list.sumOf { it.list.size }
                "替换规则-${groupCount}个分组-${totalCount}项.json"
            }
        }
        ConfigExportBottomSheet(
            json = jStr,
            onDismissRequest = onDismissRequest,
            fileName = fileName
        )
    }
}
