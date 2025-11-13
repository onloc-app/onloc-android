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

package app.onloc.android.components.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import dev.sargunv.maplibrecompose.compose.ClickResult
import dev.sargunv.maplibrecompose.compose.layer.CircleLayer
import dev.sargunv.maplibrecompose.compose.layer.SymbolLayer
import dev.sargunv.maplibrecompose.core.source.Source
import dev.sargunv.maplibrecompose.expressions.dsl.const
import dev.sargunv.maplibrecompose.expressions.dsl.format
import dev.sargunv.maplibrecompose.expressions.dsl.offset
import dev.sargunv.maplibrecompose.expressions.dsl.span

private const val ACCURACY_OPACITY = 0.5f

@Composable
fun LocationPuck(
    id: Int,
    source: Source,
    metersPerDp: Double,
    color: Color,
    name: String? = null,
    accuracy: Double? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    accuracy?.let {
        CircleLayer(
            id = "location-accuracy-$id",
            source = source,
            radius = const((accuracy / metersPerDp).dp),
            opacity = const(ACCURACY_OPACITY),
            color = const(color),
        )
    }
    CircleLayer(
        id = "location-puck-$id",
        source = source,
        radius = const(10.dp),
        strokeWidth = const(2.dp),
        strokeColor = const(Color.White),
        color = const(color),
        onClick = {
            onClick()
            ClickResult.Consume
        },
        onLongClick = {
            onLongClick()
            ClickResult.Consume
        },
    )
    name?.let {
        val textColor = if (isSystemInDarkTheme()) Color.White else Color.Black
        SymbolLayer(
            id = "location-device-name-$id",
            source = source,
            textField =
                format(
                    span(const(name), textSize = const(1.2f.em)),
                ),
            textFont = const(listOf("Noto Sans Regular")),
            textSize = const(1.2f.em),
            textColor = const(textColor),
            textOffset = offset(0f.em, 1.5f.em),
            onClick = {
                onClick()
                ClickResult.Consume
            },
            onLongClick = {
                onLongClick()
                ClickResult.Consume
            },
        )
    }
}
