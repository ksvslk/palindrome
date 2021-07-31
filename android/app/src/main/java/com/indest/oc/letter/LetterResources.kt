package com.indest.oc.letter

import android.content.Context
import android.content.res.Resources
import android.media.AudioManager
import android.media.SoundPool
import android.os.Vibrator
import androidx.core.content.res.ResourcesCompat
import com.indest.oc.R

data class LetterResources(val context: Context, val resources: Resources)
{
    // Transition durations
    val onClickColorChangeDuration: Long = 200L
    val onReleaseColorChangeDuration: Long = 200L
    val animateToHomeDurationMultiplier = 50

    // Color transitions
    val viewStartColor: Int =
        ResourcesCompat.getColor(resources, R.color.colorPrimary, null)
    val viewEndColor: Int =
        ResourcesCompat.getColor(resources, R.color.colorSecondary, null)
    val textStartColor: Int =
        ResourcesCompat.getColor(resources, R.color.colorOnPrimary, null)
    val textEndColor: Int =
        ResourcesCompat.getColor(resources, R.color.colorOnSecondary, null)
    val textEndColorInSlot: Int =
        ResourcesCompat.getColor(resources, R.color.colorOnPrimary, null)

    // Audio, sound and vibration
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val soundPool: SoundPool = SoundPool.Builder().build()
    val am: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
}
