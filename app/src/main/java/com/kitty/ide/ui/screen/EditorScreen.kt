package com.kitty.ide.ui.screen

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // === 页面状态 ===
    var isDrawerOpen by remember { mutableStateOf(false) }
    var codeText by remember { mutableStateOf("") }
    
    // 模拟的多文件列表数据
    val openTabs = remember { mutableListOf("index.html", "style.css", "script.js") }
    var activeTab by remember { mutableStateOf("index.html") }

    // 左侧文件树抽屉宽度的动画：打开 240dp，关闭 0dp
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
            // 左侧小箭头：控制文件树抽屉
            IconButton(onClick = { isDrawerOpen = !isDrawerOpen }) {
                Icon(
                    imageVector = if (isDrawerOpen) Icons.AutoMirrored.Filled.ArrowBack 
                                  else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "切换文件树",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // 中间菜单区：保存 / 文件 / 视图 / 设置
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

            // 右上角：运行按钮（预留）
            IconButton(onClick = { /* TODO: 运行 */ }) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "运行",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // ═══════════════════════════════════════
        // 第二行：多文件标签栏（预留）
        // ═══════════════════════════════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            openTabs.forEach { tabName ->
                val isActive = tabName == activeTab
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable { activeTab = tabName }
                        .background(
                            if (isActive) MaterialTheme.colorScheme.background 
                            else Color.Transparent
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive) MaterialTheme.colorScheme.onBackground 
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ═══════════════════════════════════════
        // 主体区域：文件树 + 编辑区
        // ═══════════════════════════════════════
        Row(modifier = Modifier.weight(1f)) {

            // 【左侧】文件树抽屉（预留）
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
                    // TODO: 后续替换为真正的文件树
                    Text("（文件树占位）", style = MaterialTheme.typography.bodySmall)
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

/**
 * 辅助组件：顶部菜单的一个小文字按钮
 */
@Composable
fun TextMenuButton(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
    )
}

/**
 * 辅助组件：带行号的代码编辑区
 * 使用共享 ScrollState 保证行号和文字同步滚动
 */
@Composable
fun CodeEditorWithLineNumbers(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val lineCount = code.lines().size.coerceAtLeast(1) // 至少显示一行

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        // 左侧：行号栏
        Column(
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight()
                .verticalScroll(scrollState) // 与右侧共享滚动状态
                .padding(top = 8.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.End
        ) {
            for (i in 1..lineCount) {
                Text(
                    text = i.toString(),
                    color = Color(0xFF858585), // 灰色行号
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        // 右侧：代码输入区
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(scrollState) // 与左侧共享滚动状态
                .padding(top = 8.dp, bottom = 8.dp)
        ) {
            BasicTextField(
                value = code,
                onValueChange = onCodeChange,
                modifier = Modifier.fillMaxSize(),
                textStyle = TextStyle(
                    color = Color(0xFFD4D4D4),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                    lineHeight = 22.sp // 必须和行号的 lineHeight 完全一致！
                ),
                cursorBrush = SolidColor(Color(0xFF007ACC))
            )
        }
    }
}