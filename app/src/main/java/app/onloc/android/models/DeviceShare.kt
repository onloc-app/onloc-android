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
data class DeviceShare(
    val id: Int,

    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("connection_id")
    val connectionId: Int,

    @SerializedName("device_id")
    val deviceId: Int,

    @SerializedName("can_ring")
    val canRing: Boolean,

    @SerializedName("can_lock")
    val canLock: Boolean,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("updated_at")
    val updatedAt: String,
)

@Keep
data class DeviceShareResponse(
    @SerializedName("device_shares")
    val deviceShares: List<DeviceShare>
)
