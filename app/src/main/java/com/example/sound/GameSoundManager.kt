package com.example.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class GameSoundManager {
    private val soundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var musicJob: Job? = null
    private var lastFootstepMs = 0L

    var soundEnabled: Boolean = true
    var musicEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) stopBackgroundMusic()
        }

    fun playButtonTap() {
        if (!soundEnabled) return
        playToneSequence(listOf(660.0 to 35))
    }

    fun playFootstep() {
        if (!soundEnabled) return
        val now = System.currentTimeMillis()
        if (now - lastFootstepMs < 240L) return
        lastFootstepMs = now
        playToneSequence(listOf(180.0 to 22), volume = 0.15f)
    }

    fun playCollectSuccess(combo: Int) {
        if (!soundEnabled) return
        val baseFreq = (520.0 + (combo.coerceIn(1, 10) * 55.0))
        playToneSequence(
            listOf(
                baseFreq to 55,
                (baseFreq * 1.25) to 70,
                (baseFreq * 1.5) to 95
            ),
            volume = 0.35f
        )
    }

    fun playCowEscapeSound() {
        if (!soundEnabled) return
        playToneSequence(
            listOf(
                310.0 to 90,
                440.0 to 70,
                587.0 to 110,
                784.0 to 130
            ),
            volume = 0.4f
        )
    }

    fun playComboUp(combo: Int) {
        if (!soundEnabled) return
        val pitch = 600.0 + (combo * 70.0)
        playToneSequence(
            listOf(
                pitch to 45,
                (pitch * 1.33) to 85
            ),
            volume = 0.35f
        )
    }

    fun playAlertWarning() {
        if (!soundEnabled) return
        playToneSequence(
            listOf(
                440.0 to 60,
                370.0 to 60
            ),
            volume = 0.35f
        )
    }

    fun playCaughtPenalty() {
        if (!soundEnabled) return
        playToneSequence(
            listOf(
                392.0 to 80,
                330.0 to 90,
                261.6 to 140
            ),
            volume = 0.42f
        )
    }

    fun playCountdownTick(urgent: Boolean) {
        if (!soundEnabled) return
        val freq = if (urgent) 880.0 else 640.0
        playToneSequence(listOf(freq to 50), volume = 0.3f)
    }

    fun playRoundOverVictory() {
        if (!soundEnabled) return
        playToneSequence(
            listOf(
                523.25 to 90,
                659.25 to 90,
                783.99 to 100,
                1046.50 to 210
            ),
            volume = 0.42f
        )
    }

    fun startBackgroundMusic() {
        if (!musicEnabled || musicJob?.isActive == true) return
        val melody = listOf(
            261.63 to 140,
            329.63 to 140,
            392.00 to 140,
            329.63 to 140,
            293.66 to 140,
            349.23 to 140,
            440.00 to 140,
            392.00 to 140
        )
        musicJob = soundScope.launch {
            var idx = 0
            while (isActive && musicEnabled) {
                val (freq, dur) = melody[idx % melody.size]
                synthesizeAndPlay(listOf(freq to dur), volume = 0.08f)
                delay(220L)
                idx++
            }
        }
    }

    fun stopBackgroundMusic() {
        musicJob?.cancel()
        musicJob = null
    }

    private fun playToneSequence(notes: List<Pair<Double, Int>>, volume: Float = 0.3f) {
        soundScope.launch {
            synthesizeAndPlay(notes, volume)
        }
    }

    private fun synthesizeAndPlay(notes: List<Pair<Double, Int>>, volume: Float) {
        try {
            val sampleRate = 22050
            val totalDurationMs = notes.sumOf { it.second }.coerceAtLeast(20)
            val totalSamples = (sampleRate * totalDurationMs) / 1000
            if (totalSamples <= 0) return

            val pcm = ShortArray(totalSamples)
            var offset = 0
            for ((freq, durationMs) in notes) {
                val count = ((sampleRate * durationMs) / 1000).coerceAtMost(totalSamples - offset)
                for (i in 0 until count) {
                    val env = when {
                        i < count * 0.1 -> i / (count * 0.1)
                        i > count * 0.75 -> (count - i) / (count * 0.25)
                        else -> 1.0
                    }
                    val angle = 2.0 * PI * i * freq / sampleRate
                    val sample = (sin(angle) * Short.MAX_VALUE * volume * env).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    pcm[offset + i] = sample.toShort()
                }
                offset += count
            }

            val track = AudioTrack.Builder()
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
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcm, 0, pcm.size)
            track.play()
            Thread.sleep(totalDurationMs.toLong() + 30L)
            track.stop()
            track.release()
        } catch (_: Exception) {
            // Ignore audio hardware unavailability on headless/test devices
        }
    }
}
