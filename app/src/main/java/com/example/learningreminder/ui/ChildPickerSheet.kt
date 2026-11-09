package com.example.learningreminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.learningreminder.data.ChildProfile
import com.example.learningreminder.viewmodel.SettingsViewModel
import java.time.LocalDate

/** 顶栏"孩子"入口：切换当前孩子 / 新增 / 删除（PRD 家长角色）。对齐原型 #childSheet。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildPickerSheet(
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val children by settingsViewModel.children.collectAsState()
    val currentId by settingsViewModel.currentChildId.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<ChildProfile?>(null) }
    var deleteTarget by remember { mutableStateOf<ChildProfile?>(null) }
    var manageTarget by remember { mutableStateOf<ChildProfile?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SheetHeader(title = "选择孩子", onClose = onDismiss)

            children.forEach { child ->
                ChildPickerRow(
                    child = child,
                    isCurrent = child.id == currentId,
                    onSelect = {
                        settingsViewModel.selectChild(child.id)
                        onDismiss()
                    },
                    onRename = { renameTarget = child },
                    onDelete = { deleteTarget = child }
                )
            }

            SecondaryButton(
                text = "＋ 添加新孩子",
                onClick = { showAdd = true },
                modifier = Modifier.padding(top = 4.dp)
            )

            // 原型把改名/删除放在子任务管理入口，这里为每个孩子保留操作按钮
            if (children.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryButton(
                        text = "管理孩子",
                        onClick = { manageTarget = children.firstOrNull { it.id != currentId } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text(
                text = "共 ${children.size} 位孩子",
                style = MaterialTheme.typography.labelSmall,
                color = appPalette.ink3
            )
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("添加孩子") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("名字") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        settingsViewModel.addChild(newName.trim(), null)
                        newName = ""
                        showAdd = false
                    }
                }) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("取消") }
            }
        )
    }

    manageTarget?.let { child ->
        AlertDialog(
            onDismissRequest = { manageTarget = null },
            title = { Text("管理 ${child.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "可以为孩子改名，或删除该孩子的数据。",
                        style = MaterialTheme.typography.bodySmall,
                        color = appPalette.ink3
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton(
                            text = "改名",
                            onClick = {
                                renameTarget = child
                                manageTarget = null
                            }
                        )
                        SecondaryButton(
                            text = "删除",
                            onClick = {
                                deleteTarget = child
                                manageTarget = null
                            },
                            danger = true
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { manageTarget = null }) { Text("关闭") }
            }
        )
    }

    renameTarget?.let { child ->
        ChildRenameDialog(
            child = child,
            viewModel = settingsViewModel,
            onDismiss = { renameTarget = null }
        )
    }
    deleteTarget?.let { child ->
        ChildDeleteDialog(
            child = child,
            viewModel = settingsViewModel,
            onDismiss = { deleteTarget = null }
        )
    }
}

/** 原型 .picker-item：头像 + 名字 + 年龄 + 右侧状态（当前 ✓ / 其他 ›）。 */
@Composable
private fun ChildPickerRow(
    child: ChildProfile,
    isCurrent: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = appPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChildAvatar(name = child.name, size = 30.dp, fontSize = 13)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = child.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = childSubtitleFor(child.birthDate),
                style = MaterialTheme.typography.labelSmall,
                color = palette.ink3
            )
        }
        if (isCurrent) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.titleMedium,
                color = palette.green
            )
        } else {
            Text(
                text = "›",
                style = MaterialTheme.typography.titleMedium,
                color = palette.ink3
            )
        }
    }
}

/** 孩子副标题：年龄 + 年级；无生日时退化为今天日期。 */
private fun childSubtitleFor(birthDate: String?): String {
    val today = LocalDate.now()
    if (birthDate.isNullOrBlank()) return "未设置生日"
    val birth = runCatching { LocalDate.parse(birthDate) }.getOrNull() ?: return "未设置生日"
    val age = today.year - birth.year -
        if (today.monthValue < birth.monthValue ||
            (today.monthValue == birth.monthValue && today.dayOfMonth < birth.dayOfMonth)
        ) 1 else 0
    val grade = today.year - birth.year - if (today.monthValue < 9) 1 else 0
    val gradeText = if (grade in 1..6) "$grade 年级" else "自主学习"
    return "$age 岁 · $gradeText"
}
