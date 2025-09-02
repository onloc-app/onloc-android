package ca.kebs.onloc.android.helpers

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import kotlin.math.abs

private const val HUE_MAX = 360
private const val SATURATION_BASE = 70
private const val SATURATION_RANGE = 30
private const val LIGHTNESS_BASE = 50
private const val LIGHTNESS_RANGE = 30
private const val HASH_SHIFT = 5

fun stringToColor(str: String): Color {
    var hash = 0
    for (ch in str) {
        hash = ch.code + ((hash shl HASH_SHIFT) - hash)
    }

    val hue = abs(hash) % HUE_MAX
    val saturation = SATURATION_BASE + (abs(hash) % SATURATION_RANGE)
    val lightness = LIGHTNESS_BASE + (abs(hash) % LIGHTNESS_RANGE)

    val hsl = floatArrayOf(hue.toFloat(), saturation.toFloat(), lightness.toFloat())
    val colorInt = ColorUtils.HSLToColor(hsl)

    return Color(colorInt)
}
