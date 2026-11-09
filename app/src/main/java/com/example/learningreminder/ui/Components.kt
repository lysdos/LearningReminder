package com.example.learningreminder.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learningreminder.data.ChildProfile

/**
 * 原型通用组件集合：头像、顶栏、分段标题、标签、状态药丸、开关行、权限行等。
 * 各屏幕统一从这里取样式，避免与原型 HTML 的类名一一对应实现。
 */

/** 圆形头像：渐变底 + 首字。 */
@Composable
fun ChildAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 38.dp,
    fontSize: Int = 16
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarBrush(name)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = avatarInitial(name),
            color = Color.White,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 原型 .topbar：头像 + 孩子名 + 副标题 + 铃铛(带红点) + 月亮。
 */
@Composable
fun AppTopBar(
    child: ChildProfile?,
    subtitle: String,
    onChildClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onDndClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (child != null) {
                ChildAvatar(name = child.name)
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onChildClick)
            ) {
                Text(
                    text = child?.name ?: "选择孩子",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = appPalette.ink3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TopBarIconButton(
                icon = Icons.Rounded.NotificationsNone,
                contentDescription = "提醒通知",
                showBadge = true,
                onClick = onNotificationClick
            )
            Spacer(modifier = Modifier.width(8.dp))
            TopBarIconButton(
                icon = Icons.Rounded.DarkMode,
                contentDescription = "免打扰",
                onClick = onDndClick
            )
        }
    }
}

/** 原型 .icon-btn：浅灰圆形图标按钮，可选红点。 */
@Composable
fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBadge: Boolean = false
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(19.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        if (showBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(appPalette.red)
            )
        }
    }
}

/** 原型 .sec-head：左侧标题 + 右侧动作。 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = appPalette.brand,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

/** 原型 .lbl：分组小标签。 */
@Composable
fun GroupLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = appPalette.ink3,
        modifier = modifier.padding(start = 2.dp, bottom = 8.dp)
    )
}

/** 原型 .chip：白底描边小圆角标签，选中态为主色实心。 */
@Composable
fun AppChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val bg = if (selected) appPalette.brand else MaterialTheme.colorScheme.surface
    val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(
                width = 1.dp,
                color = if (selected) appPalette.brand else appPalette.line,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = fg
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = fg)
    }
}

/** 原型 .tag：小圆角标签（科目 / 时间 / 依据）。 */
@Composable
fun MiniTag(
    text: String,
    kind: TagKind,
    modifier: Modifier = Modifier
) {
    val palette = appPalette
    val (bg, fg) = when (kind) {
        TagKind.Subject -> palette.brandSoft to palette.brandDark
        TagKind.Time -> MaterialTheme.colorScheme.background to palette.ink2
        TagKind.Evidence -> palette.orangeSoft to palette.orangeText
        TagKind.Neutral -> MaterialTheme.colorScheme.background to palette.ink2
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

enum class TagKind { Subject, Time, Evidence, Neutral }

/** 原型 .staus-pill：状态药丸。 */
@Composable
fun StatusPill(
    status: String?,
    modifier: Modifier = Modifier
) {
    val palette = appPalette
    val (bg, fg, label) = statusPillStyle(status)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

data class PillStyle(val bg: Color, val fg: Color, val label: String)

@Composable
fun statusPillStyle(status: String?): PillStyle {
    val palette = appPalette
    return when (status) {
        null -> PillStyle(palette.brandSoft, palette.brandDark, "未开始")
        "DONE" -> PillStyle(palette.greenSoft, palette.green, "已完成")
        "PARTIAL" -> PillStyle(palette.orangeSoft, palette.orangeText, "部分完成")
        "IN_PROGRESS" -> PillStyle(palette.orangeSoft, palette.orangeText, "进行中")
        "ASK_LEAVE" -> PillStyle(palette.purpleSoft, palette.purple, "请假")
        "SKIP_TODAY" -> PillStyle(MaterialTheme.colorScheme.background, palette.ink3, "今日跳过")
        "EXPIRED" -> PillStyle(palette.redSoft, palette.red, "超时未做")
        "NO_SHOW" -> PillStyle(palette.redSoft, palette.red, "没做")
        "NOT_STARTED" -> PillStyle(palette.brandSoft, palette.brandDark, "未开始")
        else -> PillStyle(MaterialTheme.colorScheme.background, palette.ink3, statusLabel(status))
    }
}

/**
 * 原型 .check：26px 圆形状态标记，内含状态 emoji。
 * 原型用 ✅▶️⏰⬜📝 等 emoji 直接表达状态，这里保持一致。
 */
@Composable
fun StatusCheck(
    status: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val palette = appPalette
    val (bg, border, glyph) = when (status) {
        "DONE" -> Triple(palette.green, palette.green, "✅")
        "PARTIAL" -> Triple(palette.orange, palette.orange, "🔶")
        "IN_PROGRESS" -> Triple(Color(0xFF3D8BFF), Color(0xFF3D8BFF), "▶️")
        "ASK_LEAVE" -> Triple(palette.purple, palette.purple, "📝")
        "SKIP_TODAY" -> Triple(palette.ink3, palette.ink3, "⏭️")
        "EXPIRED" -> Triple(palette.red, palette.red, "⏰")
        "NO_SHOW" -> Triple(palette.red, palette.red, "❌")
        else -> Triple(Color.Transparent, palette.track, "⬜")
    }
    Box(
        modifier = modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(bg)
            .border(width = 2.dp, color = border, shape = CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(text = glyph, fontSize = 12.sp)
    }
}

/** 原型 .toggle-row：标题 + 说明 + 右侧开关。 */
@Composable
fun ToggleRow(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelSmall,
                        color = appPalette.ink3
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            AppSwitch(checked = checked, onCheckedChange = onCheckedChange)
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(appPalette.line)
            )
        }
    }
}

/**
 * 原型 .switch：46x27 的胶囊开关，绿色激活。
 * Material3 的 Switch 尺寸与原型差异较大，这里自绘以精确对齐。
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColor = if (checked) appPalette.green else appPalette.track
    Box(
        modifier = modifier
            .width(46.dp)
            .height(27.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(trackColor)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .size(23.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/** 原型 .perm：权限条目，左图标 + 说明 + 右侧状态。 */
@Composable
fun PermissionRow(
    emoji: String,
    title: String,
    description: String,
    statusText: String,
    statusKind: PermissionStatusKind,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val palette = appPalette
    val (statusBg, statusFg) = when (statusKind) {
        PermissionStatusKind.Ok -> palette.greenSoft to palette.green
        PermissionStatusKind.Partial -> palette.orangeSoft to palette.orangeText
        PermissionStatusKind.Missing -> palette.redSoft to palette.red
    }
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 13.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = emoji,
                fontSize = 18.sp,
                modifier = Modifier.width(34.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = appPalette.ink3,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(statusBg)
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusFg
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(appPalette.line)
        )
    }
}

