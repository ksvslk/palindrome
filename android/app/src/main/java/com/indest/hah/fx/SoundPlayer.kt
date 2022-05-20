package com.indest.hah.fx

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.indest.hah.R
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class SoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    val soundPool: SoundPool,
    private val audioManager: AudioManager,
    val sounds: Sounds
) {

    fun play(id: Int, volume:Float = 1f) {
        context.getSharedPreferences(
            context.getString(R.string.app_preferences),
            Context.MODE_PRIVATE
        )?.let {
            if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL ||
                !it.getBoolean(context.getString(R.string.sound_on_pref), true)
            ) return
            soundPool.play(id, volume, volume, 1, 0, 1.0f)
        }
    }

    fun playNote(index: Int, volume: Float = 0.1f) {
        context.getSharedPreferences(
            context.getString(R.string.app_preferences),
            Context.MODE_PRIVATE
        )?.let {
            if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL ||
                !it.getBoolean(context.getString(R.string.sound_on_pref), true)
            ) return
            soundPool.play(sounds.noteSounds[index], volume, volume, 1, 0, 1.0f)
        }
    }

    fun isRingerModeNotSilent(): Boolean {
        return audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT
    }

    fun isRingerModeNotNormal(): Boolean {
        return audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL
    }

    fun vibrateDoubleClick() {
        context.getSharedPreferences(
            context.getString(R.string.app_preferences),
            Context.MODE_PRIVATE
        )?.let {
            if (isRingerModeNotSilent() && it.getBoolean(
                    context.getString(R.string.vibration_on_pref),
                    true
                ) && it.getBoolean(context.getString(R.string.sad_mode_pref), false)
            ) {
                val doubleClickPattern = longArrayOf(0, 50, 50, 50)
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                when {
                    Build.VERSION.SDK_INT >= 26 -> {
                        vibrator.vibrate(
                            VibrationEffect.createWaveform(doubleClickPattern, -1)
                        )
                    }
                    else -> {
                        vibrator.vibrate(doubleClickPattern, -1)
                    }
                }
            }
        }
    }

}