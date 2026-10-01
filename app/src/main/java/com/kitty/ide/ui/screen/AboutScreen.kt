package com.kitty.ide.ui.screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitty.ide.AppInfo
import com.kitty.ide.R
import com.kitty.ide.data.repository.SettingsRepository
import com.kitty.ide.data.update.ApkDownloader
import com.kitty.ide.data.update.UpdateChecker
import com.kitty.ide.data.update.UpdateResult
import kotlinx.coroutines.launch

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember { SettingsRepository(context) }

    var isChecking by remember { mutableStateOf(false) }
    var updateDialogData by remember { mutableStateOf<UpdateResult.UpdateAvailable?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── 顶栏 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(48.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_toggle_tree),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = stringResource(R.string.about_title_full),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // ── 内容 ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ===== 基本信息 =====
            SectionCard(title = stringResource(R.string.about_basic_info)) {
                InfoLine(stringResource(R.string.about_version), AppInfo.VERSION)
                InfoLine(stringResource(R.string.about_environment), AppInfo.ENVIRONMENT)
                InfoLine(stringResource(R.string.about_author), AppInfo.AUTHOR)
                // 开源地址（可点击打开）
                ClickableLine(
                    label = stringResource(R.string.about_source),
                    value = AppInfo.OPEN_SOURCE_URL,
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppInfo.OPEN_SOURCE_URL))
                                .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.about_open_link_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }

            // ===== 联系方式 =====
            SectionCard(title = stringResource(R.string.about_contact)) {
                ClickableLine(
                    label = "",
                    value = stringResource(R.string.about_contact_qq),
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:${AppInfo.AUTHOR_EMAIL}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.about_open_link_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }

            // ===== 检查更新 =====
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isChecking && !isDownloading) {
                        isChecking = true
                        scope.launch {
                            val result = UpdateChecker.checkForUpdate(AppInfo.VERSION)
                            isChecking = false
                            when (result) {
                                is UpdateResult.NoUpdate -> {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.update_no_update),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                is UpdateResult.UpdateAvailable -> {
                                    val ignored = settingsRepository.getIgnoredUpdateVersion()
                                    if (ignored == result.version) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.update_no_update),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        updateDialogData = result
                                    }
                                }
                                is UpdateResult.Failed -> {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.update_check_failed) + ": ${result.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isChecking) stringResource(R.string.update_checking)
                               else stringResource(R.string.update_check_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (isDownloading)
                            stringResource(R.string.update_downloading, downloadProgress)
                        else
                            stringResource(R.string.update_check_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ===== 感谢名单（放最后） =====
            SectionCard(title = stringResource(R.string.about_thanks_section)) {
                Text(
                    text = stringResource(R.string.about_thanks_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.about_copyright),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    // ── 更新弹窗 ──
    updateDialogData?.let { update ->
        AlertDialog(
            onDismissRequest = { updateDialogData = null },
            title = { Text(stringResource(R.string.update_available_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.update_new_version, update.version))
                    Text(stringResource(R.string.update_current_version, AppInfo.VERSION))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.update_release_notes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = update.notes.ifBlank { "—" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.height(180.dp).verticalScroll(rememberScrollState())
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = update
                    updateDialogData = null
                    isDownloading = true
                    downloadProgress = 0
                    scope.launch {
                        val file = ApkDownloader.download(
                            context = context,
                            url = target.downloadUrl,
                            onProgress = { downloadProgress = it }
                        )
                        isDownloading = false
                        if (file != null) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.update_download_complete),
                                Toast.LENGTH_SHORT
                            ).show()
                            ApkDownloader.installApk(context, file)
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.update_download_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }) { Text(stringResource(R.string.update_btn_update)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        settingsRepository.setIgnoredUpdateVersion(update.version)
                        updateDialogData = null
                        Toast.makeText(
                            context,
                            context.getString(R.string.update_no_update),
                            Toast.LENGTH_SHORT
                        ).show()
                    }) { Text(stringResource(R.string.update_btn_ignore)) }

                    TextButton(onClick = { updateDialogData = null }) {
                        Text(stringResource(R.string.update_btn_cancel))
                    }
                }
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ClickableLine(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp)
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}