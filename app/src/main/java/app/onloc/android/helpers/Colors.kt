/*
 * Copyright (C) 2026 Thomas Lavoie
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package app.onloc.android.helpers

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import kotlin.math.abs

private const val HUE_MAX = 360
private const val SATURATION_BASE = 70
private const val SATURATION_RANGE = 30
private const val LIGHTNESS_BASE = 50
private const val LIGHTNESS_RANGE = 30
private const val HASH_SHIFT = 5

@Suppress("MagicNumber")
fun stringToColor(str: String): Color {
    var hash = 0
    for (ch in str) {
        hash = ch.code + ((hash shl HASH_SHIFT) - hash)
    }

    val hue = abs(hash) % HUE_MAX
    val saturation = (SATURATION_BASE + (abs(hash) % SATURATION_RANGE)) / 100f
    val lightness = (LIGHTNESS_BASE + (abs(hash) % LIGHTNESS_RANGE)) / 100f

    val a = saturation * minOf(lightness, 1f - lightness)
    fun f(n: Int): Float {
        val k = (n + hue / 30f) % 12f
        return (lightness - a * maxOf(-1f, minOf(k - 3f, 9f - k, 1f)))
            .coerceIn(0f, 1f)
    }

    return Color(
        red = f(0),
        green = f(8),
        blue = f(4),
    )
}
