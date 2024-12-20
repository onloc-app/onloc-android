package ca.kebs.onloc.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import ca.kebs.onloc.android.ui.theme.OnlocAndroidTheme

class LocationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OnlocAndroidTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Text("hi")
                }
            }
        }
    }
}