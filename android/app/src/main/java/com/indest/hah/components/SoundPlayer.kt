package com.indest.hah.components

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import com.indest.hah.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val soundPool: SoundPool,
    private val audioManager: AudioManager,
    val sounds:Sounds) {

    fun play(id:Int) {
        context.getSharedPreferences(context.getString(R.string.app_preferences), Context.MODE_PRIVATE)?.let {
            if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL ||
                !it.getBoolean(context.getString(R.string.sound_on_pref), true)
            ) return
            soundPool.play(id, 1f, 1f, 1, 0, 1.0f)
        }
    }

    fun playNote(index:Int)
    {
        context.getSharedPreferences(context.getString(R.string.app_preferences), Context.MODE_PRIVATE)?.let {
            if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL ||
                !it.getBoolean(context.getString(R.string.sound_on_pref), true)
            ) return
            soundPool.play(sounds.noteSounds[index], 1f, 1f, 1, 0, 1.0f)
        }
    }

    fun isRingerModeNotSilent():Boolean
    {
        return audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT
    }

    fun isRingerModeNotNormal():Boolean
    {
        return audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL
    }

}