package com.example.learningreminder.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.learningreminder.reminder.ReminderScheduler

/** PRD 3.4：权限关闭或受限时，在应用入口明确引导，不静默失效。 */
@Composable
fun ReminderPermissionDialog() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationsAllowed by remember { mutableStateOf(true) }
    var exactAlarmsAllowed by remember { mutableStateOf(true) }
    var dismissed by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val refresh = {
            notificationsAllowed = notificationsAllowed(context)
            exactAlarmsAllowed = ReminderScheduler(context).canScheduleExact()
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        refresh()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (dismissed || (notificationsAllowed && exactAlarmsAllowed)) return

    AlertDialog(
        onDismissRequest = { dismissed = true },
        title = { Text("提醒权限需要处理") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!notificationsAllowed) {
                    Text("通知权限已关闭，系统提醒将无法送达。")
                }
                if (!exactAlarmsAllowed) {
                    Text("精确闹钟受限，提醒可能略有延迟。")
                }
            }
        },
        confirmButton = {
            Row {
                if (!notificationsAllowed) {
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    }) { Text("通知设置") }
                }
                if (!exactAlarmsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }) { Text("精确闹钟") }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { dismissed = true }) { Text("稍后") }
        }
    )
}

private fun notificationsAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
