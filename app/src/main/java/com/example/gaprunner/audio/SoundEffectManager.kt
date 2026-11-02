package com.example.gaprunner.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Procedural synthesizer for arcade sound effects.
 * Requires zero external audio files, operates offline with near-zero latency.
 */
class SoundEffectManager {

    private val audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 22050

    var isSoundEnabled: Boolean = true

    fun playPassSound(isPerfect: Boolean) {
        if (!isSoundEnabled) return
        audioScope.launch {
            if (isPerfect) {
                // High double-tone arcade chime
                playToneSequence(
                    frequencies = doubleArrayOf(784.0, 1046.5), // G5, C6
                    durationMs = 80
                )
            } else {
                // Single bright pass blip
                playFrequencySweep(
                    startFreq = 523.25, // C5
                    endFreq = 659.25,   // E5
                    durationMs = 60
                )
            }
        }
    }

    fun playCollisionSound() {
        if (!isSoundEnabled) return
        audioScope.launch {
            // Low arcade impact crunch
            playFrequencySweep(
                startFreq = 160.0,
                endFreq = 45.0,
                durationMs = 180,
                isNoiseBlended = true
            )
        }
    }

    fun playButtonClick() {
        if (!isSoundEnabled) return
        audioScope.launch {
            playTone(frequency = 880.0, durationMs = 25)
        }
    }

    private fun playTone(frequency: Double, durationMs: Int) {
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val generatedSound = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Linear envelope fade out to avoid clicks
            val envelope = 1.0 - (i.toDouble() / numSamples)
            val sample = (sin(2.0 * Math.PI * frequency * t) * envelope * Short.MAX_VALUE * 0.4).toInt()
            generatedSound[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        outputSound(generatedSound)
    }

    private fun playFrequencySweep(
        startFreq: Double,
        endFreq: Double,
        durationMs: Int,
        isNoiseBlended: Boolean = false
    ) {
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val generatedSound = ShortArray(numSamples)

        var phase = 0.0
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val currentFreq = startFreq + (endFreq - startFreq) * progress
            phase += 2.0 * Math.PI * currentFreq / sampleRate

            val envelope = (1.0 - progress) * (1.0 - progress)
            var wave = sin(phase)
            if (isNoiseBlended) {
                val noise = (Math.random() * 2.0 - 1.0) * 0.35
                wave = (wave * 0.65) + noise
            }

            val sample = (wave * envelope * Short.MAX_VALUE * 0.45).toInt()
            generatedSound[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        outputSound(generatedSound)
    }

    private fun playToneSequence(frequencies: DoubleArray, durationMs: Int) {
        val samplesPerTone = (sampleRate * (durationMs / 1000.0)).toInt()
        val totalSamples = samplesPerTone * frequencies.size
        val generatedSound = ShortArray(totalSamples)

        for (toneIdx in frequencies.indices) {
            val freq = frequencies[toneIdx]
            val offset = toneIdx * samplesPerTone
            for (i in 0 until samplesPerTone) {
                val t = i.toDouble() / sampleRate
                val envelope = 1.0 - (i.toDouble() / samplesPerTone)
                val sample = (sin(2.0 * Math.PI * freq * t) * envelope * Short.MAX_VALUE * 0.4).toInt()
                generatedSound[offset + i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }

        outputSound(generatedSound)
    }

    private fun outputSound(buffer: ShortArray) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            // Release after playing
            audioScope.launch {
                val waitTime = (buffer.size * 1000L) / sampleRate + 50L
                kotlinx.coroutines.delay(waitTime)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            // AudioTrack failure fallback silently
        }
    }
}
