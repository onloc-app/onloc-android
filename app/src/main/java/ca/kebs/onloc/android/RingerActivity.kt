package ca.kebs.onloc.android

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
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import ca.kebs.onloc.android.singletons.RingerState
import ca.kebs.onloc.android.ui.theme.OnlocAndroidTheme
import kotlinx.coroutines.delay

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
                startRinging(60)
            }

            OnlocAndroidTheme {
                Scaffold { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Button(onClick = {
                            finish()
                        }) {
                            Text("Stop")
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

    private suspend fun startRinging(durationSec: Int) {
        raiseVolume()
        ringtone?.play()

        delay(durationSec * 1000L)

        finish()
    }

    private fun raiseVolume() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        oldRingerMode = audioManager.ringerMode
        oldAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
    }

    private fun resetVolume() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        oldRingerMode?.let { audioManager.ringerMode = it }
        oldAlarmVolume?.let { audioManager.ringerMode = it }
    }
}