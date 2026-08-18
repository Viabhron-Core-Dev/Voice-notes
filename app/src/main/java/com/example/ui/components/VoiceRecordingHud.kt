package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.whisper.InferenceBenchmark

@Composable
fun VoiceRecordingHud(
    isRecording: Boolean,
    durationMs: Long,
    amplitude: Float,
    chunkCount: Int,
    latestBenchmark: InferenceBenchmark? = null,
    onStopAndSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val minutes = (durationMs / 1000) / 60
    val seconds = (durationMs / 1000) % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A) // Dark slate floating HUD
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("voice_recording_hud")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Pulsing Mic Icon + Timer & Benchmark Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFEF4444).copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Recording",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    )

                    val statusSubtitle = if (latestBenchmark != null) {
                        "⚡ ${String.format("%.1f", latestBenchmark.speedupMultiplier)}x RTF (${latestBenchmark.totalDurationMs}ms)"
                    } else if (chunkCount > 0) {
                        "$chunkCount chunks processed"
                    } else {
                        "Listening (16kHz PCM)..."
                    }

                    Text(
                        text = statusSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (latestBenchmark != null) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = if (latestBenchmark != null) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }
            }

            // Real-time Visualizer Waves
            AudioWaveformBars(
                amplitude = amplitude,
                modifier = Modifier
                    .width(80.dp)
                    .height(28.dp)
            )

            // Stop / Done Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_cancel_recording")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onStopAndSave,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF00897B), CircleShape)
                        .testTag("btn_done_recording")
                ) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Finish Dictation",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AudioWaveformBars(
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val barCount = 5
    val clampedAmp = amplitude.coerceIn(0.05f, 1.0f)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            // Stagger wave factor based on position
            val multiplier = when (i) {
                0, 4 -> 0.4f
                1, 3 -> 0.75f
                else -> 1.0f
            }
            val targetHeight = (clampedAmp * multiplier * 24.dp.value).coerceIn(4f, 24f)
            val animatedHeight by animateFloatAsState(
                targetValue = targetHeight,
                animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
                label = "bar_$i"
            )

            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(animatedHeight.dp)
                    .background(Color(0xFF38BDF8), RoundedCornerShape(2.dp))
            )
        }
    }
}
