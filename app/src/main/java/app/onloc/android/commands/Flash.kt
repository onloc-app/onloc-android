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

package app.onloc.android.commands

import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val FLASH_REPEAT_COUNT = 10
private const val FLASH_DELAY = 500L

class Flash(val context: Context) : Command {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null

    override fun execute() {
        val cameraManager = context.getSystemService(CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0]
        job?.cancel()
        job = coroutineScope.launch {
            try {
                repeat(FLASH_REPEAT_COUNT) {
                    cameraManager.setTorchMode(cameraId, true)
                    delay(FLASH_DELAY.milliseconds)
                    cameraManager.setTorchMode(cameraId, false)
                    delay(FLASH_DELAY.milliseconds)
                }
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }
    }
}
