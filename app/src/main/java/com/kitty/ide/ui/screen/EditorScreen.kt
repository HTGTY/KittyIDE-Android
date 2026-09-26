package com.kitty.ide.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import com.kitty.ide.data.model.ProjectFile
import com.kitty.ide.data.terminal.TerminalManager
import com.kitty.ide.ui.component.TerminalPanel
import com.kitty.ide.ui.viewmodel.EditorViewModel
import java.io.File

/** 新建项类型 */
enum class NewItemType(val title: String, val defaultExt: String?, val hint: String) {
    FILE("新建文件", null, "输入完整文件名，例如 main.py"),
    FOLDER("新建文件夹", null, "输入文件夹名"),
    HTML("新建 HTML 文件", ".html", "输入文件名（自动补 .html）"),
    CSS("新建 CSS 文件", ".css", "输入文件名（自动补 .css）"),
    JS("新建 JS 文件", ".js", "输入文件名（自动补 .js）")
}

/**
 * 单链目录自动展开
 */
fun autoExpandChain(
    dir: File,
    expandedMap: MutableMap<String, Boolean>,
    viewModel: EditorViewModel
) {
    var current = dir
    while (true) {
        val children = viewModel.listChildren(current)
        if (children.size == 1 && children[0].isDirectory) {
            val childPath = children[0].file.absolutePath
            expandedMap[childPath] = true
            current = children[0].file
        } else {
            break
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(
    projectPath: String,
    onBack: () -> Unit,
    onRun: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val projectFiles by viewModel.projectFiles.collectAsState()
    val openFiles by viewModel.openFiles.collectAsState()
    val activeIndex by viewModel.activeFileIndex.collectAsState()
    val projectDir by viewModel.projectDir.collectAsState()

    var isDrawerOpen by remember { mutableStateOf(false) }
    val drawerWidth by animateDpAsState(
        targetValue = if (isDrawerOpen) 240.dp else 0.dp,
        label = "drawerWidth"
    )

    var showTerminal by remember { mutableStateOf(false) }

    var showTabMenu by remember { mutableStateOf(false) }
    var tabMenuIndex by remember { mutableStateOf(-1) }

    var showFileTreeMenu by remember { mutableStateOf(false) }
    var showNewItemDialog by remember { mutableStateOf(false) }
    var newItemType by remember { mutableStateOf(NewItemType.FILE) }
    var newItemName by remember { mutableStateOf("") }

    var newItemParentDir by remember { mutableStateOf<File?>(null) }
    var fileTreeVersion by remember { mutableIntStateOf(0) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

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
                                  else Icons.Default.Menu,
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
                TextMenuButton("设置") { onOpenSettings() }
            }

            // ── 终端按钮 ──
            IconButton(onClick = { showTerminal = !showTerminal }) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "终端",
                    tint = if (showTerminal) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurface
                )
            }

            // ── 运行按钮 ──
            IconButton(onClick = {
                viewModel.saveActiveFile()

                if (activeIndex !in openFiles.indices) {
                    Toast.makeText(context, "没有打开的文件", Toast.LENGTH_SHORT).show()
                    return@IconButton
                }

                val currentFile = openFiles[activeIndex].file
                val htmlFile: File? = when {
                    currentFile.extension.lowercase() == "html" -> currentFile
                    else -> {
                        val sibling = File(currentFile.parentFile, "index.html")
                        if (sibling.exists()) sibling else null
                    }
                }

                if (htmlFile != null) {
                    TerminalManager.clear()
                    onRun(htmlFile.absolutePath)
                } else {
                    Toast.makeText(context, "没有找到可运行的 HTML 文件", Toast.LENGTH_SHORT).show()
                }
            }) {
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

        // ── 主体区域：文件树 + 编辑区 + 终端 ──
        Row(modifier = Modifier.weight(1f)) {
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
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "长按空白区域创建文件或目录",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .combinedClickable(
                                onClick = { },
                                onLongClick = {
                                    newItemParentDir = null
                                    showFileTreeMenu = true
                                }
                            )
                    ) {
                        projectFiles.forEach { file ->
                            FileTreeItem(
                                file = file,
                                depth = 0,
                                expandedMap = expandedMap,
                                version = fileTreeVersion,
                                onFileClick = { f ->
                                    if (f.fileType == FileType.TEXT) {
                                        viewModel.openFile(f.file)
                                    } else {
                                        Toast.makeText(context, "暂不支持预览此格式", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onLongClick = { f ->
                                    newItemParentDir = f.file
                                    showFileTreeMenu = true
                                },
                                viewModel = viewModel
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "没有打开的文件",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "点击左上角展开文件树",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // ── 右侧终端面板 ──
            AnimatedVisibility(
                visible = showTerminal,
                enter = expandHorizontally(expandFrom = Alignment.End),
                exit = shrinkHorizontally(shrinkTowards = Alignment.End)
            ) {
                TerminalPanel(
                    onClose = { showTerminal = false },
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight()
                )
            }
        }

        // ── 底部：符号工具栏 ──
        SymbolToolbar(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
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

    // ── 文件树新建菜单 ──
    if (showFileTreeMenu) {
        val targetLabel = newItemParentDir?.name ?: "项目根目录"
        AlertDialog(
            onDismissRequest = { showFileTreeMenu = false },
            title = { Text("在「$targetLabel」中新建") },
            text = {
                Column {
                    MenuOption("📄 新建文件") {
                        newItemType = NewItemType.FILE
                        newItemName = ""
                        showNewItemDialog = true
                        showFileTreeMenu = false
                    }
                    MenuOption("📁 新建文件夹") {
                        newItemType = NewItemType.FOLDER
                        newItemName = ""
                        showNewItemDialog = true
                        showFileTreeMenu = false
                    }
                    MenuOption("🌐 新建 HTML 文件") {
                        newItemType = NewItemType.HTML
                        newItemName = ""
                        showNewItemDialog = true
                        showFileTreeMenu = false
                    }
                    MenuOption("🎨 新建 CSS 文件") {
                        newItemType = NewItemType.CSS
                        newItemName = ""
                        showNewItemDialog = true
                        showFileTreeMenu = false
                    }
                    MenuOption("⚙️ 新建 JS 文件") {
                        newItemType = NewItemType.JS
                        newItemName = ""
                        showNewItemDialog = true
                        showFileTreeMenu = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFileTreeMenu = false }) { Text("取消") }
            }
        )
    }

    if (showNewItemDialog) {
        NewItemDialog(
            type = newItemType,
            value = newItemName,
            onValueChange = { newItemName = it },
            onDismiss = { showNewItemDialog = false; newItemName = "" },
            onConfirm = {
                val name = newItemName.trim()
                val targetDir = newItemParentDir ?: projectDir
                if (name.isNotEmpty() && targetDir != null) {
                    expandedMap[targetDir.absolutePath] = true
                    autoExpandChain(targetDir, expandedMap, viewModel)

                    if (newItemType == NewItemType.FOLDER) {
                        viewModel.createFolder(targetDir, name) { success ->
                            if (success) {
                                fileTreeVersion++
                            } else {
                                Toast.makeText(context, "创建失败，可能已存在", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        val ext = newItemType.defaultExt
                        val fileName = if (ext != null && !name.endsWith(ext)) name + ext else name
                        viewModel.createFile(targetDir, fileName) { success ->
                            if (success) {
                                fileTreeVersion++
                            } else {
                                Toast.makeText(context, "创建失败，可能已存在", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                showNewItemDialog = false
                newItemName = ""
                newItemParentDir = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileTreeItem(
    file: ProjectFile,
    depth: Int,
    expandedMap: MutableMap<String, Boolean>,
    version: Int,
    onFileClick: (ProjectFile) -> Unit,
    onLongClick: (ProjectFile) -> Unit,
    viewModel: EditorViewModel
) {
    val path = file.file.absolutePath
    val isExpanded = expandedMap[path] == true

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (file.isDirectory) {
                            val willExpand = !isExpanded
                            expandedMap[path] = willExpand
                            if (willExpand) {
                                autoExpandChain(file.file, expandedMap, viewModel)
                            }
                        } else {
                            onFileClick(file)
                        }
                    },
                    onLongClick = { onLongClick(file) }
                )
                .padding(
                    start = (depth * 16).dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (file.isDirectory) (if (isExpanded) "▼" else "▶") else "",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                modifier = Modifier.width(16.dp)
            )
            Text(
                text = if (file.isDirectory) "📁" else "📄",
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (file.isDirectory) {
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val children = remember(path, version) {
                        viewModel.listChildren(file.file)
                    }
                    children.forEach { child ->
                        FileTreeItem(
                            file = child,
                            depth = depth + 1,
                            expandedMap = expandedMap,
                            version = version,
                            onFileClick = onFileClick,
                            onLongClick = onLongClick,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MenuOption(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp)
    )
}

@Composable
fun NewItemDialog(
    type: NewItemType,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(type.title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("名称") },
                placeholder = { Text(type.hint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = value.isNotBlank()
            ) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

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

fun insertSymbol(value: TextFieldValue, symbol: String): TextFieldValue {
    val text = value.text
    val start = value.selection.start
    val end = value.selection.end

    val pairs = mapOf("{}" to "}", "[]" to "]", "()" to ")", "<>" to ">", "\"\"" to "\"", "''" to "'")

    if (symbol.length == 2 && pairs.containsKey(symbol)) {
        val left = symbol.substring(0, 1)
        val right = symbol.substring(1)
        val newText = text.substring(0, start) + left + right + text.substring(end)
        return TextFieldValue(newText, TextRange(start + 1))
    }

    if (symbol == "Tab") {
        val newText = text.substring(0, start) + "    " + text.substring(end)
        return TextFieldValue(newText, TextRange(start + 4))
    }

    val newText = text.substring(0, start) + symbol + text.substring(end)
    return TextFieldValue(newText, TextRange(start + symbol.length))
}

fun handleEnter(value: TextFieldValue): TextFieldValue? {
    val text = value.text
    val start = value.selection.start
    val end = value.selection.end

    if (start != end) return null

    if (start > 0 && start < text.length && text[start - 1] == '{' && text[start] == '}') {
        val indent = "    "
        val newText = text.substring(0, start) + "\n" + indent + "\n" + text.substring(start)
        return TextFieldValue(newText, TextRange(start + 1 + indent.length))
    }

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
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                val newValue = handleEnter(value)
                                if (newValue != null) {
                                    onValueChange(newValue)
                                    return@onPreviewKeyEvent true
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