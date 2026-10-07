package com.deproof.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.deproof.data.observations.*
import com.deproof.ui.screen.DeviceStatsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = Color(0xFF0A0E27)) {
                    DeviceObservationDemo()
                }
            }
        }
    }
}

@Composable
fun DeviceObservationDemo() {
    val observation = AIZObservation(
        source = "operator-supplied-cli-stats",
        sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
        metrics = Metrics(
            storageObjectCount = 42,
            storageSizeBytes = 1048576,
            upstreamSpeedRaw = 1024
        )
    )

    DeviceStatsScreen(observation)
}
