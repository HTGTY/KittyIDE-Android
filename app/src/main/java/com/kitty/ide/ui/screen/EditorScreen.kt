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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Terminal
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitty.ide.data.editor.UndoManager
import com.kitty.ide.data.model.FileType
import com.kitty.ide.data.model.ProjectFile
import com.kitty.ide.data.terminal.TerminalManager
import com.kitty.ide.ui.component.TerminalPanel
import com.kitty.ide.ui.viewmodel.EditorViewModel
import com.kitty.ide.util.NameValidator
import java.io.File

/** 新建项类型 */
enum class NewItemType(val title: String, val defaultExt: String?, val hint: String) {
    FILE("新建文件", null, "输入完整文件名，例如 main.py"),
    FOLDER("新建文件夹", null, "输入文件夹名"),
    HTML("新建 HTML 文件", ".html", "输入文件名（自动补 .html）"),
    CSS("新建 CSS 文件", ".css", "输入文件名（自动补 .css）"),
    JS("新建 JS 文件", ".js", "输入文件名（自动补 .js）")
}

/** 返回每个新建类型对应的图标 */
private fun iconForNewItem(type: NewItemType): ImageVector = when (type) {
    NewItemType.FILE -> Icons.Outlined.NoteAdd
    NewItemType.FOLDER -> Icons.Outlined.CreateNewFolder
    NewItemType.HTML -> Icons.Outlined.Code
    NewItemType.CSS -> Icons.Outlined.Brush
    NewItemType.JS -> Icons.Outlined.Terminal
}

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
    val clipboard = LocalClipboardManager.current
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
    var newItemNameError by remember { mutableStateOf<String?>(null) }
    var newItemParentDir by remember { mutableStateOf<File?>(null) }
    var expandedMenuPath by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ProjectFile?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var renameError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ProjectFile?>(null) }

    var fileTreeVersion by remember { mutableIntStateOf(0) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

    var canUndoState by remember { mutableStateOf(false) }
    var canRedoState by remember { mutableStateOf(false) }

    val activePath: String? = if (activeIndex in openFiles.indices) {
        openFiles[activeIndex].file.absolutePath
    } else null

    LaunchedEffect(activeIndex) {
        if (activeIndex in openFiles.indices) {
            val file = openFiles[activeIndex].file
            val value = TextFieldValue(
                text = openFiles[activeIndex].content,
                selection = TextRange(0)
            )
            textFieldValue = value
            UndoManager.initFile(file.absolutePath, value)
            canUndoState = UndoManager.canUndo(file.absolutePath)
            canRedoState = UndoManager.canRedo(file.absolutePath)
        } else {
            textFieldValue = TextFieldValue("")
            canUndoState = false
            canRedoState = false
        }
    }

    LaunchedEffect(projectPath) {
        viewModel.loadProject(projectPath)
    }

    fun handleNewItem(parent: File, type: NewItemType) {
        newItemType = type
        newItemName = ""
        newItemNameError = null
        newItemParentDir = parent
        showNewItemDialog = true
    }

    fun handleRename(target: ProjectFile) {
        renameTarget = target
        renameValue = target.name
        renameError = null
        showRenameDialog = true
    }

    fun handleCopyPath(target: ProjectFile) {
        clipboard.setText(AnnotatedString(target.file.absolutePath))
        Toast.makeText(context, "路径已复制", Toast.LENGTH_SHORT).show()
    }

    fun handleDelete(target: ProjectFile) {
        deleteTarget = target
        showDeleteConfirm = true
    }

    fun handleUndo() {
        val path = activePath ?: return
        val result = UndoManager.undo(path, textFieldValue)
        if (result != null) {
            textFieldValue = result
            viewModel.updateActiveContent(result.text)
            canUndoState = UndoManager.canUndo(path)
            canRedoState = UndoManager.canRedo(path)
        }
    }

    fun handleRedo() {
        val path = activePath ?: return
        val result = UndoManager.redo(path)
        if (result != null) {
            textFieldValue = result
            viewModel.updateActiveContent(result.text)
            canUndoState = UndoManager.canUndo(path)
            canRedoState = UndoManager.canRedo(path)
        }
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

            IconButton(onClick = { showTerminal = !showTerminal }) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "终端",
                    tint = if (showTerminal) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurface
                )
            }

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

        // ── 主体区域 ──
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
                        text = "长按空白 / 文件可弹出菜单",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(8.dp))

                    // 用 Box 包裹整个区域，让空白长按的小菜单锚定在这里
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .combinedClickable(
                                    onClick = { },
                                    onLongClick = {
                                        // 长按空白 → 项目根目录新建菜单（小菜单）
                                        newItemParentDir = null
                                        expandedMenuPath = null
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
                                    expandedMenuPath = expandedMenuPath,
                                    onMenuToggle = { expandedMenuPath = it },
                                    onFileClick = { f ->
                                        if (f.fileType == FileType.TEXT) {
                                            viewModel.openFile(f.file)
                                        } else {
                                            Toast.makeText(context, "暂不支持预览此格式", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onNewItem = { parent, type -> handleNewItem(parent, type) },
                                    onRename = { handleRename(it) },
                                    onCopyPath = { handleCopyPath(it) },
                                    onDelete = { handleDelete(it) },
                                    viewModel = viewModel
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }

                        // ── 空白长按的小菜单 ──
                        DropdownMenu(
                            expanded = showFileTreeMenu,
                            onDismissRequest = { showFileTreeMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("新建文件") },
                                leadingIcon = { MenuIcon(Icons.Outlined.NoteAdd) },
                                onClick = {
                                    showFileTreeMenu = false
                                    val dir = projectDir ?: return@DropdownMenuItem
                                    handleNewItem(dir, NewItemType.FILE)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("新建文件夹") },
                                leadingIcon = { MenuIcon(Icons.Outlined.CreateNewFolder) },
                                onClick = {
                                    showFileTreeMenu = false
                                    val dir = projectDir ?: return@DropdownMenuItem
                                    handleNewItem(dir, NewItemType.FOLDER)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("新建 HTML 文件") },
                                leadingIcon = { MenuIcon(Icons.Outlined.Code) },
                                onClick = {
                                    showFileTreeMenu = false
                                    val dir = projectDir ?: return@DropdownMenuItem
                                    handleNewItem(dir, NewItemType.HTML)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("新建 CSS 文件") },
                                leadingIcon = { MenuIcon(Icons.Outlined.Brush) },
                                onClick = {
                                    showFileTreeMenu = false
                                    val dir = projectDir ?: return@DropdownMenuItem
                                    handleNewItem(dir, NewItemType.CSS)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("新建 JS 文件") },
                                leadingIcon = { MenuIcon(Icons.Outlined.Terminal) },
                                onClick = {
                                    showFileTreeMenu = false
                                    val dir = projectDir ?: return@DropdownMenuItem
                                    handleNewItem(dir, NewItemType.JS)
                                }
                            )
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
                        activePath?.let { path ->
                            UndoManager.record(path, newValue)
                            canUndoState = UndoManager.canUndo(path)
                            canRedoState = UndoManager.canRedo(path)
                        }
                    },
                    focusRequestKey = activeIndex,
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
            canUndo = canUndoState,
            canRedo = canRedoState,
            onUndo = { handleUndo() },
            onRedo = { handleRedo() },
            onSymbolClick = { symbol ->
                val newValue = insertSymbol(textFieldValue, symbol)
                textFieldValue = newValue
                viewModel.updateActiveContent(newValue.text)
                activePath?.let { path ->
                    UndoManager.record(path, newValue)
                    canUndoState = UndoManager.canUndo(path)
                    canRedoState = UndoManager.canRedo(path)
                }
            },
            onMoreClick = {
                Toast.makeText(context, "符号栏设置待开发", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ═════════════════════════════════════════
    // 对话框们
    // ═════════════════════════════════════════

    if (showNewItemDialog) {
        AlertDialog(
            onDismissRequest = {
                showNewItemDialog = false
                newItemName = ""
                newItemNameError = null
            },
            title = { Text(newItemType.title) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = {
                            newItemName = it
                            newItemNameError = null
                        },
                        label = { Text("名称") },
                        placeholder = { Text(newItemType.hint) },
                        singleLine = true,
                        isError = newItemNameError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (newItemNameError != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = newItemNameError!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newItemName
                        val error = NameValidator.validate(name)
                        if (error != null) {
                            newItemNameError = error
                            return@TextButton
                        }
                        val targetDir = newItemParentDir ?: projectDir
                        if (targetDir != null) {
                            expandedMap[targetDir.absolutePath] = true
                            autoExpandChain(targetDir, expandedMap, viewModel)

                            if (newItemType == NewItemType.FOLDER) {
                                viewModel.createFolder(targetDir, name) { success ->
                                    if (success) {
                                        fileTreeVersion++
                                    } else {
                                        newItemNameError = "创建失败，可能已存在"
                                    }
                                }
                            } else {
                                val ext = newItemType.defaultExt
                                val fileName = if (ext != null && !name.endsWith(ext)) name + ext else name
                                val finalError = NameValidator.validate(fileName)
                                if (finalError != null) {
                                    newItemNameError = finalError
                                    return@TextButton
                                }
                                viewModel.createFile(targetDir, fileName) { success ->
                                    if (success) {
                                        fileTreeVersion++
                                    } else {
                                        newItemNameError = "创建失败，可能已存在"
                                    }
                                }
                            }
                            showNewItemDialog = false
                            newItemName = ""
                            newItemNameError = null
                            newItemParentDir = null
                        }
                    }
                ) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNewItemDialog = false
                    newItemName = ""
                    newItemNameError = null
                }) { Text("取消") }
            }
        )
    }

    if (showRenameDialog && renameTarget != null) {
        val target = renameTarget!!
        AlertDialog(
            onDismissRequest = {
                showRenameDialog = false
                renameValue = ""
                renameError = null
                renameTarget = null
            },
            title = { Text("重命名") },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameValue,
                        onValueChange = {
                            renameValue = it
                            renameError = null
                        },
                        label = { Text("新名称") },
                        singleLine = true,
                        isError = renameError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (renameError != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = renameError!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = renameValue
                    val error = NameValidator.validate(name)
                    if (error != null) {
                        renameError = error
                        return@TextButton
                    }
                    viewModel.renameFile(target.file, name) { success ->
                        if (success) {
                            fileTreeVersion++
                            showRenameDialog = false
                            renameValue = ""
                            renameTarget = null
                        } else {
                            renameError = "重命名失败，可能已存在同名项"
                        }
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRenameDialog = false
                    renameValue = ""
                    renameError = null
                    renameTarget = null
                }) { Text("取消") }
            }
        )
    }

    if (showDeleteConfirm && deleteTarget != null) {
        val target = deleteTarget!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false; deleteTarget = null },
            title = { Text("删除确认") },
            text = {
                Text(
                    if (target.isDirectory)
                        "确定要删除文件夹「${target.name}」及其全部内容吗？此操作不可撤销。"
                    else
                        "确定要删除「${target.name}」吗？此操作不可撤销。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFile(target.file) { success ->
                        if (success) {
                            fileTreeVersion++
                            Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                    showDeleteConfirm = false
                    deleteTarget = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    deleteTarget = null
                }) { Text("取消") }
            }
        )
    }
}

