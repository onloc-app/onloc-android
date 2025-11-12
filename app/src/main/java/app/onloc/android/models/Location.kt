package app.onloc.android.models

import com.google.gson.annotations.SerializedName

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
