package com.example.audio.whisper

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sin

object WhisperAudioPreprocessor {
    const val SAMPLE_RATE = 16000
    const val N_FFT = 400
    const val HOP_LENGTH = 160
    const val N_MELS = 80

    private val hannWindow = FloatArray(N_FFT) { i ->
        (0.5f * (1.0f - cos(2.0f * PI.toFloat() * i / N_FFT))).toFloat()
    }

    private val melFilters: Array<FloatArray> = createMelFilters()

    private fun hzToMel(hz: Float): Float {
        return 2595.0f * log10(1.0f + hz / 700.0f)
    }

    private fun melToHz(mel: Float): Float {
        return 700.0f * (Math.pow(10.0, (mel / 2595.0f).toDouble()).toFloat() - 1.0f)
    }

    private fun createMelFilters(): Array<FloatArray> {
        val fMin = 0.0f
        val fMax = 8000.0f // Nyquist frequency for 16kHz audio
        val melMin = hzToMel(fMin)
        val melMax = hzToMel(fMax)

        val melPoints = FloatArray(N_MELS + 2) { i ->
            melMin + (melMax - melMin) * i / (N_MELS + 1)
        }

        val hzPoints = FloatArray(N_MELS + 2) { i ->
            melToHz(melPoints[i])
        }

        val binPoints = IntArray(N_MELS + 2) { i ->
            ((N_FFT + 1) * hzPoints[i] / SAMPLE_RATE).toInt().coerceIn(0, N_FFT / 2)
        }

        val numFreqBins = N_FFT / 2 + 1
        val filters = Array(N_MELS) { FloatArray(numFreqBins) }

        for (m in 1..N_MELS) {
            val fMinus = binPoints[m - 1]
            val fCenter = binPoints[m]
            val fPlus = binPoints[m + 1]

            for (k in fMinus until fCenter) {
                if (fCenter > fMinus) {
                    filters[m - 1][k] = (k - fMinus).toFloat() / (fCenter - fMinus)
                }
            }
            for (k in fCenter until fPlus) {
                if (fPlus > fCenter) {
                    filters[m - 1][k] = (fPlus - k).toFloat() / (fPlus - fCenter)
                }
            }
        }

        return filters
    }

    /**
     * Converts raw 16kHz Float32 PCM audio into an 80-channel Log-Mel Spectrogram.
     */
    fun computeLogMelSpectrogram(samples: FloatArray): MelSpectrogram {
        val numFrames = max(1, (samples.size - N_FFT) / HOP_LENGTH + 1)
        val numFreqBins = N_FFT / 2 + 1
        val melData = FloatArray(N_MELS * numFrames)

        val frame = FloatArray(N_FFT)
        val real = FloatArray(N_FFT)
        val imag = FloatArray(N_FFT)
        val powerSpectrum = FloatArray(numFreqBins)

        for (t in 0 until numFrames) {
            val sampleOffset = t * HOP_LENGTH

            // Apply Hann window
            for (i in 0 until N_FFT) {
                val idx = sampleOffset + i
                val s = if (idx < samples.size) samples[idx] else 0.0f
                frame[i] = s * hannWindow[i]
                real[i] = frame[i]
                imag[i] = 0.0f
            }

            // Real FFT computation (N=400)
            computeDft(real, imag, powerSpectrum)

            // Apply 80 Mel filterbanks
            for (m in 0 until N_MELS) {
                var melSum = 0.0f
                val filter = melFilters[m]
                for (k in 0 until numFreqBins) {
                    melSum += powerSpectrum[k] * filter[k]
                }
                // Log compression: log10(max(mel, 1e-5))
                val logMel = log10(max(melSum, 1e-5f))
                melData[m * numFrames + t] = logMel
            }
        }

        return MelSpectrogram(
            nMel = N_MELS,
            nFrames = numFrames,
            data = melData
        )
    }

    private fun computeDft(real: FloatArray, imag: FloatArray, powerSpectrum: FloatArray) {
        val n = N_FFT
        val halfN = n / 2 + 1

        for (k in 0 until halfN) {
            var sumReal = 0.0f
            var sumImag = 0.0f
            val angleStep = -2.0 * PI * k / n

            for (t in 0 until n) {
                val angle = (angleStep * t).toFloat()
                val cosVal = cos(angle)
                val sinVal = sin(angle)
                sumReal += real[t] * cosVal - imag[t] * sinVal
                sumImag += real[t] * sinVal + imag[t] * cosVal
            }

            powerSpectrum[k] = sumReal * sumReal + sumImag * sumImag
        }
    }
}
