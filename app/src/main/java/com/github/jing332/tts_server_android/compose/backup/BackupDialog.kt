package com.github.jing332.tts_server_android.compose.backup

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.TextCheckBox
import com.github.jing332.tts_server_android.R

@Composable
internal fun BackupDialog(
    profile: BackupProfile,
    onDismissRequest: () -> Unit,
    onBackupRequested: (BackupProfile, Boolean) -> Unit,
) {
    val includedTypes = remember(profile) { profile.includedTypes() }
    var uploadToWebDav by remember(profile) { mutableStateOf(false) }

    AppDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(profile.titleResId())) },
        content = {
            LazyColumn(Modifier.fillMaxWidth()) {
                item {
                    Text(
                        text = stringResource(profile.warningResId()),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    Text(
                        text = stringResource(R.string.backup_included_content),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(includedTypes) { type ->
                    IncludedTypeRow(type)
                }
                if (profile == BackupProfile.PERSONAL_FULL) {
                    item {
                        HorizontalDivider()
                        TextCheckBox(
                            modifier = Modifier.fillMaxWidth(),
                            text = { Text(stringResource(R.string.backup_to_webdav)) },
                            checked = uploadToWebDav,
                            onCheckedChange = { uploadToWebDav = it },
                        )
                    }
                }
            }
        },
        buttons = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
            TextButton(onClick = {
                onBackupRequested(profile, uploadToWebDav)
                onDismissRequest()
            }) {
                Text(stringResource(R.string.confirm))
            }
        },
    )
}

@Composable
private fun IncludedTypeRow(type: Type) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = true,
            onCheckedChange = null,
            enabled = false,
        )
        Text(
            text = stringResource(type.nameStrId),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

private fun BackupProfile.includedTypes(): List<Type> = when (this) {
    BackupProfile.PERSONAL_FULL -> Type.typeList
    BackupProfile.SHARE_SANITIZED -> listOf(
        Type.Preference,
        Type.List,
        Type.SpeechRule,
        Type.ReplaceRule,
        Type.Plugin,
    )
}

private fun BackupProfile.titleResId(): Int = when (this) {
    BackupProfile.PERSONAL_FULL -> R.string.personal_complete_backup
    BackupProfile.SHARE_SANITIZED -> R.string.share_backup
}

private fun BackupProfile.warningResId(): Int = when (this) {
    BackupProfile.PERSONAL_FULL -> R.string.personal_backup_sensitive_warning
    BackupProfile.SHARE_SANITIZED -> R.string.share_backup_privacy_warning
}
