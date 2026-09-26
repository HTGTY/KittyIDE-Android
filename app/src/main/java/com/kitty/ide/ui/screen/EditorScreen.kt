package com.kitty.ide.ui.screen

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // 👈 就是这行缺失了！
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditorScreen(
    projectUri: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // === 页面状态 ===
    var isDrawerOpen by remember { mutableStateOf(false) }
    var codeText by remember { mutableStateOf("") }

    val drawerWidth by animateDpAsState(
        targetValue = if (isDrawerOpen) 240.dp else 0.dp,
        label = "drawerWidth"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ═══════════════════════════════════════
        // 第一行：顶部主菜单栏
        // ═══════════════════════════════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(48.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { isDrawerOpen = !isDrawerOpen }) {
                Icon(
                    imageVector = if (isDrawerOpen) Icons.AutoMirrored.Filled.ArrowBack
                                  else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "切换文件树",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextMenuButton("保存") { /* TODO */ }
                TextMenuButton("文件") { /* TODO */ }
                TextMenuButton("视图") { /* TODO */ }
                TextMenuButton("设置") { /* TODO */ }
            }

            IconButton(onClick = { /* TODO: 运行 */ }) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "运行",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // ═══════════════════════════════════════
        // 第二行：多文件标签栏（占位）
        // ═══════════════════════════════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "未命名文件",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // ═══════════════════════════════════════
        // 主体区域：文件树 + 编辑区
        // ═══════════════════════════════════════
        Row(modifier = Modifier.weight(1f)) {
            // 【左侧】文件树抽屉（占位）
            if (drawerWidth > 0.dp) {
                Column(
                    modifier = Modifier
                        .width(drawerWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "资源管理器",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "项目路径:\n$projectUri", // 显示传入的 URI，证明导航传参成功
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 【右侧】编辑区
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                CodeEditorWithLineNumbers(
                    code = codeText,
                    onCodeChange = { codeText = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TextMenuButton(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clickable(onClick = onClick) // 这里用到了 clickable，上面已修复导入
            .padding(horizontal = 12.dp)
    )
}

@Composable
fun CodeEditorWithLineNumbers(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val lineCount = code.lines().size.coerceAtLeast(1)
    var fontSize by remember { mutableFloatStateOf(15f) }
    val lineHeight = fontSize * 1.5f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    fontSize = (fontSize * zoom).coerceIn(10f, 30f)
                }
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 行号
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .verticalScroll(verticalScrollState)
                    .padding(top = 8.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lineCount) {
                    Text(
                        text = i.toString(),
                        color = Color(0xFF858585),
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSize.sp,
                        lineHeight = lineHeight.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            // 输入区
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(verticalScrollState)
                    .horizontalScroll(horizontalScrollState)
                    .padding(top = 8.dp, bottom = 8.dp)
            ) {
                BasicTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(
                        color = Color(0xFFD4D4D4),
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSize.sp,
                        lineHeight = lineHeight.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF007ACC))
                )
            }
        }
    }
}