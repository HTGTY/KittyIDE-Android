package com.kitty.ide.ui.screen

import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val recentProjects by viewModel.recentProjects.collectAsState()

    // 控制新建项目弹窗
    var showNewProjectDialog by remember { mutableStateOf(false) }

    // 用这个变量暂存“新建项目”的元数据，等用户选完目录后再去创建
    var pendingMeta by remember { mutableStateOf<ProjectMeta?>(null) }
    var pendingCreateSample by remember { mutableStateOf(false) }

    // SAF 选择器：打开项目
    val openProjectLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.openProject(uri) { resultUri ->
                if (resultUri != null) onOpenProject(resultUri)
            }
        }
    }

    // SAF 选择器：新建项目（选择父目录）
    val createProjectLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        val meta = pendingMeta
        if (uri != null && meta != null) {
            viewModel.createProject(uri, meta, pendingCreateSample) { resultUri ->
                if (resultUri != null) onOpenProject(resultUri)
            }
        }
        pendingMeta = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp)
    ) {
        Spacer(Modifier.height(80.dp))

        // ── Logo 与标题 ──
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

        // ── 两个主按钮 ──
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

        // ── 最近项目列表 ──
        if (recentProjects.isNotEmpty()) {
            Text(
                text = "最近打开",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            recentProjects.forEach { record ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val uri = Uri.parse(record.uriString)
                            viewModel.openProject(uri) { resultUri ->
                                if (resultUri != null) onOpenProject(resultUri)
                            }
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "📁", fontSize = 20.sp)
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text(
                            text = record.projectName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
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

    // ── 新建项目对话框 ──
    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onConfirm = { name, version, desc, language, createSample ->
                pendingMeta = ProjectMeta(
                    projectName = name,
                    projectVersion = version,
                    ideVersion = "0.0.2",
                    description = desc,
                    mainLanguage = language,
                    createdAt = ProjectRepository.nowIso()
                )
                pendingCreateSample = createSample
                showNewProjectDialog = false
                // 触发系统文件选择器
                createProjectLauncher.launch(null)
            }
        )
    }
}

/**
 * 新建项目信息填写弹窗
 */
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
                // 编程语言下拉框
                Box {
                    OutlinedTextField(
                        value = language,
                        onValueChange = {},
                        label = { Text("Program language") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false // 禁用输入，只允许下拉
                    )
                    // 覆盖一个透明的可点击区域来触发下拉
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
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}