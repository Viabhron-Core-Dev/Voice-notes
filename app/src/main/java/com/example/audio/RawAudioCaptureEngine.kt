package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.example.data.logkeeper.LogKeeperManager
import com.example.data.logkeeper.LogTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

sealed interface AudioCaptureState {
    data object Idle : AudioCaptureState
    data class Recording(
        val durationMs: Long = 0L,
        val currentRms: Float = 0.0f,
        val totalChunksEmitted: Int = 0
    ) : AudioCaptureState
    data class Error(val message: String) : AudioCaptureState
}

class RawAudioCaptureEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        const val SAMPLE_RATE = 16000 // 16kHz required for Whisper
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val CHUNK_DURATION_SECONDS = 3 // 3-second streaming audio chunks
        const val SAMPLES_PER_CHUNK = SAMPLE_RATE * CHUNK_DURATION_SECONDS // 48,000 samples
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    private val _captureState = MutableStateFlow<AudioCaptureState>(AudioCaptureState.Idle)
    val captureState: StateFlow<AudioCaptureState> = _captureState.asStateFlow()

    // Real-time amplitude for visualizer bars (normalized 0.0f to 1.0f)
    private val _currentAmplitude = MutableStateFlow(0.0f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    // Shared flow emitting normalized Float32 audio chunks (ready for Whisper inference)
    private val _audioChunks = MutableSharedFlow<AudioChunk>(extraBufferCapacity = 16)
    val audioChunks: SharedFlow<AudioChunk> = _audioChunks.asSharedFlow()

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startCapture(): Boolean {
        if (!hasRecordPermission()) {
            LogKeeperManager.log(
                LogTag.VoiceEngine,
                "Cannot start capture: RECORD_AUDIO permission missing"
            )
            _captureState.value = AudioCaptureState.Error("Microphone permission required")
            return false
        }

        if (_captureState.value is AudioCaptureState.Recording) {
            LogKeeperManager.log(LogTag.VoiceEngine, "Audio capture is already active")
            return true
        }

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            val err = "Invalid AudioRecord buffer configuration"
            LogKeeperManager.log(LogTag.VoiceEngine, err)
            _captureState.value = AudioCaptureState.Error(err)
            return false
        }

        // Buffer twice the minimum for safe non-blocking reads
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                // Fallback to Standard MIC if VOICE_RECOGNITION fails
                audioRecord?.release()
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                val err = "AudioRecord failed to initialize hardware"
                LogKeeperManager.log(LogTag.VoiceEngine, err)
                _captureState.value = AudioCaptureState.Error(err)
                return false
            }

            audioRecord?.startRecording()
            LogKeeperManager.log(
                LogTag.VoiceEngine,
                "AudioRecord started: 16kHz Mono PCM (Buffer: $bufferSize bytes, Chunk: ${CHUNK_DURATION_SECONDS}s)"
            )

            val startTime = System.currentTimeMillis()
            var chunkCount = 0

            recordingJob = scope.launch(Dispatchers.IO) {
                val shortBuffer = ShortArray(1024)
                val chunkAccumulator = FloatArray(SAMPLES_PER_CHUNK)
                var accumulatedSamples = 0
                var chunkId = 1L

                _captureState.value = AudioCaptureState.Recording(
                    durationMs = 0L,
                    currentRms = 0f,
                    totalChunksEmitted = 0
                )

                while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val readCount = audioRecord?.read(shortBuffer, 0, shortBuffer.size) ?: 0
                    if (readCount > 0) {
                        // Calculate Root Mean Square (RMS) amplitude for real-time UI waveform
                        var sumOfSquares = 0.0
                        for (i in 0 until readCount) {
                            val sample = shortBuffer[i]
                            sumOfSquares += (sample * sample).toDouble()
                        }
                        val rms = sqrt(sumOfSquares / readCount).toFloat()

                        // Acoustic dB calculation: 20 * log10(rms / 32767)
                        // Range: -50 dB (quiet room) -> -10 dB (normal speaking voice) -> 0 dB (loud peak)
                        val db = if (rms > 1.0f) 20.0 * kotlin.math.log10(rms.toDouble() / 32767.0) else -60.0
                        val normalizedRms = ((db + 50.0) / 40.0).coerceIn(0.0, 1.0).toFloat()

                        _currentAmplitude.value = normalizedRms

                        // Convert short samples to normalized Float32 [-1.0f, 1.0f]
                        for (i in 0 until readCount) {
                            val floatSample = (shortBuffer[i] / 32768.0f).coerceIn(-1.0f, 1.0f)
                            chunkAccumulator[accumulatedSamples++] = floatSample

                            // If we reached chunk size (3.0 seconds / 48,000 samples), emit chunk
                            if (accumulatedSamples >= SAMPLES_PER_CHUNK) {
                                val chunkSamples = chunkAccumulator.copyOf()
                                val chunk = AudioChunk(
                                    id = chunkId++,
                                    samples = chunkSamples,
                                    sampleRate = SAMPLE_RATE,
                                    durationSeconds = CHUNK_DURATION_SECONDS.toFloat(),
                                    rmsAmplitude = normalizedRms
                                )
                                _audioChunks.tryEmit(chunk)
                                chunkCount++

                                LogKeeperManager.log(
                                    LogTag.VoiceEngine,
                                    "Captured audio chunk #$chunkCount (48,000 samples / 3.0s | RMS: ${(normalizedRms * 100).toInt()}%)"
                                )

                                accumulatedSamples = 0
                            }
                        }

                        val elapsed = System.currentTimeMillis() - startTime
                        _captureState.value = AudioCaptureState.Recording(
                            durationMs = elapsed,
                            currentRms = normalizedRms,
                            totalChunksEmitted = chunkCount
                        )
                    }
                }

                // If stopped with remaining audio (> 0.5s), emit final partial chunk
                if (accumulatedSamples >= (SAMPLE_RATE / 2)) {
                    val partialSamples = chunkAccumulator.copyOfRange(0, accumulatedSamples)
                    val durationSec = accumulatedSamples.toFloat() / SAMPLE_RATE
                    val finalChunk = AudioChunk(
                        id = chunkId++,
                        samples = partialSamples,
                        sampleRate = SAMPLE_RATE,
                        durationSeconds = durationSec,
                        rmsAmplitude = _currentAmplitude.value
                    )
                    _audioChunks.tryEmit(finalChunk)
                    chunkCount++
                    LogKeeperManager.log(
                        LogTag.VoiceEngine,
                        "Captured final partial chunk #$chunkCount ($accumulatedSamples samples / ${String.format("%.1f", durationSec)}s)"
                    )
                }
            }

            return true
        } catch (e: Exception) {
            val err = "AudioRecord exception: ${e.localizedMessage}"
            LogKeeperManager.log(LogTag.VoiceEngine, err)
            _captureState.value = AudioCaptureState.Error(err)
            release()
            return false
        }
    }

    fun stopCapture() {
        recordingJob?.cancel()
        recordingJob = null

        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
            audioRecord = null
            _currentAmplitude.value = 0.0f
            _captureState.value = AudioCaptureState.Idle
            LogKeeperManager.log(LogTag.VoiceEngine, "Audio capture stopped and resources released")
        } catch (e: Exception) {
            LogKeeperManager.log(LogTag.VoiceEngine, "Error stopping AudioRecord: ${e.message}")
        }
    }

    fun release() {
        stopCapture()
    }
}
