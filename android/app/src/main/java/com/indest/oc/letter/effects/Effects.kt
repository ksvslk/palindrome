package com.indest.oc.letter.effects

import android.content.Context
import android.content.res.Resources
import android.media.AudioManager
import android.media.SoundPool
import android.os.Vibrator
import androidx.core.content.res.ResourcesCompat
import com.indest.oc.R
import com.indest.oc.letter.effects.ColorSelector.ACTION_DOWN_NO_FRIEND
import com.indest.oc.letter.effects.ColorSelector.ACTION_UP_FRIEND
import com.indest.oc.letter.effects.ColorSelector.ACTION_UP_NO_FRIEND
import com.indest.oc.letter.effects.ColorSelector.ACTION_DOWN_FRIEND

data class Effects(val context: Context, val resources: Resources) {

    // Transition durations
    val onClickColorChangeDuration: Long = 200L
    val onReleaseColorChangeDuration: Long = 200L
    val animateToHomeDurationMultiplier = 50

    // Audio, sound and vibration
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val soundPool: SoundPool = SoundPool.Builder().build()
    val am: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Colors
    private val actionDownNoFriend = AnimationColor(
        viewStart = ResourcesCompat.getColor(resources, R.color.colorPrimary, null),
        viewEnd = ResourcesCompat.getColor(resources, R.color.colorSecondary, null),
        textStart = ResourcesCompat.getColor(resources, R.color.colorOnPrimary, null),
        textEnd = ResourcesCompat.getColor(resources, R.color.colorOnSecondary, null)
    )

    private val actionDownFriend = AnimationColor(
        viewStart = ResourcesCompat.getColor(resources, R.color.colorSecondary, null),
        viewEnd = ResourcesCompat.getColor(resources, R.color.colorSecondary, null),
        textStart = ResourcesCompat.getColor(resources, R.color.colorOnPrimary, null),
        textEnd = ResourcesCompat.getColor(resources, R.color.colorOnSecondary, null)
    )

    private val actionUpNoFriend = AnimationColor(
        viewStart = ResourcesCompat.getColor(resources, R.color.colorSecondary, null),
        viewEnd = ResourcesCompat.getColor(resources, R.color.colorPrimary, null),
        textStart = ResourcesCompat.getColor(resources, R.color.colorOnSecondary, null),
        textEnd = ResourcesCompat.getColor(resources, R.color.colorOnPrimary, null)
    )

    private val actionUpFriend = AnimationColor(
        viewStart = ResourcesCompat.getColor(resources, R.color.colorSecondary, null),
        viewEnd = ResourcesCompat.getColor(resources, R.color.colorSecondary, null),
        textStart = ResourcesCompat.getColor(resources, R.color.colorOnSecondary, null),
        textEnd = ResourcesCompat.getColor(resources, R.color.colorOnPrimary, null)
    )

    fun getColors(colorSelector: ColorSelector): AnimationColor {
        return when (colorSelector) {
            ACTION_DOWN_NO_FRIEND -> actionDownNoFriend
            ACTION_DOWN_FRIEND -> actionDownFriend
            ACTION_UP_NO_FRIEND -> actionUpNoFriend
            ACTION_UP_FRIEND -> actionUpFriend
        }
    }
}
