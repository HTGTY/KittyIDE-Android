package com.kitty.ide.ui.screen

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitty.ide.data.model.FileType
import com.kitty.ide.ui.viewmodel.EditorViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(
    projectPath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val projectFiles by viewModel.projectFiles.collectAsState()
    val openFiles by viewModel.openFiles.collectAsState()
    val activeIndex by viewModel.activeFileIndex.collectAsState()

    var isDrawerOpen by remember { mutableStateOf(false) }
    val drawerWidth by animateDpAsState(
        targetValue = if (isDrawerOpen) 240.dp else 0.dp,
        label = "drawerWidth"
    )

    var showTabMenu by remember { mutableStateOf(false) }
    var tabMenuIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(projectPath) {
        viewModel.loadProject(projectPath)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── 第一行：顶部主菜单栏 ──
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
                TextMenuButton("保存") {
                    if (viewModel.saveActiveFile()) {
                        Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "没有可保存的文件", Toast.LENGTH_SHORT).show()
                    }
                }
                TextMenuButton("文件") { }
                TextMenuButton("视图") { }
                TextMenuButton("设置") { }
            }

            IconButton(onClick = { /* TODO 运行 */ }) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "运行",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // ── 第二行：多文件标签栏 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            openFiles.forEachIndexed { index, openFile ->
                val isActive = index == activeIndex
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .combinedClickable(
                            onClick = { viewModel.selectFile(index) },
                            onDoubleClick = { tabMenuIndex = index; showTabMenu = true },
                            onLongClick = { tabMenuIndex = index; showTabMenu = true }
                        )
                        .background(
                            if (isActive) MaterialTheme.colorScheme.background
                            else Color.Transparent
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = openFile.file.name + if (openFile.isDirty) " *" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive) MaterialTheme.colorScheme.onBackground
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 标签弹出菜单（放在外层 Row 里，使用 Box 定位）
            if (showTabMenu && tabMenuIndex in openFiles.indices) {
                DropdownMenu(
                    expanded = showTabMenu,
                    onDismissRequest = { showTabMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("关闭该文件") },
                        onClick = { viewModel.closeFile(tabMenuIndex); showTabMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("关闭所有文件") },
                        onClick = { viewModel.closeAllFiles(); showTabMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("关闭左侧文件") },
                        onClick = { viewModel.closeLeftFiles(tabMenuIndex); showTabMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("关闭右侧文件") },
                        onClick = { viewModel.closeRightFiles(tabMenuIndex); showTabMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("创建副本") },
                        onClick = { viewModel.createCopy(tabMenuIndex); showTabMenu = false }
                    )
                }
            }
        }

        // ── 主体区域：文件树 + 编辑区 ──
        Row(modifier = Modifier.weight(1f)) {
            if (drawerWidth > 0.dp) {
                LazyColumn(
                    modifier = Modifier
                        .width(drawerWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                ) {
                    item {
                        Text(
                            text = "资源管理器",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(projectFiles) { projectFile ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (projectFile.isDirectory) {
                                        Toast.makeText(context, "目录暂不支持展开", Toast.LENGTH_SHORT).show()
                                    } else if (projectFile.fileType == FileType.TEXT) {
                                        viewModel.openFile(projectFile.file)
                                    } else {
                                        Toast.makeText(context, "暂不支持预览此格式", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (projectFile.isDirectory) "📁" else "📄",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = projectFile.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (openFiles.isNotEmpty() && activeIndex in openFiles.indices) {
                CodeEditorWithLineNumbers(
                    code = openFiles[activeIndex].content,
                    onCodeChange = { viewModel.updateActiveContent(it) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "没有打开的文件",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
            .clickable(onClick = onClick)
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