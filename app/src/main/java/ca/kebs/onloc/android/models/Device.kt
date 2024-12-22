package ca.kebs.onloc.android.models

import com.google.gson.annotations.SerializedName

data class Device(
    val id: Int,

    @SerializedName("user_id")
    val userId: Int,

    val name: String,
    val icon: String?,

    @SerializedName("created_at")
    val createdAt: String?,

    @SerializedName("updated_at")
    val updatedAt: String?
)
