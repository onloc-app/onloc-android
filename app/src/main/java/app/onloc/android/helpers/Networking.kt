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
import android.net.LinkAddress
import android.net.Network
import java.net.Inet4Address
import java.net.InetAddress

data class LocalSubnet(val address: Inet4Address, val prefixLength: Int)

fun getLocalSubnet(context: Context): LocalSubnet? {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network: Network = cm.activeNetwork ?: return null
    val linkProperties = cm.getLinkProperties(network) ?: return null

    val linkAddress: LinkAddress = linkProperties.linkAddresses
        .firstOrNull { it.address is Inet4Address && !it.address.isLoopbackAddress }
        ?: return null

    return LocalSubnet(
        address = linkAddress.address as Inet4Address,
        prefixLength = linkAddress.prefixLength
    )
}

fun isInSameSubnet(candidate: InetAddress, local: LocalSubnet): Boolean {
    if (candidate !is Inet4Address) return false

    val localBytes = local.address.address
    val candidateBytes = candidate.address

    val prefixLength = local.prefixLength
    val fullBytes = prefixLength / 8
    val remainingBits = prefixLength % 8

    // Compare the full bytes covered by the prefix
    for (i in 0 until fullBytes) {
        if (localBytes[i] != candidateBytes[i]) return false
    }

    // Compare any remaining bits in the next byte
    if (remainingBits > 0 && fullBytes < 4) {
        val mask = (0xFF shl (8 - remainingBits)) and 0xFF
        if ((localBytes[fullBytes].toInt() and mask) != (candidateBytes[fullBytes].toInt() and mask)) {
            return false
        }
    }

    return true
}
