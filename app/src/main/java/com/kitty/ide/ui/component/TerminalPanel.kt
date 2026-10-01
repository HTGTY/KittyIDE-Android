package com.kitty.ide.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitty.ide.R
import com.kitty.ide.data.terminal.LogLevel
import com.kitty.ide.data.terminal.TerminalManager

/**
 * 终端面板：日志列表 + 清空 + 关闭
 */
@Composable
fun TerminalPanel(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.background(Color(0xFF181818))
    ) {
        // ── 顶栏 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(Color(0xFF252526))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.terminal_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFD4D4D4),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { TerminalManager.clear() },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.terminal_clear),
                    tint = Color(0xFF858585),
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.terminal_close),
                    tint = Color(0xFF858585),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // ── 日志列表 ──
        if (TerminalManager.logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.terminal_empty),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6F6F6F)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(TerminalManager.logs) { entry ->
                    val color = when (entry.level) {
                        LogLevel.ERROR -> Color(0xFFF48771)
                        LogLevel.WARN -> Color(0xFFDCDCAA)
                        LogLevel.INFO -> Color(0xFF6A9955)
                        LogLevel.LOG -> Color(0xFFD4D4D4)
                    }
                    val prefix = when (entry.level) {
                        LogLevel.ERROR -> "[ERROR]"
                        LogLevel.WARN -> "[WARN]"
                        LogLevel.INFO -> "[INFO]"
                        LogLevel.LOG -> "[LOG]"
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = prefix,
                            color = color,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = entry.message,
                            color = color,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}