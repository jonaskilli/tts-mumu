package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.jing332.database.entities.systts.SystemTtsV2
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickEditBottomSheet(
    onDismissRequest: () -> Unit,
    systts: SystemTtsV2,
    onSysttsChange: (SystemTtsV2) -> Unit,
) {
    val ui = remember {
        com.github.jing332.tts_server_android.compose.systts.list.ui.ConfigUiFactory.from(
            systts.config
        ) ?: throw IllegalArgumentException("Not supported config type: ${systts.config}")
    }
    val callbacks = rememberSaveCallBacks()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = {
        val ret = runBlocking {
            for (callback in callbacks) {
                if (!callback.onSave()) return@runBlocking false
            }
            true
        }

        if (ret) onDismissRequest()
    }) {
        Column(
            Modifier
                .padding(top = 12.dp)
                .padding(horizontal = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (ui.showSpeechEdit)
                CompositionLocalProvider(LocalSaveCallBack provides callbacks) {
                    SpeechRuleEditScreen(
                        modifier = Modifier.fillMaxWidth(),
                        systts = systts,
                        onSysttsChange = onSysttsChange,
                        showSpeechTarget = true
                    )
                }

            BasicInfoEditScreen(
                modifier = Modifier,
                systemTts = systts,
                onSystemTtsChange = onSysttsChange
            )
            ui.ParamsEditScreen(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                systemTts = systts,
                onSystemTtsChange = onSysttsChange
            )
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}