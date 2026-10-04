package com.example

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object SoundManager {
    private const val SAMPLE_RATE = 44100

    private fun playSound(generateBuffer: () -> ShortArray) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val buffer = generateBuffer()
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                    .setAudioFormat(AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                    .setBufferSizeInBytes(buffer.size * 2)
                    .build()

                audioTrack.play()
                audioTrack.write(buffer, 0, buffer.size)
                // Let the track finish playing before releasing
                Thread.sleep((buffer.size.toDouble() / SAMPLE_RATE * 1000).toLong())
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playWhistle() {
        playSound {
            val duration = 0.6 // seconds
            val numSamples = (duration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val time = i.toDouble() / SAMPLE_RATE
                // 2500Hz with 15Hz vibrato to simulate a referee whistle
                val freq = 2500.0 + 100.0 * sin(2.0 * Math.PI * 15.0 * time)
                val envelope = if (time < 0.1) time / 0.1 else if (time > duration - 0.2) (duration - time) / 0.2 else 1.0
                buffer[i] = (sin(2.0 * Math.PI * freq * time) * Short.MAX_VALUE * 0.4 * envelope).toInt().toShort()
            }
            buffer
        }
    }

    fun playKick() {
        playSound {
            val duration = 0.15
            val numSamples = (duration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val time = i.toDouble() / SAMPLE_RATE
                // Pitch drop to simulate a thump/kick
                val freq = 150.0 * (1.0 - time / duration) 
                val envelope = (1.0 - time / duration)
                buffer[i] = (sin(2.0 * Math.PI * freq * time) * Short.MAX_VALUE * 0.8 * envelope).toInt().toShort()
            }
            buffer
        }
    }

    fun playSave() {
        playSound {
            val duration = 0.25
            val numSamples = (duration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val time = i.toDouble() / SAMPLE_RATE
                // Fast metallic glove smack with white noise blend
                val freq = 280.0 * (1.0 - time / duration)
                val noise = (Math.random() * 2.0 - 1.0) * 0.35
                val envelope = (1.0 - time / duration) * (1.0 - time / duration)
                val tone = sin(2.0 * Math.PI * freq * time) * 0.65
                buffer[i] = ((tone + noise) * Short.MAX_VALUE * 0.7 * envelope).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            buffer
        }
    }

    fun playCorner() {
        playWhistle()
    }

    fun playGoal() {
        playSound {
            val duration = 1.6
            val numSamples = (duration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(numSamples)
            // C5, E5, G5, C6 Arpeggio for celebration
            val notes = listOf(523.25, 659.25, 783.99, 1046.50, 1046.50) 
            for (i in 0 until numSamples) {
                val time = i.toDouble() / SAMPLE_RATE
                val noteDuration = duration / notes.size
                val noteIdx = (time / noteDuration).toInt().coerceIn(0, notes.size - 1)
                val freq = notes[noteIdx]
                val envelope = if (noteIdx == notes.size - 1) {
                    1.0 - ((time % noteDuration) / noteDuration) // final note fade out
                } else {
                    1.0
                }
                buffer[i] = (sin(2.0 * Math.PI * freq * time) * Short.MAX_VALUE * 0.4 * envelope).toInt().toShort()
            }
            buffer
        }
    }
}
