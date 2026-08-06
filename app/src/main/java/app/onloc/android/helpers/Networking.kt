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

package app.onloc.android.helpers

import android.content.Context
import android.net.ConnectivityManager
import java.net.Inet4Address
import java.net.InetAddress

fun getLocalSubnet(context: Context): LocalSubnet? {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    return cm.activeNetwork
        ?.let(cm::getLinkProperties)
        ?.linkAddresses
        ?.firstOrNull {
            it.address is Inet4Address && !it.address.isLoopbackAddress
        }
        ?.let {
            LocalSubnet(
                address = it.address as Inet4Address,
                prefixLength = it.prefixLength,
            )
        }
}

@SuppressWarnings("MagicNumber")
fun isInSameSubnet(candidate: InetAddress, local: LocalSubnet): Boolean {
    if (candidate !is Inet4Address) return false

    val localBytes = local.address.address
    val candidateBytes = candidate.address

    val fullBytes = local.prefixLength / 8
    val remainingBits = local.prefixLength % 8

    var matches = true

    for (i in 0 until fullBytes) {
        if (localBytes[i] != candidateBytes[i]) {
            matches = false
            break
        }
    }

    if (matches && remainingBits > 0 && fullBytes < 4) {
        val mask = (0xFF shl (8 - remainingBits)) and 0xFF
        matches = (localBytes[fullBytes].toInt() and mask) == (candidateBytes[fullBytes].toInt() and mask)
    }

    return matches
}

data class LocalSubnet(val address: Inet4Address, val prefixLength: Int)
