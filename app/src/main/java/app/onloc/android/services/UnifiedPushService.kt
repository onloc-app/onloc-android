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

package app.onloc.android.services

import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

private val ringCommandEvent = "ring-command"
private val lockCommandEvent = "lock-command"
private val flashCommandEvent = "flash-command"
private val registerDeviceEvent = "register-device"
private val locationsChangeEvent = "locations-change"

class UnifiedPushService : PushService() {
    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        TODO("Not yet implemented")
    }

    override fun onMessage(message: PushMessage, instance: String) {
        TODO("Not yet implemented")
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        TODO("Not yet implemented")
    }

    override fun onUnregistered(instance: String) {
        TODO("Not yet implemented")
    }

}