@Composable
fun NewMenuOptions(onSelect: (NewItemType) -> Unit) {
    MenuOption("新建文件", Icons.Outlined.NoteAdd) { onSelect(NewItemType.FILE) }
    MenuOption("新建文件夹", Icons.Outlined.CreateNewFolder) { onSelect(NewItemType.FOLDER) }
    MenuOption("新建 HTML 文件", Icons.Outlined.Code) { onSelect(NewItemType.HTML) }
    MenuOption("新建 CSS 文件", Icons.Outlined.Brush) { onSelect(NewItemType.CSS) }
    MenuOption("新建 JS 文件", Icons.Outlined.Terminal) { onSelect(NewItemType.JS) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileTreeItem(
    file: ProjectFile,
    depth: Int,
    expandedMap: MutableMap<String, Boolean>,
    version: Int,
    expandedMenuPath: String?,
    onMenuToggle: (String?) -> Unit,
    onFileClick: (ProjectFile) -> Unit,
    onNewItem: (File, NewItemType) -> Unit,
    onRename: (ProjectFile) -> Unit,
    onCopyPath: (ProjectFile) -> Unit,
    onDelete: (ProjectFile) -> Unit,
    viewModel: EditorViewModel
) {
    val path = file.file.absolutePath
    val isExpanded = expandedMap[path] == true
    val menuExpanded = expandedMenuPath == path

    Column(modifier = Modifier.fillMaxWidth()) {
        Box {
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
                        onLongClick = { onMenuToggle(path) }
                    )
                    .padding(
                        start = (depth * 16).dp,
                        top = 8.dp,
                        bottom = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (file.isDirectory) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandMore
                                      else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Spacer(Modifier.width(16.dp))
                }

                Icon(
                    imageVector = if (file.isDirectory) Icons.Outlined.Folder
                                  else Icons.Outlined.InsertDriveFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 2.dp, end = 8.dp)
                        .size(16.dp)
                )

                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { onMenuToggle(null) }
            ) {
                if (file.isDirectory) {
                    DropdownMenuItem(
                        text = { Text("新建文件") },
                        leadingIcon = { MenuIcon(Icons.Outlined.NoteAdd) },
                        onClick = { onMenuToggle(null); onNewItem(file.file, NewItemType.FILE) }
                    )
                    DropdownMenuItem(
                        text = { Text("新建文件夹") },
                        leadingIcon = { MenuIcon(Icons.Outlined.CreateNewFolder) },
                        onClick = { onMenuToggle(null); onNewItem(file.file, NewItemType.FOLDER) }
                    )
                    DropdownMenuItem(
                        text = { Text("新建 HTML 文件") },
                        leadingIcon = { MenuIcon(Icons.Outlined.Code) },
                        onClick = { onMenuToggle(null); onNewItem(file.file, NewItemType.HTML) }
                    )
                    DropdownMenuItem(
                        text = { Text("新建 CSS 文件") },
                        leadingIcon = { MenuIcon(Icons.Outlined.Brush) },
                        onClick = { onMenuToggle(null); onNewItem(file.file, NewItemType.CSS) }
                    )
                    DropdownMenuItem(
                        text = { Text("新建 JS 文件") },
                        leadingIcon = { MenuIcon(Icons.Outlined.Terminal) },
                        onClick = { onMenuToggle(null); onNewItem(file.file, NewItemType.JS) }
                    )
                    DropdownMenuItem(
                        text = { Text("重命名") },
                        leadingIcon = { MenuIcon(Icons.Outlined.DriveFileRenameOutline) },
                        onClick = { onMenuToggle(null); onRename(file) }
                    )
                    DropdownMenuItem(
                        text = { Text("复制路径") },
                        leadingIcon = { MenuIcon(Icons.Outlined.ContentCopy) },
                        onClick = { onMenuToggle(null); onCopyPath(file) }
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        leadingIcon = { MenuIcon(Icons.Outlined.Delete) },
                        onClick = { onMenuToggle(null); onDelete(file) }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("重命名") },
                        leadingIcon = { MenuIcon(Icons.Outlined.DriveFileRenameOutline) },
                        onClick = { onMenuToggle(null); onRename(file) }
                    )
                    DropdownMenuItem(
                        text = { Text("复制路径") },
                        leadingIcon = { MenuIcon(Icons.Outlined.ContentCopy) },
                        onClick = { onMenuToggle(null); onCopyPath(file) }
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        leadingIcon = { MenuIcon(Icons.Outlined.Delete) },
                        onClick = { onMenuToggle(null); onDelete(file) }
                    )
                }
            }
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
                            expandedMenuPath = expandedMenuPath,
                            onMenuToggle = onMenuToggle,
                            onFileClick = onFileClick,
                            onNewItem = onNewItem,
                            onRename = onRename,
                            onCopyPath = onCopyPath,
                            onDelete = onDelete,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

/** 菜单项的统一图标样式（20dp） */
@Composable
private fun MenuIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
fun MenuOption(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SymbolToolbar(
    modifier: Modifier = Modifier,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSymbolClick: (String) -> Unit,
    onMoreClick: () -> Unit
) {
    val symbols = listOf(
        "Tab", "{}", "[]", "()", "<>", "\"\"", "''",
        ";", ":", ",", ".", "!", "?", "=", "+", "-",
        "*", "/", "\\", "_", "|", "#", "$", "%", "&",
        "@", "^", "~"
    )

    val disabledColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    val enabledColor = MaterialTheme.colorScheme.onSurfaceVariant

    LazyRow(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .clickable(enabled = canUndo) { onUndo() }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "撤销",
                    tint = if (canUndo) enabledColor else disabledColor,
                    modifier = Modifier.width(20.dp).height(20.dp)
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .clickable(enabled = canRedo) { onRedo() }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "重做",
                    tint = if (canRedo) enabledColor else disabledColor,
                    modifier = Modifier.width(20.dp).height(20.dp)
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            )
        }

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

/**
 * 代码编辑区
 */
@Composable
fun CodeEditorWithLineNumbers(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    focusRequestKey: Any? = null,
    modifier: Modifier = Modifier
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val lineCount = value.text.lines().size.coerceAtLeast(1)
    var fontSize by remember { mutableFloatStateOf(15f) }
    val lineHeight = fontSize * 1.5f

    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(focusRequestKey) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
        }
    }

    val cursorLineColor = Color(0x14FFFFFF)
    val guideColor = Color(0x2AFFFFFF)

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
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                val newValue = handleEnter(value)
                                if (newValue != null) {
                                    onValueChange(newValue)
                                    return@onPreviewKeyEvent true
                                }
                            }
                            false
                        }
                        .drawBehind {
                            val layout = textLayout ?: return@drawBehind

                            val cursorOffset = value.selection.start.coerceIn(0, value.text.length)
                            val cursorLine = layout.getLineForOffset(cursorOffset)
                            val top = layout.getLineTop(cursorLine)
                            val bottom = layout.getLineBottom(cursorLine)
                            drawRect(
                                color = cursorLineColor,
                                topLeft = Offset(0f, top),
                                size = Size(size.width, bottom - top)
                            )

                            val text = value.text
                            for (line in 0 until layout.lineCount) {
                                val lineStart = layout.getLineStart(line)
                                val lineEnd = layout.getLineEnd(line)

                                var spaces = 0
                                var i = lineStart
                                while (i < lineEnd && i < text.length) {
                                    val ch = text[i]
                                    if (ch == ' ') {
                                        spaces++
                                        i++
                                    } else if (ch == '\t') {
                                        spaces += 4
                                        i++
                                    } else {
                                        break
                                    }
                                }
                                val indentLevel = spaces / 4
                                if (indentLevel == 0) continue

                                val lineTop = layout.getLineTop(line)
                                val lineBottom = layout.getLineBottom(line)

                                for (level in 1..indentLevel) {
                                    val charOffset = lineStart + level * 4
                                    if (charOffset > text.length) continue
                                    val x = layout.getHorizontalPosition(charOffset, true)
                                    drawLine(
                                        color = guideColor,
                                        start = Offset(x, lineTop),
                                        end = Offset(x, lineBottom),
                                        strokeWidth = 2f
                                    )
                                }
                            }
                        }
                        .pointerInput(textLayout) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == PointerEventType.Press) {
                                        val change = event.changes.firstOrNull() ?: continue
                                        if (!change.changedToDown()) continue
                                        val pos = change.position
                                        val layout = textLayout
                                        if (layout != null) {
                                            val line = layout.getLineForVerticalPosition(pos.y)
                                            val lineLeft = layout.getLineLeft(line)
                                            val lineRight = layout.getLineRight(line)
                                            val lineTop = layout.getLineTop(line)
                                            val lineBottom = layout.getLineBottom(line)
                                            val inTextBounds = pos.x in lineLeft..lineRight &&
                                                               pos.y in lineTop..lineBottom
                                            if (!inTextBounds) {
                                                focusRequester.requestFocus()
                                                change.consume()
                                            }
                                        }
                                    }
                                }
                            }
                        },
                    onTextLayout = { textLayout = it },
                    textStyle = TextStyle(
                        color = Color(0xFFD4D4D4),
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSize.sp,
                        lineHeight = lineHeight.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF1A73E8))
                )
            }
        }
    }
}