package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.widgets.AppSpinner
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.AbstractListGroup.Companion.DEFAULT_GROUP_ID
import com.github.jing332.database.entities.systts.SystemTtsGroup
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.tts_server_android.R

@Composable
fun BasicInfoEditScreen(
    modifier: Modifier,
    systemTts: SystemTtsV2,
    onSystemTtsChange: (SystemTtsV2) -> Unit,

    group: SystemTtsGroup = rememberUpdatedState(
        newValue = dbm.systemTtsV2.getGroup(systemTts.groupId)
            ?: SystemTtsGroup(id = DEFAULT_GROUP_ID, name = "")
    ).value,
    groups: List<SystemTtsGroup> = remember { dbm.systemTtsV2.allGroup },
) {
    Column(modifier) {
        AppSpinner(
            modifier = Modifier.fillMaxWidth(),
            labelText = stringResource(id = R.string.group),
            value = group,
            values = groups,
            onValueSame = { current, new -> (current as SystemTtsGroup).id == (new as SystemTtsGroup).id },
            entries = groups.map { it.name },
            onSelectedChange = { k, _ ->
                onSystemTtsChange(systemTts.copy(groupId = (k as SystemTtsGroup).id))
            }
        )
        OutlinedTextField(
            label = { Text(stringResource(R.string.display_name)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            value = systemTts.displayName, onValueChange = {
                onSystemTtsChange(systemTts.copy(displayName = it))
            },
            trailingIcon = {
                if (systemTts.displayName.isNotEmpty())
                    IconButton(onClick = {
                        onSystemTtsChange(systemTts.copy(displayName = ""))
                    }) {
                        Icon(Icons.Default.Clear, stringResource(id = R.string.clear_text_content))
                    }
            }
        )
    }
}
