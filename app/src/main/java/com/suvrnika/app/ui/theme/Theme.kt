package com.suvrnika.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Men / Women storefront views, the same as the website's header switch (src/lib/gender.ts).
 * null = the common view: every item, the normal look. "MEN" or "WOMEN" = that gender's items plus
 * unisex ones, with that view's theme on the whole app. The choice is remembered across launches,
 * like the website's sv_gender cookie.
 */
object StoreView {
    val GENDERS = listOf("MEN", "WOMEN")
    val LABEL = mapOf("MEN" to "Men", "WOMEN" to "Women", "UNISEX" to "Both")

    /** Snapshot state: every screen that reads a [Brand] colour re-themes when this changes. */
    var gender by mutableStateOf<String?>(null)
        private set
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("suvrnika_view", Context.MODE_PRIVATE)
        gender = prefs?.getString(KEY, null)?.takeIf { it in GENDERS }
    }

    fun set(next: String?) {
        gender = next?.takeIf { it in GENDERS }
        prefs?.edit()?.apply { if (gender == null) remove(KEY) else putString(KEY, gender) }?.apply()
    }

    private const val KEY = "gender"
}

/** One storefront palette. Field names follow the website's Tailwind colours (ivory, sand, gold-*, emerald-*). */
class Palette(
    val ivory: Color, val sand: Color, val line: Color, val ink: Color, val muted: Color, val body: Color,
    val gold: Color, val goldDark: Color, val goldLight: Color, val goldPale: Color, val goldSoft: Color,
    val emerald: Color, val emeraldLine: Color, val emeraldText: Color, val cream: Color,
    /** Lighter and darker ends of the emerald and gold gradients (hero slides, plan promo, pop-up). */
    val emeraldHi: Color, val emeraldLo: Color, val goldHi: Color,
    /** Text on a gold-pale panel. */
    val onGoldPale: Color,
)

/** The common look, identical to the website's tailwind.config.ts. */
private val Common = Palette(
    ivory = Color(0xFFFAF7F2), sand = Color(0xFFF4EEE4), line = Color(0xFFE4DACA), ink = Color(0xFF1C1A17), muted = Color(0xFF6B6358), body = Color(0xFF5A5249),
    gold = Color(0xFF8C5E14), goldDark = Color(0xFF6E4A0F), goldLight = Color(0xFFC9A45C), goldPale = Color(0xFFF1E4C8), goldSoft = Color(0xFFE9D7AE),
    emerald = Color(0xFF17302A), emeraldLine = Color(0xFF2C4A42), emeraldText = Color(0xFFA9BDB5), cream = Color(0xFFF3E7CC),
    emeraldHi = Color(0xFF2C5A4D), emeraldLo = Color(0xFF0F241F), goldHi = Color(0xFFB88A3E), onGoldPale = Color(0xFF4A3309),
)

/** Men: cool steel and navy (globals.css `.storefront[data-gender="MEN"]`). */
private val Men = Palette(
    ivory = Color(0xFFEEF1F4), sand = Color(0xFFE2E7ED), line = Color(0xFFC8D1DB), ink = Color(0xFF0E1620), muted = Color(0xFF4F5D6B), body = Color(0xFF3D4957),
    gold = Color(0xFF1E4B73), goldDark = Color(0xFF143654), goldLight = Color(0xFF8DB0CF), goldPale = Color(0xFFDCE7F1), goldSoft = Color(0xFFC2D5E6),
    emerald = Color(0xFF0D1520), emeraldLine = Color(0xFF26323F), emeraldText = Color(0xFF9DAABA), cream = Color(0xFFE6EDF4),
    emeraldHi = Color(0xFF1D3550), emeraldLo = Color(0xFF0B1118), goldHi = Color(0xFF3A6A94), onGoldPale = Color(0xFF14304D),
)

/** Women: rose and wine (globals.css `.storefront[data-gender="WOMEN"]`). */
private val Women = Palette(
    ivory = Color(0xFFFDF6F4), sand = Color(0xFFF9EAE7), line = Color(0xFFEFD5D1), ink = Color(0xFF2B1A1F), muted = Color(0xFF7D6066), body = Color(0xFF6B5157),
    gold = Color(0xFFA2485A), goldDark = Color(0xFF7F3546), goldLight = Color(0xFFDE9EAA), goldPale = Color(0xFFF8E1E4), goldSoft = Color(0xFFF1C8CF),
    emerald = Color(0xFF4A1D2C), emeraldLine = Color(0xFF663245), emeraldText = Color(0xFFE2B9C2), cream = Color(0xFFFBE7EB),
    emeraldHi = Color(0xFF6B2C40), emeraldLo = Color(0xFF2A1119), goldHi = Color(0xFFC0687A), onGoldPale = Color(0xFF5A2333),
)

