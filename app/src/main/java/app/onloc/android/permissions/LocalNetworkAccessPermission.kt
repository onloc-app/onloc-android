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

package app.onloc.android.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

private const val LOCAL_NETWORK_REQUEST_CODE = 2
private const val MINIMUM_API_VERSION = 37

class LocalNetworkAccessPermission : Permission {
    override fun isGranted(context: Context): Boolean {
        return Build.VERSION.SDK_INT < MINIMUM_API_VERSION || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_LOCAL_NETWORK
        ) == PERMISSION_GRANTED
    }

    override fun request(activity: Activity) {
        if (Build.VERSION.SDK_INT < MINIMUM_API_VERSION) return
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_LOCAL_NETWORK),
            LOCAL_NETWORK_REQUEST_CODE,
        )
    }
}
