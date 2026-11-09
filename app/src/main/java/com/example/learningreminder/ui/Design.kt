package com.example.learningreminder.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 对齐《学习提醒App原型.html》的设计令牌。
 * 原型中的 CSS 变量在这里一一映射，Compose 侧不再各自硬编码颜色。
 */

/** 原型 --brand 系列 */
val BrandBlue = Color(0xFF4F8EF7)
val BrandBlueDark = Color(0xFF2F6DE0)
val BrandBlueSoft = Color(0xFFEAF2FF)

val StatusGreen = Color(0xFF2EBD7A)
val StatusGreenSoft = Color(0xFFE6F8F0)
val StatusOrange = Color(0xFFF5A623)
val StatusOrangeSoft = Color(0xFFFFF4E0)
val StatusOrangeText = Color(0xFFC77E00)
val StatusRed = Color(0xFFF2564D)
val StatusRedSoft = Color(0xFFFDECEB)
val StatusRedText = Color(0xFFC23A32)
val StatusPurple = Color(0xFF8E5BF2)
val StatusPurpleSoft = Color(0xFFF1ECFE)

val AppBg = Color(0xFFF5F7FB)
val CardWhite = Color(0xFFFFFFFF)
val InkMain = Color(0xFF1F2733)
val InkSecond = Color(0xFF5A6675)
val InkThird = Color(0xFF8A96A6)
val LineColor = Color(0xFFEDF0F5)

/** 原型头像渐变 */
val AvatarA = listOf(Color(0xFFFF9A6C), Color(0xFFFF6B88))
val AvatarB = listOf(Color(0xFF4F8EF7), Color(0xFF42C6FF))
val AvatarC = listOf(Color(0xFF56D39A), Color(0xFF4FDB9C))
val AvatarD = listOf(Color(0xFFB58BFF), Color(0xFF7E6BF7))

/** 今日页摘要卡渐变 */
val SummaryGradient = listOf(Color(0xFF4F8EF7), Color(0xFF6BA1FF))
/** 报告页摘要卡渐变 */
val ReportGradient = listOf(Color(0xFF42C6FF), Color(0xFF4F8EF7))

/** 原型 --radius: 16px */
val AppRadius = 16.dp
val AppRadiusSmall = 12.dp
val AppRadiusTiny = 10.dp

/** 原型阴影色 rgba(31,39,51,.06) */
val CardShadowColor = Color(0x0F1F2733)

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueSoft,
    onPrimaryContainer = BrandBlueDark,
    secondary = StatusGreen,
    onSecondary = Color.White,
    secondaryContainer = StatusGreenSoft,
    onSecondaryContainer = Color(0xFF22795A),
    tertiary = StatusOrange,
    onTertiary = Color.White,
    tertiaryContainer = StatusOrangeSoft,
    onTertiaryContainer = StatusOrangeText,
    error = StatusRed,
    onError = Color.White,
    errorContainer = StatusRedSoft,
    onErrorContainer = StatusRedText,
    background = AppBg,
    onBackground = InkMain,
    surface = CardWhite,
    onSurface = InkMain,
    surfaceVariant = LineColor,
    onSurfaceVariant = InkSecond,
    outline = Color(0xFFD7DEE8),
    outlineVariant = LineColor
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BBDFB),
    onPrimary = Color(0xFF0B2A5B),
    primaryContainer = Color(0xFF2F6DE0),
    onPrimaryContainer = Color(0xFFDCE7FF),
    secondary = Color(0xFF7BE2A9),
    onSecondary = Color(0xFF103C28),
    secondaryContainer = Color(0xFF22795A),
    onSecondaryContainer = Color(0xFFD7F4E2),
    tertiary = Color(0xFFFCC075),
    onTertiary = Color(0xFF4B2B00),
    tertiaryContainer = Color(0xFFA86B00),
    onTertiaryContainer = Color(0xFFFFE5B8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF5C1A16),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101519),
    onBackground = Color(0xFFE6EAF2),
    surface = Color(0xFF151B20),
    onSurface = Color(0xFFE6EAF2),
    surfaceVariant = Color(0xFF232A31),
    onSurfaceVariant = Color(0xFFC3C9D6),
    outline = Color(0xFF3A434D),
    outlineVariant = Color(0xFF232A31)
)

