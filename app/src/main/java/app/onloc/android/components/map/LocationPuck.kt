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

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import app.onloc.android.helpers.stringToColor
import app.onloc.android.models.Device
import app.onloc.android.models.Location
import dev.sargunv.maplibrecompose.compose.ClickResult
import dev.sargunv.maplibrecompose.compose.layer.CircleLayer
import dev.sargunv.maplibrecompose.compose.source.rememberGeoJsonSource
import dev.sargunv.maplibrecompose.core.source.GeoJsonData
import dev.sargunv.maplibrecompose.expressions.dsl.const
import io.github.dellisd.spatialk.geojson.Point
import io.github.dellisd.spatialk.geojson.Position

private const val ACCURACY_OPACITY = 0.5f

@Composable
fun LocationPuck(
    id: Int,
    longitude: Double,
    latitude: Double,
    accuracy: Double,
    color: Color,
    metersPerDp: Double,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val markerSource = rememberGeoJsonSource(
        data = GeoJsonData.Features(
            Point(
                Position(longitude, latitude),
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
    CircleLayer(
        id = "location-puck-$id",
        source = markerSource,
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
}

@Composable
fun LocationPuck(
    id: Int,
    location: Location,
    device: Device,
    metersPerDp: Double,
    showCone: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val accuracy = location.accuracy.toDouble()
    val name = device.name
    val color = device.color?.let { Color(it.toColorInt()) } ?: stringToColor(device.name)

    // Bearing cone first so it's layered under the rest
    if (location.bearing != null && location.bearingAccuracyDegrees != null) {
        BearingConeLayer(
            id = id,
            longitude = location.longitude,
            latitude = location.latitude,
            bearing = location.bearing,
            bearingAccuracyDegrees = location.bearingAccuracyDegrees,
            metersPerDp = metersPerDp,
            color = color,
            visible = showCone,
        )
    }

    LocationPuck(
        id = id,
        longitude = location.longitude,
        latitude = location.latitude,
        accuracy = accuracy,
        color = color,
        metersPerDp = metersPerDp,
        onClick = onClick,
        onLongClick = onLongClick,
    )

    DeviceNameLayer(
        id = id,
        longitude = location.longitude,
        latitude = location.latitude,
        name = name,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}
