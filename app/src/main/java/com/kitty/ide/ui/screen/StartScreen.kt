package com.kitty.ide.ui.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitty.ide.R
import com.kitty.ide.data.model.ProjectMeta
import com.kitty.ide.data.repository.ProjectRepository
import com.kitty.ide.ui.component.KittyActionButton
import com.kitty.ide.ui.viewmodel.ProjectViewModel

@Composable
fun StartScreen(
    onOpenProject: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectViewModel = viewModel()
) {
    val context = LocalContext.current
    val recentProjects by viewModel.recentProjects.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

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

    // 合并排序：最近打开的排在前面
    // 用 recentProjects 的 lastOpened 匹配，匹配不到则用文件夹 lastModified 兜底
    val recentMap = recentProjects.associateBy { it.projectPath }
    val sortedProjects = allProjects.sortedByDescending { info ->
        recentMap[info.dir.absolutePath]?.lastOpened ?: info.dir.lastModified()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp)
    ) {
        Spacer(Modifier.height(80.dp))

        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🐱", fontSize = 48.sp)
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

        // ── 所有项目（平铺，最近打开的排最前面） ──
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
                    Text(text = "📦", fontSize = 20.sp)
                    Spacer(Modifier.size(12.dp))
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
            text = stringResource(R.string.start_version),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(32.dp))
    }

    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onConfirm = { name, version, desc, lang, createSample ->
                val meta = ProjectMeta(
                    projectName = name,
                    projectVersion = version,
                    ideVersion = "0.0.6",
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建项目") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("项目名 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), version.trim(), desc.trim(), language, createSample)
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}