enum class PermissionStatusKind { Ok, Partial, Missing }

/** 原型 .list-item：图标 + 主副标题 + 右侧箭头。 */
@Composable
fun ListItemRow(
    icon: ImageVector?,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    showArrow: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = appPalette.ink3
                    )
                }
            }
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelSmall,
                    color = appPalette.ink3
                )
            }
            if (showArrow) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "›",
                    style = MaterialTheme.typography.bodyMedium,
                    color = appPalette.ink3
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(appPalette.line)
        )
    }
}

/** 原型 .card：通用白卡。 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(14.dp),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(AppRadius),
        border = BorderStroke(1.dp, appPalette.line),
        content = {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    )
}

/** 原型 .mini-stat：小统计块。 */
@Composable
fun MiniStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(AppRadiusSmall),
        border = BorderStroke(1.dp, appPalette.line)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = appPalette.ink3
            )
        }
    }
}

/** 原型 .alert：提示条（warn / error）。 */
@Composable
fun AlertBanner(
    text: String,
    modifier: Modifier = Modifier,
    kind: AlertKind = AlertKind.Warn
) {
    val palette = appPalette
    val (bg, fg) = when (kind) {
        AlertKind.Warn -> palette.orangeSoft to palette.orangeText
        AlertKind.Error -> palette.redSoft to palette.redText
        AlertKind.Info -> palette.brandSoft to palette.brandDark
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadiusSmall))
            .background(bg)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = when (kind) {
                AlertKind.Warn -> "⚠️"
                AlertKind.Error -> "⛔"
                AlertKind.Info -> "💡"
            },
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = fg,
            modifier = Modifier.weight(1f)
        )
    }
}

enum class AlertKind { Warn, Error, Info }

/** 原型 .empty：空状态。 */
@Composable
fun EmptyState(
    emoji: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, fontSize = 40.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = appPalette.ink3
        )
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(14.dp))
            PrimaryButton(text = actionText, onClick = onAction)
        }
    }
}

/** 原型 .btn-main：主按钮。 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AppRadiusSmall))
            .background(if (enabled) appPalette.brand else appPalette.track)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

/** 原型 .btn-line：次要描边按钮。 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false
) {
    val borderColor = if (danger) appPalette.red else appPalette.line
    val textColor = if (danger) appPalette.red else appPalette.ink2
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AppRadiusSmall))
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 1.5.dp, color = borderColor, shape = RoundedCornerShape(AppRadiusSmall))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) textColor else appPalette.ink3
        )
    }
}
