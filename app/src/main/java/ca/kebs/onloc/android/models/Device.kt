package ca.kebs.onloc.android.models

import com.google.gson.annotations.SerializedName

data class Device(
    val id: Int,

    @SerializedName("user_id")
    val userId: Int,

    val name: String,
    val icon: String?,

    @SerializedName("latest_location")
    val latestLocation: Location?,

    @SerializedName("created_at")
    val createdAt: String?,

    @SerializedName("updated_at")
    val updatedAt: String?
)

data class DeviceResponse(
    val devices: List<Device>
)