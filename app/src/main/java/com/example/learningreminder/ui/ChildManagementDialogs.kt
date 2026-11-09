package com.example.learningreminder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.example.learningreminder.data.ChildProfile
import com.example.learningreminder.viewmodel.SettingsViewModel

@Composable
fun ChildRenameDialog(
    child: ChildProfile,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    var name by remember(child.id) { mutableStateOf(child.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改孩子名字") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名字") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    viewModel.updateChildName(child.id, name)
                    onDismiss()
                }
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun ChildDeleteDialog(
    child: ChildProfile,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    var counts by remember(child.id) { mutableStateOf<Pair<Int, Int>?>(null) }
    var deleting by remember(child.id) { mutableStateOf(false) }

    LaunchedEffect(child.id) {
        counts = runCatching { viewModel.childCounts(child.id) }.getOrNull()
    }

    AlertDialog(
        onDismissRequest = { if (!deleting) onDismiss() },
        title = { Text("删除孩子“${child.name}”") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (counts == null) {
                        "正在统计关联数据..."
                    } else {
                        "该孩子有 ${counts!!.first} 个任务、${counts!!.second} 条完成记录。"
                    }
                )
                Text(
                    "删除后任务和记录都会一并移除，且无法恢复。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = counts != null && !deleting,
                onClick = {
                    deleting = true
                    viewModel.deleteChild(child.id)
                    onDismiss()
                }
            ) { Text("确认删除") }
        },
        dismissButton = {
            TextButton(enabled = !deleting, onClick = onDismiss) { Text("取消") }
        }
    )
}