/**
 * 原型中固定的浅色调色板（状态色、标签色）。
 * 深色模式下这些语义色仍然可辨认，因此保持不变，避免状态语义漂移。
 */
class AppPalette(
    val green: Color,
    val greenSoft: Color,
    val orange: Color,
    val orangeSoft: Color,
    val orangeText: Color,
    val red: Color,
    val redSoft: Color,
    val redText: Color,
    val purple: Color,
    val purpleSoft: Color,
    val brandSoft: Color,
    val brandDark: Color,
    val brand: Color,
    val ink2: Color,
    val ink3: Color,
    val line: Color,
    val track: Color
)

private val LightPalette = AppPalette(
    green = StatusGreen,
    greenSoft = StatusGreenSoft,
    orange = StatusOrange,
    orangeSoft = StatusOrangeSoft,
    orangeText = StatusOrangeText,
    red = StatusRed,
    redSoft = StatusRedSoft,
    redText = StatusRedText,
    purple = StatusPurple,
    purpleSoft = StatusPurpleSoft,
    brandSoft = BrandBlueSoft,
    brandDark = BrandBlueDark,
    brand = BrandBlue,
    ink2 = InkSecond,
    ink3 = InkThird,
    line = LineColor,
    track = Color(0xFFD7DEE8)
)

private val DarkPalette = AppPalette(
    green = Color(0xFF7BE2A9),
    greenSoft = Color(0xFF14372A),
    orange = Color(0xFFFCC075),
    orangeSoft = Color(0xFF3D2C10),
    orangeText = Color(0xFFFFD08A),
    red = Color(0xFFFFB4AB),
    redSoft = Color(0xFF3D1A17),
    redText = Color(0xFFFFC7C1),
    purple = Color(0xFFC9B0FF),
    purpleSoft = Color(0xFF2B2145),
    brandSoft = Color(0xFF1B2F52),
    brandDark = Color(0xFF9BBDFB),
    brand = Color(0xFF9BBDFB),
    ink2 = Color(0xFFC3C9D6),
    ink3 = Color(0xFF8A96A6),
    line = Color(0xFF232A31),
    track = Color(0xFF3A434D)
)

val LocalAppPalette = staticCompositionLocalOf { LightPalette }

/** 便捷访问语义调色板。 */
val appPalette: AppPalette
    @Composable get() = LocalAppPalette.current

/** 原型字体阶梯：字号/字重按 CSS 逐条对齐。 */
private val AppTypography = Typography(
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(AppRadiusTiny),
    medium = RoundedCornerShape(AppRadiusSmall),
    large = RoundedCornerShape(AppRadius),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun LearningReminderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAppPalette provides if (darkTheme) DarkPalette else LightPalette
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

/** 今日页摘要卡渐变刷（深色模式下略微压暗以保证对比度）。 */
@Composable
fun summaryBrush(dark: Boolean = isSystemInDarkTheme()): Brush {
    val colors = if (dark) {
        listOf(Color(0xFF3567C4), Color(0xFF4F8EF7))
    } else {
        SummaryGradient
    }
    return Brush.linearGradient(colors)
}

/** 报告页摘要卡渐变刷。 */
@Composable
fun reportBrush(dark: Boolean = isSystemInDarkTheme()): Brush {
    val colors = if (dark) {
        listOf(Color(0xFF2F7FA8), Color(0xFF3567C4))
    } else {
        ReportGradient
    }
    return Brush.linearGradient(colors)
}

/** 头像渐变刷，按名字稳定取色，保证同一孩子始终同色。 */
fun avatarBrush(name: String): Brush {
    val palettes = listOf(AvatarA, AvatarB, AvatarC, AvatarD)
    val index = (name.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }) % palettes.size
    return Brush.linearGradient(palettes[index])
}

/** 取名字首字作为头像文字。 */
fun avatarInitial(name: String): String = name.trim().takeIf { it.isNotEmpty() }?.substring(0, 1) ?: "?"
