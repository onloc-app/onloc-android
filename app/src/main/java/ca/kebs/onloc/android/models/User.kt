package ca.kebs.onloc.android.models

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    val username: String,

    @SerializedName("created_at")
    val createdAt: String?,

    @SerializedName("updated_at")
    val updatedAt: String?
)
