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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Device(
    val id: Int,

    @SerialName("user_id")
    val userId: Int,

    val name: String,

    val color: String? = null,

    @SerialName("can_ring")
    val canRing: Boolean? = null,

    @SerialName("can_lock")
    val canLock: Boolean? = null,

    @SerialName("can_flash")
    val canFlash: Boolean? = null,

    val icon: String? = null,

    @SerialName("latest_location")
    val latestLocation: Location? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null,

    @SerialName("device_share")
    val deviceShare: DeviceShare? = null,

    @SerialName("is_connected")
    val isConnected: Boolean? = null,
)
