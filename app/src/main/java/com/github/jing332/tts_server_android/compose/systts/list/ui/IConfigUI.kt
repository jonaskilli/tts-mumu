package com.github.jing332.tts_server_android.compose.systts.list.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.TextToSpeechSource
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.TtsTopAppBar

abstract class IConfigUI {
    open val showSpeechEdit: Boolean = true

    @Composable
    abstract fun FullEditScreen(
        modifier: Modifier,
        systemTts: SystemTtsV2,
        onSystemTtsChange: (SystemTtsV2) -> Unit,
        onSave: () -> Unit,
        onCancel: () -> Unit,
        content: @Composable () -> Unit,
    )

    @Composable
    abstract fun ParamsEditScreen(
        modifier: Modifier,
        systemTts: SystemTtsV2,
        onSystemTtsChange: (SystemTtsV2) -> Unit,
    )

    protected fun SystemTtsV2.copySource(source: TextToSpeechSource): SystemTtsV2 {
        val config = config as TtsConfigurationDTO
        return this.copy(config = config.copy(source = source))
    }

    @ExperimentalMaterial3Api
    @Composable
    protected fun DefaultFullEditScreen(
        modifier: Modifier,
        title: String,
        verticalScrollEnabled: Boolean = true,
        onCancel: () -> Unit,
        onSave: () -> Unit,
        content: @Composable ColumnScope.( ) -> Unit,
    ) {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        Scaffold(
            modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TtsTopAppBar(
                    scrollBehavior = scrollBehavior,
                    title = { Text(text = title) },
                    onBackAction = onCancel,
                    onSaveAction = onSave
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(top = paddingValues.calculateTopPadding())
                    .then(
                        if (verticalScrollEnabled) Modifier.verticalScroll(rememberScrollState()) else Modifier,
                    )

            ) {
                content( )
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}