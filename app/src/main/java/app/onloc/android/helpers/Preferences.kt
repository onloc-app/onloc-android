/*
 * Copyright (C) 2025 Thomas Lavoie
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

package app.onloc.android.helpers

import android.content.SharedPreferences
import app.onloc.android.ACCESS_TOKEN_KEY
import app.onloc.android.DEVICE_ID_KEY
import app.onloc.android.IP_KEY
import app.onloc.android.LOCATION_UPDATES_INTERVAL_KEY

fun getIP(deviceEncryptedPreferences: SharedPreferences): String? {
    return deviceEncryptedPreferences.getString(IP_KEY, null)
}

fun getAccessToken(deviceEncryptedPreferences: SharedPreferences): String? {
    return deviceEncryptedPreferences.getString(ACCESS_TOKEN_KEY, null)
}

fun getSelectedDeviceId(deviceEncryptedPreferences: SharedPreferences): Int {
    return deviceEncryptedPreferences.getInt(DEVICE_ID_KEY, -1)
}

fun getInterval(deviceEncryptedPreferences: SharedPreferences): Int {
    return deviceEncryptedPreferences.getInt(LOCATION_UPDATES_INTERVAL_KEY, -1)
}
