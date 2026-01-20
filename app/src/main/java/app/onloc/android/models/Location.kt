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

package app.onloc.android.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class Location(
    val id: Int,

    @SerializedName("device_id")
    val deviceId: Int,

    val accuracy: Float,
    val altitude: Double,

    @SerializedName("altitude_accuracy")
    val altitudeAccuracy: Float,

    val latitude: Double,
    val longitude: Double,
    var battery: Int?,

    @SerializedName("created_at")
    val createdAt: String?,

    @SerializedName("updated_at")
    val updatedAt: String?
) {
    companion object {
        fun fromAndroidLocation(id: Int, deviceId: Int, location: android.location.Location): Location {
            return Location(
                id,
                deviceId,
                location.accuracy,
                location.altitude,
                location.verticalAccuracyMeters,
                location.latitude,
                location.longitude,
                null,
                null,
                null,
            )
        }
    }
}
