package ca.kebs.onloc.android.helpers

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import kotlin.math.abs

fun stringToColor(str: String): Color {
    var hash = 0
    for (ch in str) {
        hash = ch.code + ((hash shl 5) - hash)
    }

    val hue = abs(hash) % 360
    val saturation = 70 + (abs(hash) % 30)
    val lightness = 50 + (abs(hash) % 30)

    val hsl = floatArrayOf(hue.toFloat(), saturation.toFloat(), lightness.toFloat())
    val colorInt = ColorUtils.HSLToColor(hsl)

    return Color(colorInt)
}
