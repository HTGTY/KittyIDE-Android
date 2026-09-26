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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
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

    // 核心状态：编辑器的 TextFieldValue，用于获取光标位置
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }

    // 当切换标签时，重新加载对应文件的内容
    LaunchedEffect(activeIndex) {
        if (activeIndex in openFiles.indices) {
            textFieldValue = TextFieldValue(openFiles[activeIndex].content)
        } else {
            textFieldValue = TextFieldValue("")
        }
    }

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

            if (showTabMenu && tabMenuIndex in openFiles.indices) {
                DropdownMenu(
                    expanded = showTabMenu,
                    onDismissRequest = { showTabMenu = false }
                ) {
                    DropdownMenuItem(text = { Text("关闭该文件") }, onClick = { viewModel.closeFile(tabMenuIndex); showTabMenu = false })
                    DropdownMenuItem(text = { Text("关闭所有文件") }, onClick = { viewModel.closeAllFiles(); showTabMenu = false })
                    DropdownMenuItem(text = { Text("关闭左侧文件") }, onClick = { viewModel.closeLeftFiles(tabMenuIndex); showTabMenu = false })
                    DropdownMenuItem(text = { Text("关闭右侧文件") }, onClick = { viewModel.closeRightFiles(tabMenuIndex); showTabMenu = false })
                    DropdownMenuItem(text = { Text("创建副本") }, onClick = { viewModel.createCopy(tabMenuIndex); showTabMenu = false })
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
                            Text(text = if (projectFile.isDirectory) "📁" else "📄", modifier = Modifier.padding(end = 8.dp))
                            Text(text = projectFile.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            if (openFiles.isNotEmpty() && activeIndex in openFiles.indices) {
                CodeEditorWithLineNumbers(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        viewModel.updateActiveContent(newValue.text)
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text("没有打开的文件", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ── 底部：符号工具栏 ──
        SymbolToolbar(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding() // 👈 核心：跟随键盘上移
                .navigationBarsPadding(), // 👈 防止被系统底部导航条遮挡
            onSymbolClick = { symbol ->
                val newValue = insertSymbol(textFieldValue, symbol)
                textFieldValue = newValue
                viewModel.updateActiveContent(newValue.text)
            },
            onMoreClick = {
                Toast.makeText(context, "符号栏设置待开发", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * 底部符号工具栏
 */
@Composable
fun SymbolToolbar(
    modifier: Modifier = Modifier,
    onSymbolClick: (String) -> Unit,
    onMoreClick: () -> Unit
) {
    val symbols = listOf(
        "Tab", "{}", "[]", "()", "<>", "\"\"", "''",
        ";", ":", ",", ".", "!", "?", "=", "+", "-",
        "*", "/", "\\", "_", "|", "#", "$", "%", "&",
        "@", "^", "~"
    )

    LazyRow(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(symbols) { symbol ->
            Box(
                modifier = Modifier
                    .clickable { onSymbolClick(symbol) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = symbol,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 预留的“...”按钮
        item {
            Box(
                modifier = Modifier
                    .clickable { onMoreClick() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 在 TextFieldValue 中插入符号，并处理成对符号的光标跳转
 */
fun insertSymbol(value: TextFieldValue, symbol: String): TextFieldValue {
    val text = value.text
    val start = value.selection.start
    val end = value.selection.end

    val pairs = mapOf("{}" to "}", "[]" to "]", "()" to ")", "<>" to ">", "\"\"" to "\"", "''" to "'")

    // 成对符号：插入左符号和右符号，光标停在中间
    if (symbol.length == 2 && pairs.containsKey(symbol)) {
        val left = symbol.substring(0, 1)
        val right = symbol.substring(1)
        val newText = text.substring(0, start) + left + right + text.substring(end)
        return TextFieldValue(newText, TextRange(start + 1))
    }

    // Tab 键：插入 4 个空格
    if (symbol == "Tab") {
        val newText = text.substring(0, start) + "    " + text.substring(end)
        return TextFieldValue(newText, TextRange(start + 4))
    }

    // 普通符号：直接插入到光标处
    val newText = text.substring(0, start) + symbol + text.substring(end)
    return TextFieldValue(newText, TextRange(start + symbol.length))
}

/**
 * 处理回车自动缩进
 */
fun handleEnter(value: TextFieldValue): TextFieldValue? {
    val text = value.text
    val start = value.selection.start
    val end = value.selection.end

    if (start != end) return null // 有选中文本，不处理，走默认逻辑

    // 场景1：光标在 {|} 中间，回车后自动展开并缩进
    if (start > 0 && start < text.length && text[start - 1] == '{' && text[start] == '}') {
        val indent = "    "
        val newText = text.substring(0, start) + "\n" + indent + "\n" + text.substring(start)
        return TextFieldValue(newText, TextRange(start + 1 + indent.length))
    }

    // 场景2：普通换行，自动继承上一行的缩进
    val lineStart = text.lastIndexOf('\n', start - 1) + 1
    val currentLine = text.substring(lineStart, start)
    val indent = currentLine.takeWhile { it == ' ' || it == '\t' }
    val newText = text.substring(0, start) + "\n" + indent + text.substring(start)
    return TextFieldValue(newText, TextRange(start + 1 + indent.length))
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
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val lineCount = value.text.lines().size.coerceAtLeast(1)
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
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                val newValue = handleEnter(value)
                                if (newValue != null) {
                                    onValueChange(newValue)
                                    return@onPreviewKeyEvent true // 消费事件，阻止默认换行
                                }
                            }
                            false
                        },
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