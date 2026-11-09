package com.example.learningreminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 原型 #notifSheet：提醒通知面板，包含待处理提醒、权限异常与最近提醒。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterSheet(
    onDismiss: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onToast: (String) -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SheetHeader(title = "提醒通知", onClose = onDismiss)

            NotificationCard(
                icon = Icons.Rounded.Schedule,
                title = "📖 阅读 · 晚上朗读 20 分钟",
                description = "提醒时间 19:15 · 孩子：阿里",
                accent = appPalette.orange,
                background = appPalette.orangeSoft,
                descriptionColor = appPalette.orangeText,
                actions = listOf(
                    SheetAction("查看详情", primary = true) { onOpenTask(1L) },
                    SheetAction("稍后提醒") { onToast("已稍后 10 分钟提醒") }
                )
            )

            NotificationCard(
                icon = Icons.Rounded.ReportProblem,
                title = "📮 2 条提醒未送达",
                description = "精确闹钟权限被限制，提醒已降级为近似提醒。",
                accent = appPalette.red,
                background = appPalette.redSoft,
                descriptionColor = appPalette.redText,
                actions = listOf(
                    SheetAction("修复权限", primary = true) { onToast("前往系统设置") }
                )
            )

            SectionHeader(
                title = "最近提醒",
                modifier = Modifier.padding(top = 6.dp)
            )

            RecentReminderRow(
                icon = Icons.Rounded.CalendarToday,
                title = "数学口算练习",
                subtitle = "今天 18:00 · 已提醒",
                trailing = "✓",
                trailingColor = appPalette.green
            )
            RecentReminderRow(
                icon = Icons.Rounded.Notifications,
                title = "英语听力 15 分钟",
                subtitle = "今天 17:30 · 已忽略",
                trailing = "—",
                trailingColor = appPalette.ink3
            )
        }
    }
}

/** 原型 .sheet-head：标题 + 关闭按钮。 */
@Composable
fun SheetHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.background)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✕",
                fontSize = 14.sp,
                color = appPalette.ink3
            )
        }
    }
}

/** 原型 .notif：左侧色条 + 浅色底的提醒卡。 */
@Composable
private fun NotificationCard(
    icon: ImageVector,
    title: String,
    description: String,
    accent: Color,
    background: Color,
    descriptionColor: Color,
    actions: List<SheetAction>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadiusSmall))
            .background(background)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 原型 border-left: 4px solid
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                .background(accent)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = descriptionColor,
                modifier = Modifier.padding(top = 3.dp)
            )
            if (actions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    actions.forEach { action ->
                        if (action.primary) {
                            PrimaryButton(
                                text = action.text,
                                onClick = action.onClick,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            SecondaryButton(
                                text = action.text,
                                onClick = action.onClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class SheetAction(
    val text: String,
    val primary: Boolean = false,
    val onClick: () -> Unit
)

/** 原型 .list-item 变体：最近提醒列表。 */
@Composable
private fun RecentReminderRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: String,
    trailingColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = appPalette.ink3
            )
        }
        Text(
            text = trailing,
            style = MaterialTheme.typography.bodyMedium,
            color = trailingColor
        )
    }
}
