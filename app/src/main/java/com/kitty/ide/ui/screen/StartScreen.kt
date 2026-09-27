package com.kitty.ide.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitty.ide.AppInfo
import com.kitty.ide.R
import com.kitty.ide.data.model.ProjectMeta
import com.kitty.ide.data.repository.ProjectRepository
import com.kitty.ide.ui.component.KittyActionButton
import com.kitty.ide.ui.viewmodel.ProjectViewModel
import com.kitty.ide.util.NameValidator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * 从 assets 加载 Kitty 表情图片
 */
private fun loadAssetBitmap(context: Context, name: String): ImageBitmap? {
    return try {
        context.assets.open(name).use { input ->
            BitmapFactory.decodeStream(input)?.asImageBitmap()
        }
    } catch (e: Exception) {
        null
    }
}

/** 4 张 Kitty 图的文件名，顺序固定 */
private val KITTY_FILES = listOf(
    "kitty.png",          // 0 普通
    "weary_kitty.png",    // 1 惊恐
    "kissing_kitty.png",  // 2 亲亲
    "happy_kitty.png"     // 3 开心
)

@Composable
fun StartScreen(
    onOpenProject: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectViewModel = viewModel()
) {
    val context = LocalContext.current
    val recentProjects by viewModel.recentProjects.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // ── 猫咪彩蛋状态 ──
    // 预加载 4 张图，避免每次重组都解码
    val kittyBitmaps = remember {
        KITTY_FILES.map { loadAssetBitmap(context, it) }
    }
    var kittyIndex by remember { mutableIntStateOf(0) }
    val kittyRotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshRecentProjects()
        viewModel.refreshAllProjects()
    }

    LaunchedEffect(Unit) {
        if (!hasStoragePermission()) {
            showPermissionDialog = true
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("需要文件管理权限") },
            text = {
                Text(
                    "为了提供更好的代码编辑体验，Kitty IDE 需要访问所有文件。\n\n" +
                        "同意后，项目将保存在 /sdcard/Documents/Kitty/project/ 目录下。\n" +
                        "拒绝后，项目将保存在 App 私有目录中。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:${context.packageName}")
                        context.startActivity(intent)
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            )
                        )
                    }
                }) { Text("去开启") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) { Text("拒绝") }
            }
        )
    }

    val openProjectLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.openProjectFromSaf(it) { resultPath ->
                if (resultPath != null) onOpenProject(resultPath)
                else Toast.makeText(context, "暂不支持此目录", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val recentMap = recentProjects.associateBy { it.projectPath }
    val sortedProjects = allProjects.sortedByDescending { info ->
        recentMap[info.dir.absolutePath]?.lastOpened ?: info.dir.lastModified()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp)
        ) {
            Spacer(Modifier.height(80.dp))

            // ── 猫咪头像（点击旋转彩蛋，用图片替换 emoji） ──
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        scope.launch {
                            val delta = Random.nextInt(50, 381).toFloat()
                            val willChangeEmoji = Random.nextFloat() < 0.6f

                            if (willChangeEmoji) {
                                launch {
                                    delay(150)
                                    // 从 1~3 里随机选一个（0 是普通脸，不进随机池）
                                    kittyIndex = Random.nextInt(1, 4)
                                }
                            }

                            kittyRotation.animateTo(
                                targetValue = kittyRotation.value + delta,
                                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                            )

                            if (willChangeEmoji) {
                                delay(250)
                                kittyIndex = 0
                            }
                        }
                    }
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = kittyBitmaps.getOrNull(kittyIndex)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Kitty",
                        modifier = Modifier
                            .size(64.dp)
                            .graphicsLayer { rotationZ = kittyRotation.value }
                    )
                } else {
                    // 兜底：图片缺失时退回 emoji
                    Text(
                        text = "🐱",
                        fontSize = 48.sp,
                        modifier = Modifier.graphicsLayer { rotationZ = kittyRotation.value }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.start_slogan),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(48.dp))

            KittyActionButton(
                text = stringResource(R.string.start_new_project),
                primary = true,
                onClick = { showNewProjectDialog = true },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            KittyActionButton(
                text = stringResource(R.string.start_open_project),
                primary = false,
                onClick = { openProjectLauncher.launch(null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(48.dp))

            Text(
                text = "所有项目",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (sortedProjects.isEmpty()) {
                Text(
                    text = "暂无项目，去创建一个吧～",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                )
            } else {
                sortedProjects.forEach { info ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.openProjectByPath(info.dir.absolutePath) { path ->
                                    if (path != null) onOpenProject(path)
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 📦 → Icons.Outlined.Code
                        Icon(
                            imageVector = Icons.Outlined.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = info.meta.projectName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "v${info.meta.projectVersion} · ${info.meta.mainLanguage}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }

            Spacer(Modifier.height(48.dp))

            Text(
                text = "v${AppInfo.VERSION}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(32.dp))
        }

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 8.dp, top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "设置",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onConfirm = { name, version, desc, lang, createSample ->
                val meta = ProjectMeta(
                    projectName = name,
                    projectVersion = version,
                    ideVersion = AppInfo.VERSION,
                    description = desc,
                    mainLanguage = lang,
                    createdAt = ProjectRepository.nowIso()
                )
                viewModel.createProject(meta, createSample) { path ->
                    if (path != null) {
                        onOpenProject(path)
                    } else {
                        Toast.makeText(context, "创建失败，项目名可能重复", Toast.LENGTH_SHORT).show()
                    }
                }
                showNewProjectDialog = false
            }
        )
    }
}

@Composable
fun NewProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, version: String, desc: String, language: String, createSample: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var version by remember { mutableStateOf("v0.0.1") }
    var desc by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("Web") }
    var languageExpanded by remember { mutableStateOf(false) }
    var createSample by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建项目") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    label = { Text("项目名 *") },
                    singleLine = true,
                    isError = nameError != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (nameError != null) {
                    Text(
                        text = nameError!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                OutlinedTextField(
                    value = version,
                    onValueChange = { version = it },
                    label = { Text("初始版本") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("项目描述（选填）") },
                    modifier = Modifier.fillMaxWidth()
                )
                Box {
                    OutlinedTextField(
                        value = language,
                        onValueChange = {},
                        label = { Text("Program language") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { languageExpanded = true }
                    )
                    DropdownMenu(
                        expanded = languageExpanded,
                        onDismissRequest = { languageExpanded = false }
                    ) {
                        listOf("Web").forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang) },
                                onClick = {
                                    language = lang
                                    languageExpanded = false
                                }
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = createSample,
                        onCheckedChange = { createSample = it }
                    )
                    Text("创建示例文件 (index.html)")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val error = NameValidator.validate(name)
                    if (error != null) {
                        nameError = error
                        return@TextButton
                    }
                    onConfirm(name.trim(), version.trim(), desc.trim(), language, createSample)
                }
            ) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}