package com.github.jing332.tts_server_android.compose.systts.replace.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.ComposeExtensions.clickableRipple
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.HtmlText
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.list.ui.ItemDescriptorFactory
import com.github.jing332.tts_server_android.compose.theme.AppTheme

@Preview
@Composable
private fun PreviewTtsConfigSelectDialog() {
    AppTheme {
        var show by remember { mutableStateOf(true) }
        if (show)
            SysttsSelectBottomSheet(onDismissRequest = { show = false }, {})
    }
}

@Composable
internal fun SysttsSelectBottomSheet(onDismissRequest: () -> Unit, onClick: (SystemTtsV2) -> Unit) {
    val items = remember { dbm.systemTtsV2.allEnabled.filter { it.config is TtsConfigurationDTO } }
    val context = LocalContext.current
    // 居中弹窗（10-13 用户令「适合居中弹窗的都要用居中弹窗…底部弹窗才是少见的特殊形式」）
    AppDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(id = R.string.choice_item, ""),
                style = MaterialTheme.typography.titleLarge
            )
        },
        content = {
            LazyColumn(Modifier.padding(vertical = 4.dp, horizontal = 8.dp)) {
                itemsIndexed(items) { _, systts ->
                    val descriptor =
                        remember(systts) { ItemDescriptorFactory.from(context, systts) }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickableRipple {
                                onClick(systts)
                            }
                            .padding(4.dp)
                    ) {
                        Text(
                            text = systts.displayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        HtmlText(
                            text = descriptor.desc,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        buttons = {},
    )
}
