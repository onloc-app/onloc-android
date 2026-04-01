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

import android.R.attr.visible
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.graphics.toColorInt
import app.onloc.android.helpers.stringToColor
import app.onloc.android.models.Device
import app.onloc.android.models.Location
import dev.sargunv.maplibrecompose.compose.ClickResult
import dev.sargunv.maplibrecompose.compose.layer.CircleLayer
import dev.sargunv.maplibrecompose.compose.layer.FillLayer
import dev.sargunv.maplibrecompose.compose.layer.SymbolLayer
import dev.sargunv.maplibrecompose.compose.source.rememberGeoJsonSource
import dev.sargunv.maplibrecompose.core.source.GeoJsonData
import dev.sargunv.maplibrecompose.expressions.dsl.const
import dev.sargunv.maplibrecompose.expressions.dsl.format
import dev.sargunv.maplibrecompose.expressions.dsl.offset
import dev.sargunv.maplibrecompose.expressions.dsl.span
import io.github.dellisd.spatialk.geojson.Point
import io.github.dellisd.spatialk.geojson.Polygon
import io.github.dellisd.spatialk.geojson.Position
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val ACCURACY_OPACITY = 0.5f
private const val EARTH_RADIUS = 6371000.0
private const val OPACITY_DURATION = 300

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

    DeviceNameLayer(
        id = id,
        longitude = location.longitude,
        latitude = location.latitude,
        name = name,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

@Composable
private fun BearingConeLayer(
    id: Int,
    longitude: Double,
    latitude: Double,
    bearing: Float,
    bearingAccuracyDegrees: Float,
    metersPerDp: Double,
    color: Color,
    visible: Boolean,
) {
    val animatedOpacity by animateFloatAsState(
        targetValue = if (visible) 0.3f else 0f,
        animationSpec = tween(OPACITY_DURATION),
    )

    val coneSource = rememberGeoJsonSource(
        data = GeoJsonData.Features(
            generateBearingCone(
                longitude,
                latitude,
                bearing,
                bearingAccuracyDegrees,
                metersPerDp
            )
        )
    )

    FillLayer(
        id = "bearing-cone-$id",
        source = coneSource,
        color = const(color),
        opacity = const(animatedOpacity),
    )
}

private fun generateBearingCone(
    longitude: Double,
    latitude: Double,
    bearing: Float,
    bearingAccuracyDegrees: Float,
    metersPerDp: Double,
    radius: Float = 50f,
): Polygon {
    val coneRadiusMeters = (radius * metersPerDp).toFloat()

    fun offset(lat: Double, lon: Double, distance: Float, angle: Double): Position {
        val bearingRad = Math.toRadians(angle)
        val latRad = Math.toRadians(lat)
        val lonRad = Math.toRadians(lon)
        val newLat = asin(
            sin(latRad) * cos(distance / EARTH_RADIUS) +
                    cos(latRad) * sin(distance / EARTH_RADIUS) * cos(bearingRad)
        )
        val newLon = lonRad + atan2(
            sin(bearingRad) * sin(distance / EARTH_RADIUS) * cos(latRad),
            cos(distance / EARTH_RADIUS) - sin(latRad) * sin(newLat)
        )
        return Position(Math.toDegrees(newLon), Math.toDegrees(newLat))
    }

    val left = offset(latitude, longitude, coneRadiusMeters, (bearing - bearingAccuracyDegrees / 2).toDouble())
    val right = offset(latitude, longitude, coneRadiusMeters, (bearing + bearingAccuracyDegrees / 2).toDouble())
    val center = Position(longitude, latitude)

    return Polygon(listOf(listOf(center, left, right, center)))
}

@Composable
fun DeviceNameLayer(
    id: Int,
    longitude: Double,
    latitude: Double,
    name: String,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val markerSource = rememberGeoJsonSource(
        data = GeoJsonData.Features(Point(Position(longitude, latitude)))
    )

    val textColor = if (isSystemInDarkTheme()) Color.White else Color.Black

    SymbolLayer(
        id = "location-device-name-$id",
        source = markerSource,
        textField = format(
            span(const(name), textSize = const(1.2f.em))
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
