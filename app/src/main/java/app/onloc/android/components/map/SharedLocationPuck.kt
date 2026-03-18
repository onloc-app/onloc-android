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

package app.onloc.android.components.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.graphics.toColorInt
import app.onloc.android.AppPreferences
import app.onloc.android.R
import app.onloc.android.UserPreferences
import app.onloc.android.helpers.stringToColor
import app.onloc.android.models.Device
import app.onloc.android.models.Location
import app.onloc.android.models.User
import dev.sargunv.maplibrecompose.compose.ClickResult
import dev.sargunv.maplibrecompose.compose.layer.CircleLayer
import dev.sargunv.maplibrecompose.compose.layer.SymbolLayer
import dev.sargunv.maplibrecompose.compose.source.rememberGeoJsonSource
import dev.sargunv.maplibrecompose.core.source.GeoJsonData
import dev.sargunv.maplibrecompose.expressions.dsl.const
import dev.sargunv.maplibrecompose.expressions.dsl.format
import dev.sargunv.maplibrecompose.expressions.dsl.offset
import dev.sargunv.maplibrecompose.expressions.dsl.span
import io.github.dellisd.spatialk.geojson.Point
import io.github.dellisd.spatialk.geojson.Position
import dev.sargunv.maplibrecompose.expressions.dsl.image

private const val ACCURACY_OPACITY = 0.5f
private const val BACKGROUND_LAYER_SIZE = 1.25f

@Composable
fun SharedLocationPuck(
    id: Int,
    location: Location,
    device: Device,
    user: User,
    metersPerDp: Double,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val accuracy = location.accuracy.toDouble()
    val name = device.name
    val color = device.color?.let { Color(it.toColorInt()) } ?: stringToColor(device.name)

    val markerSource = rememberGeoJsonSource(
        data = GeoJsonData.Features(
            Point(
                Position(location.longitude, location.latitude),
            )
        )
    )

    CircleLayer(
        id = "location-accuracy-$id",
        source = markerSource,
        radius = const((accuracy / metersPerDp).dp),
        opacity = const(ACCURACY_OPACITY),
        color = const(color),
    )
    SymbolLayer(
        id = "location-puck-border-$id",
        source = markerSource,
        iconImage = image(painterResource(R.drawable.triangle), drawAsSdf = true),
        iconSize = const(BACKGROUND_LAYER_SIZE),
        iconColor = const(Color.White),
        iconAllowOverlap = const(true),
        onClick = { onClick(); ClickResult.Consume },
        onLongClick = { onLongClick(); ClickResult.Consume },
    )
    SymbolLayer(
        id = "location-puck-$id",
        source = markerSource,
        iconImage = image(painterResource(R.drawable.triangle), drawAsSdf = true),
        iconOffset = offset(0f.dp, 1f.dp),
        iconColor = const(color),
        iconAllowOverlap = const(true),
        onClick = { onClick(); ClickResult.Consume },
        onLongClick = { onLongClick(); ClickResult.Consume },
    )
    val textColor = if (isSystemInDarkTheme()) Color.White else Color.Black
    SymbolLayer(
        id = "location-device-name-$id",
        source = markerSource,
        textField = format(
            span(const("${user!!.username}'s $name"), textSize = const(1.2f.em))
        ),
        textFont = const(listOf("Noto Sans Regular")),
        textColor = const(textColor),
        textOffset = offset(0f.em, 2f.em),
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
