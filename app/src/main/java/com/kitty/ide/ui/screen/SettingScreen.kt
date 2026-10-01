package com.kitty.ide.ui.screen

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.kitty.ide.ui.theme.FontManager
import com.kitty.ide.ui.theme.ThemeManager

/** 把主题模式的 key 映射成本地化文本 */
@Composable
private fun themeLabelOf(mode: String): String = when (mode) {
    "Light" -> stringResource(R.string.theme_light)
    "Dark" -> stringResource(R.string.theme_dark)
    else -> stringResource(R.string.theme_auto)
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mode by ThemeManager.themeMode
    val fontKey by FontManager.fontKey

    var showAboutDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── 顶栏：返回 + 标题 ──
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
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // ── 卡片列表 ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 卡片 1：关于
            SettingsCard(
                title = stringResource(R.string.settings_about),
                subtitle = stringResource(R.string.settings_about_subtitle),
                onClick = { showAboutDialog = true }
            )

            // 卡片 2：显示设置
            SettingsCard(
                title = stringResource(R.string.settings_display),
                subtitle = stringResource(R.string.settings_display_subtitle, themeLabelOf(mode)),
                onClick = { showThemeDialog = true }
            )

            // 卡片 3：编辑器字体
            SettingsCard(
                title = stringResource(R.string.settings_font),
                subtitle = stringResource(R.string.settings_font_subtitle, FontManager.labelOf(fontKey)),
                onClick = { showFontDialog = true }
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "${AppInfo.NAME} v${AppInfo.VERSION}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    // ── 关于弹窗 ──
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.about_title, AppInfo.NAME)) },
            text = {
                Column {
                    Text(
                        text = AppInfo.NAME,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    AboutLine(stringResource(R.string.about_version), AppInfo.VERSION)
                    AboutLine(stringResource(R.string.about_environment), AppInfo.ENVIRONMENT)
                    AboutLine(stringResource(R.string.about_author), AppInfo.AUTHOR)
                    AboutLine(stringResource(R.string.about_source), AppInfo.OPEN_SOURCE_URL)
                    AboutLine(
                        stringResource(R.string.about_thanks),
                        stringResource(R.string.about_thanks_placeholder)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.app_slogan),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.about_copyright),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.about_ok))
                }
            }
        )
    }

    // ── 主题模式选择弹窗 ──
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.theme_title)) },
            text = {
                Column {
                    listOf("Light", "Dark", "Auto").forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    ThemeManager.setMode(context, option)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = mode == option,
                                onClick = { ThemeManager.setMode(context, option) }
                            )
                            Text(
                                text = themeLabelOf(option),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.theme_done))
                }
            }
        )
    }

    // ── 编辑器字体选择弹窗 ──
    if (showFontDialog) {
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text(stringResource(R.string.font_title)) },
            text = {
                Column {
                    FontManager.available.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    FontManager.setFont(context, option.key)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = fontKey == option.key,
                                onClick = { FontManager.setFont(context, option.key) }
                            )
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontDialog = false }) {
                    Text(stringResource(R.string.font_done))
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AboutLine(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
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