private fun paletteFor(gender: String?) = when (gender) { "MEN" -> Men; "WOMEN" -> Women; else -> Common }

/** Brand colours for the current storefront view. Status colours (ok/bad) are the same in every view. */
object Brand {
    private val p get() = paletteFor(StoreView.gender)
    val Ivory get() = p.ivory
    val Sand get() = p.sand
    val Line get() = p.line
    val Ink get() = p.ink
    val Muted get() = p.muted
    val Body get() = p.body
    val Gold get() = p.gold
    val GoldDark get() = p.goldDark
    val GoldLight get() = p.goldLight
    val GoldPale get() = p.goldPale
    val GoldSoft get() = p.goldSoft
    val Emerald get() = p.emerald
    val EmeraldLine get() = p.emeraldLine
    val EmeraldText get() = p.emeraldText
    val Cream get() = p.cream
    val EmeraldHi get() = p.emeraldHi
    val EmeraldLo get() = p.emeraldLo
    val GoldHi get() = p.goldHi
    val OnGoldPale get() = p.onGoldPale
    val Ok = Color(0xFF2F6B4F)
    val OkBg = Color(0xFFE4F0E8)
    val Bad = Color(0xFF9A3B1E)
    val BadBg = Color(0xFFF6E0D6)

    /** Men: squarer, modern buttons. Women: soft pills. Common: the website's crisp edge. */
    val ButtonShape: Shape get() = when (StoreView.gender) { "MEN" -> RoundedCornerShape(4.dp); "WOMEN" -> RoundedCornerShape(50); else -> RoundedCornerShape(2.dp) }
    /** Product-card corner radius (`.product-card-visual` in each view). */
    val CardRadius: Dp get() = when (StoreView.gender) { "MEN" -> 6.dp; "WOMEN" -> 28.dp; else -> 14.dp }
}

/** Heading face: the serif in the common and Women views; Men gets clean, modern sans-serif headings. */
val Display: FontFamily get() = if (StoreView.gender == "MEN") FontFamily.SansSerif else FontFamily.Serif

private fun typography(gender: String?): Typography {
    val face = if (gender == "MEN") FontFamily.SansSerif else FontFamily.Serif
    val weight = if (gender == "MEN") FontWeight.SemiBold else null
    val spacing = if (gender == "MEN") (-0.3).sp else 0.sp
    // Women: italic serif headlines, like `h1.font-display, h2.font-display` on the website.
    val italic = if (gender == "WOMEN") FontStyle.Italic else null
    fun heading(size: Int, line: Int, slanted: Boolean) =
        TextStyle(fontFamily = face, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing, fontStyle = if (slanted) italic else null)
    return Typography(
        displaySmall = heading(34, 38, slanted = true),
        headlineMedium = heading(28, 32, slanted = true),
        headlineSmall = heading(24, 28, slanted = false),
        titleLarge = heading(22, 26, slanted = false),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.6.sp),
    )
}

@Composable
fun SuvrnikaTheme(content: @Composable () -> Unit) {
    val gender = StoreView.gender
    val p = paletteFor(gender)
    val colors = remember(gender) {
        lightColorScheme(
            primary = p.ink, onPrimary = p.ivory,
            secondary = p.gold, onSecondary = Color.White,
            tertiary = p.emerald, onTertiary = p.cream,
            background = p.ivory, onBackground = p.ink,
            surface = Color.White, onSurface = p.ink,
            surfaceVariant = p.sand, onSurfaceVariant = p.body,
            surfaceContainer = p.ivory, surfaceContainerLow = Color.White, surfaceContainerHigh = p.sand,
            surfaceContainerLowest = Color.White, surfaceContainerHighest = p.sand,
            outline = p.line, outlineVariant = p.line,
            error = Brand.Bad, errorContainer = Brand.BadBg,
        )
    }
    val type = remember(gender) { typography(gender) }
    MaterialTheme(colorScheme = colors, typography = type, content = content)
}
