/*
 * Copyright (C) 2025 Thomas Lavoie
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
