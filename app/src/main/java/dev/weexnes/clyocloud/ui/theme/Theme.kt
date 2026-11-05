package dev.weexnes.clyocloud.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Design tokens, identical to the web UI (assets/css/main.css) and DESIGN.md.
 * Black first, colour only where it means something, hairlines instead of shadows.
 */
@Immutable
data class ClyoColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val lineStrong: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val invert: Color,
    val onInvert: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val scrim: Color,
    val isDark: Boolean,
)

val DarkColors = ClyoColors(
    bg = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surface2 = Color(0xFF141414),
    line = Color(0xFF1F1F1F),
    lineStrong = Color(0xFF333333),
    text = Color(0xFFFAFAFA),
    text2 = Color(0xFFA3A3A3),
    text3 = Color(0xFF808080),
    invert = Color(0xFFFAFAFA),
    onInvert = Color(0xFF000000),
    success = Color(0xFF4ADE80),
    warning = Color(0xFFFBBF24),
    danger = Color(0xFFF87171),
    scrim = Color(0xB8000000),
    isDark = true,
)

val LightColors = ClyoColors(
    bg = Color(0xFFFFFFFF),
    surface = Color(0xFFFAFAFA),
    surface2 = Color(0xFFF0F0F0),
    line = Color(0xFFE8E8E8),
    lineStrong = Color(0xFFCCCCCC),
    text = Color(0xFF0A0A0A),
    text2 = Color(0xFF525252),
    text3 = Color(0xFF6B6B6B),
    invert = Color(0xFF0A0A0A),
    onInvert = Color(0xFFFFFFFF),
    success = Color(0xFF15803D),
    warning = Color(0xFFA16207),
    danger = Color(0xFFB91C1C),
    scrim = Color(0x99000000),
    isDark = false,
)

val LocalClyoColors = staticCompositionLocalOf { DarkColors }

/** Shorthand: `Clyo.colors.text` inside composables. */
object Clyo {
    val colors: ClyoColors
        @Composable get() = LocalClyoColors.current
}

/** Dark (default) or Light. Dark does not follow the operating system, like the web UI. */
object ThemePrefs {
    private const val PREFS = "clyo_cloud_prefs"
    private const val KEY = "app_theme_mode"

    /** Observed by the root composable so a change applies immediately. */
    val isDark = mutableStateOf(true)

    fun load(context: Context) {
        isDark.value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "dark") != "light"
    }

    fun set(context: Context, dark: Boolean) {
        isDark.value = dark
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, if (dark) "dark" else "light").apply()
    }
}

@Composable
fun ClyoCloudTheme(dark: Boolean = ThemePrefs.isDark.value, content: @Composable () -> Unit) {
    val c = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.invert, onPrimary = c.onInvert,
            secondary = c.text2, onSecondary = c.bg,
            background = c.bg, onBackground = c.text,
            surface = c.bg, onSurface = c.text,
            surfaceVariant = c.surface, onSurfaceVariant = c.text2,
            surfaceContainer = c.surface, surfaceContainerHigh = c.surface2, surfaceContainerHighest = c.surface2,
            surfaceContainerLow = c.surface, surfaceContainerLowest = c.bg,
            outline = c.lineStrong, outlineVariant = c.line,
            error = c.danger, onError = c.bg,
            inverseSurface = c.invert, inverseOnSurface = c.onInvert,
            surfaceTint = Color.Transparent,
        )
    } else {
        lightColorScheme(
            primary = c.invert, onPrimary = c.onInvert,
            secondary = c.text2, onSecondary = c.bg,
            background = c.bg, onBackground = c.text,
            surface = c.bg, onSurface = c.text,
            surfaceVariant = c.surface, onSurfaceVariant = c.text2,
            surfaceContainer = c.surface, surfaceContainerHigh = c.surface2, surfaceContainerHighest = c.surface2,
            surfaceContainerLow = c.surface, surfaceContainerLowest = c.bg,
            outline = c.lineStrong, outlineVariant = c.line,
            error = c.danger, onError = c.bg,
            inverseSurface = c.invert, inverseOnSurface = c.onInvert,
            surfaceTint = Color.Transparent,
        )
    }
    CompositionLocalProvider(LocalClyoColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = ClyoTypography, content = content)
    }
}
