package com.suvrnika.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Brand palette, identical to the website's tailwind.config.ts. */
object Brand {
    val Ivory = Color(0xFFFAF7F2)
    val Sand = Color(0xFFF4EEE4)
    val Line = Color(0xFFE4DACA)
    val Ink = Color(0xFF1C1A17)
    val Muted = Color(0xFF6B6358)
    val Body = Color(0xFF5A5249)
    val Gold = Color(0xFF8C5E14)
    val GoldDark = Color(0xFF6E4A0F)
    val GoldLight = Color(0xFFC9A45C)
    val GoldPale = Color(0xFFF1E4C8)
    val GoldSoft = Color(0xFFE9D7AE)
    val Emerald = Color(0xFF17302A)
    val EmeraldLine = Color(0xFF2C4A42)
    val EmeraldText = Color(0xFFA9BDB5)
    val Ok = Color(0xFF2F6B4F)
    val OkBg = Color(0xFFE4F0E8)
    val Bad = Color(0xFF9A3B1E)
    val BadBg = Color(0xFFF6E0D6)
    val Cream = Color(0xFFF3E7CC)
}

val Display = FontFamily.Serif

private val colors = lightColorScheme(
    primary = Brand.Ink, onPrimary = Brand.Ivory,
    secondary = Brand.Gold, onSecondary = Color.White,
    tertiary = Brand.Emerald, onTertiary = Brand.Cream,
    background = Brand.Ivory, onBackground = Brand.Ink,
    surface = Color.White, onSurface = Brand.Ink,
    surfaceVariant = Brand.Sand, onSurfaceVariant = Brand.Body,
    surfaceContainer = Brand.Ivory, surfaceContainerLow = Color.White, surfaceContainerHigh = Brand.Sand,
    outline = Brand.Line, outlineVariant = Brand.Line,
    error = Brand.Bad, errorContainer = Brand.BadBg,
)

private val type = Typography(
    displaySmall = TextStyle(fontFamily = Display, fontSize = 34.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = Display, fontSize = 28.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Display, fontSize = 24.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Display, fontSize = 22.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.6.sp),
)

@Composable
fun SuvrnikaTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = colors, typography = type, content = content)
