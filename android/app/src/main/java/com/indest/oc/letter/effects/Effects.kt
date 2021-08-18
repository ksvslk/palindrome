package com.indest.oc.letter.effects

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.res.ResourcesCompat
import com.indest.oc.R
import com.indest.oc.letter.effects.ColorSelector.ACTION_DOWN_NO_FRIEND
import com.indest.oc.letter.effects.ColorSelector.ACTION_UP_FRIEND
import com.indest.oc.letter.effects.ColorSelector.ACTION_UP_NO_FRIEND
import com.indest.oc.letter.effects.ColorSelector.ACTION_DOWN_FRIEND
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Effects @Inject constructor(@ApplicationContext val context: Context) {
    //
    val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Transition durations
    val onClickColorChangeDuration: Long = 200L
    val onReleaseColorChangeDuration: Long = 200L
    val animateToHomeDurationMultiplier = 50

    // Audio, sound and vibration
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val soundPool: SoundPool = SoundPool.Builder().build()
    val am: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Sound effect IDs
    private val switchLettersSoundId = soundPool.load(context, R.raw.swish, 1)
    private val placeLetterSoundId = soundPool.load(context, R.raw.positive_beep, 1)
    private val actionDownSoundId = soundPool.load(context, R.raw.action_down, 1)
    private val actionUpSoundId = soundPool.load(context, R.raw.action_up, 1)

    // Colors
    private val actionDownNoFriend = AnimationColor(
        viewStart = ResourcesCompat.getColor(context.resources, R.color.colorPrimary, null),
        viewEnd = ResourcesCompat.getColor(context.resources, R.color.colorSecondary, null),
        textStart = ResourcesCompat.getColor(context.resources, R.color.colorOnPrimary, null),
        textEnd = ResourcesCompat.getColor(context.resources, R.color.colorOnSecondary, null)
    )

    private val actionDownFriend = AnimationColor(
        viewStart = ResourcesCompat.getColor(context.resources, R.color.colorSecondary, null),
        viewEnd = ResourcesCompat.getColor(context.resources, R.color.colorSecondary, null),
        textStart = ResourcesCompat.getColor(context.resources, R.color.colorOnPrimary, null),
        textEnd = ResourcesCompat.getColor(context.resources, R.color.colorOnSecondary, null)
    )

    private val actionUpNoFriend = AnimationColor(
        viewStart = ResourcesCompat.getColor(context.resources, R.color.colorSecondary, null),
        viewEnd = ResourcesCompat.getColor(context.resources, R.color.colorPrimary, null),
        textStart = ResourcesCompat.getColor(context.resources, R.color.colorOnSecondary, null),
        textEnd = ResourcesCompat.getColor(context.resources, R.color.colorOnPrimary, null)
    )

    private val actionUpFriend = AnimationColor(
        viewStart = ResourcesCompat.getColor(context.resources, R.color.colorSecondary, null),
        viewEnd = ResourcesCompat.getColor(context.resources, R.color.colorSecondary, null),
        textStart = ResourcesCompat.getColor(context.resources, R.color.colorOnSecondary, null),
        textEnd = ResourcesCompat.getColor(context.resources, R.color.colorOnPrimary, null)
    )

    fun playSound(soundSelector: SoundSelector) {
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        when (soundSelector) {
            SoundSelector.PLACE_LETTER_IN_EMPTY_BOX -> {
                soundPool.play(placeLetterSoundId,1.0f, 1.0f, 1, 0, 1.0f)
            }
            SoundSelector.SWITCH_LETTERS -> {
                soundPool.play(switchLettersSoundId,1.0f, 1.0f, 1, 0, 1.0f)
            }
            SoundSelector.ACTION_DOWN_LETTER -> {
                soundPool.play(actionDownSoundId,1.0f, 1.0f, 1, 0, 1.0f)
            }
            SoundSelector.ACTION_UP_LETTER -> {
                soundPool.play(actionUpSoundId,1.0f, 1.0f, 1, 0, 1.0f)
            }
        }
    }

    fun getColors(colorSelector: ColorSelector): AnimationColor {
        return when (colorSelector) {
            ACTION_DOWN_NO_FRIEND -> actionDownNoFriend
            ACTION_DOWN_FRIEND -> actionDownFriend
            ACTION_UP_NO_FRIEND -> actionUpNoFriend
            ACTION_UP_FRIEND -> actionUpFriend
        }
    }

    fun addShortVibration(length: Long = 20) {
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT) {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        length,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                vibrator.vibrate(length)
            }
        }
    }
}
