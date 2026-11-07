package com.dhikr.app.data

import android.media.*
import kotlin.math.*
import kotlin.random.Random

/** Synthesized, non-musical clicks (filtered noise burst, no pitched notes). Zero audio assets. */
class ClickPlayer {
    private val rate = 22050
    private val tick = build(durMs = 12, lowpass = 0.55f, gain = 0.5f)
    private val done = build(durMs = 70, lowpass = 0.30f, gain = 0.6f, doubleClick = true)

    private fun build(durMs: Int, lowpass: Float, gain: Float, doubleClick: Boolean = false): AudioTrack {
        val n = rate * durMs / 1000
        val pcm = ShortArray(n)
        var prev = 0f
        for (i in 0 until n) {
            val env = exp(-i / (n * 0.18f))
            val noise = Random.nextFloat() * 2 - 1
            prev += lowpass * (noise - prev)                    // one-pole low-pass => "tock", not "beep"
            var s = prev * env
            if (doubleClick && i > n / 2) s = prev * exp(-(i - n / 2) / (n * 0.09f)) * 0.8f
            pcm[i] = (s * gain * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
        }
        return AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(rate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(pcm.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
            .also { it.write(pcm, 0, pcm.size) }
    }
    private fun play(t: AudioTrack) { try { t.stop(); t.setPlaybackHeadPosition(0); t.play() } catch (_: Exception) {} }
    fun tick() = play(tick)
    fun complete() = play(done)
    fun release() { tick.release(); done.release() }
}
