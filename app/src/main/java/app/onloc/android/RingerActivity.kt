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

package app.onloc.android

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.onloc.android.services.RingerService
import app.onloc.android.singletons.RingerState
import app.onloc.android.ui.theme.OnlocAndroidTheme
import kotlinx.coroutines.delay

const val DURATION = 60000L

class RingerActivity : ComponentActivity() {
    private var ringtone: Ringtone? = null
    private var oldRingerMode: Int? = null
    private var oldAlarmVolume: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        setContent {
            val context = LocalContext.current

            val ringtoneUri = Settings.System.DEFAULT_ALARM_ALERT_URI
            ringtone = RingtoneManager.getRingtone(context, ringtoneUri)

            LaunchedEffect(Unit) {
                startRinging(DURATION)
            }

            OnlocAndroidTheme {
                Scaffold { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Button(
                            shape = CircleShape,
                            modifier = Modifier
                                .padding(16.dp)
                                .size(256.dp),
                            onClick = {
                                stopRingerService(context)
                                finish()
                            }) {
                            Text(text = "Stop", fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        ringtone?.stop()
        resetVolume()
        RingerState.isRinging = false
    }

    private suspend fun startRinging(duration: Long) {
        raiseVolume()
        ringtone?.play()

        delay(duration)

        finish()
    }

    private fun raiseVolume() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        oldRingerMode = audioManager.ringerMode
        oldAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)

        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolume, 0)
    }

    private fun resetVolume() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        oldRingerMode?.let { audioManager.ringerMode = it }
        oldAlarmVolume?.let { audioManager.setStreamVolume(AudioManager.STREAM_RING, it, 0) }
    }

    fun stopRingerService(context: Context) {
        val ringerServiceIntent = Intent(context, RingerService::class.java)
        context.stopService(ringerServiceIntent)
    }
}
