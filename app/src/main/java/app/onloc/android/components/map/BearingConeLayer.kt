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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import dev.sargunv.maplibrecompose.compose.layer.FillLayer
import dev.sargunv.maplibrecompose.compose.source.rememberGeoJsonSource
import dev.sargunv.maplibrecompose.core.source.GeoJsonData
import dev.sargunv.maplibrecompose.expressions.dsl.const
import io.github.dellisd.spatialk.geojson.Polygon
import io.github.dellisd.spatialk.geojson.Position
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val EARTH_RADIUS = 6371000.0
private const val OPACITY_DURATION = 300

@Composable
fun BearingConeLayer(
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
