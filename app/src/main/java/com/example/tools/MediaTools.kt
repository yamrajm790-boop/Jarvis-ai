package com.example.tools

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent

class MediaTools(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun setVolume(levelPercentage: Int): ToolExecutionResult {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVol = ((levelPercentage.coerceIn(0, 100) / 100.0) * maxVol).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)
        return ToolExecutionResult(true, "Volume set to $levelPercentage percent, sir.")
    }

    fun increaseVolume(stepPercentage: Int = 15): ToolExecutionResult {
        val current = getVolumePercentage()
        return setVolume(current + stepPercentage)
    }

    fun decreaseVolume(stepPercentage: Int = 15): ToolExecutionResult {
        val current = getVolumePercentage()
        return setVolume(current - stepPercentage)
    }

    private fun getVolumePercentage(): Int {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (maxVol == 0) return 0
        return ((currentVol.toDouble() / maxVol.toDouble()) * 100).toInt()
    }

    fun playPauseMedia(): ToolExecutionResult {
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        return ToolExecutionResult(true, "Media toggled, sir.")
    }

    fun playMedia(): ToolExecutionResult {
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY)
        return ToolExecutionResult(true, "Playing media, sir.")
    }

    fun pauseMedia(): ToolExecutionResult {
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
        return ToolExecutionResult(true, "Media paused, sir.")
    }

    fun nextTrack(): ToolExecutionResult {
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
        return ToolExecutionResult(true, "Playing next track, sir.")
    }

    fun previousTrack(): ToolExecutionResult {
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        return ToolExecutionResult(true, "Playing previous track, sir.")
    }

    private fun dispatchMediaKey(keyCode: Int) {
        val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        audioManager.dispatchMediaKeyEvent(eventDown)
        audioManager.dispatchMediaKeyEvent(eventUp)
    }
}
