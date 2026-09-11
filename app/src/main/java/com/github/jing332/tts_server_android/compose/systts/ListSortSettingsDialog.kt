package com.github.jing332.tts_server_android.compose.systts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.jing332.common.utils.toast
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.LoadingContent
import com.github.jing332.compose.widgets.TextCheckBox
import com.github.jing332.tts_server_android.R
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ListSortSettingsDialog(
    name: String,
    onDismissRequest: () -> Unit,
    index: Int,
    onIndexChange: (Int) -> Unit,
    entries: List<String>,
    onConfirm: suspend (index: Int, descending: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var descending by remember { mutableStateOf(false) }
    var sorting by remember { mutableStateOf(false) }
    AppDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.sort)) },
        content = {
            LoadingContent(Modifier.padding(vertical = 4.dp), isLoading = sorting) {
                Column(Modifier.fillMaxWidth()) {
                    // 数量小字（用户 09-12：原孤零零居中大数字太空旷，降为辅助行说明对多少项排序）
                    Text(
                        "共 $name 项",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    // 左对齐 + 固定间距（原居中 FlowRow 缩在中间、上下留白观感空旷）
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        entries.forEachIndexed { i, s ->
                            val selected = i == index
                            FilterChip(
                                selected,
                                onClick = { onIndexChange(i) },
                                label = {
                                    Text(
                                        s,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        buttons = {
            Row(Modifier.fillMaxWidth()) {
                TextCheckBox(
                    text = {
                        Text(
                            stringResource(id = R.string.descending),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }, checked = descending, onCheckedChange = { descending = it }
                )

                Row(Modifier.weight(1f)) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(id = R.string.cancel))
                    }

                    TextButton(onClick = {
                        scope.launch {
                            sorting = true
                            val cost = measureTimeMillis { onConfirm(index, descending) }
                            context.toast(R.string.sorting_complete_msg, cost)
                            sorting = false
                        }
                    }) {
                        Text(stringResource(id = R.string.start))
                    }
                }
            }
        }
    )

}