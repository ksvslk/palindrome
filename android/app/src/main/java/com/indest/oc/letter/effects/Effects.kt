package com.indest.oc.letter.effects

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat
import com.indest.oc.R
import com.indest.oc.letter.effects.ColorSelector.ACTION_DOWN_NO_FRIEND
import com.indest.oc.letter.effects.ColorSelector.ACTION_UP_FRIEND
import com.indest.oc.letter.effects.ColorSelector.ACTION_UP_NO_FRIEND
import com.indest.oc.letter.effects.ColorSelector.ACTION_DOWN_FRIEND
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class Effects @Inject constructor(@ActivityContext val context: Context) {

    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Transition durations
    val onClickColorChangeDuration: Long = 200L
    val onReleaseColorChangeDuration: Long = 200L
    val animateToHomeDurationMultiplier = 50

    // Audio, sound and vibration
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val soundPool: SoundPool = SoundPool.Builder().build()
    val am: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Sound effect IDs
    private val switchLettersSoundId = soundPool.load(context, R.raw.swish, 1)
    private val placeLetterSoundId = soundPool.load(context, R.raw.positive_beep, 1)
    private val actionDownSoundId = soundPool.load(context, R.raw.action_down, 1)
    private val actionUpSoundId = soundPool.load(context, R.raw.action_up, 1)
    private val draggingSoundId = soundPool.load(context, R.raw.drums, 1)

    // Dragging loop id
    private var draggingLoopStream = -1

    // Colors
    private val actionDownNoFriend = AnimationColor(
        viewStart = ContextCompat.getColor(context, R.color.colorPrimary),
        viewEnd = ContextCompat.getColor(context, R.color.colorSecondary),
        textStart = ContextCompat.getColor(context, R.color.colorOnPrimary),
        textEnd = ContextCompat.getColor(context, R.color.colorOnSecondary)
    )

    private val actionDownFriend = AnimationColor(
        viewStart = ContextCompat.getColor(context, R.color.colorSecondary),
        viewEnd = ContextCompat.getColor(context, R.color.colorSecondary),
        textStart = ContextCompat.getColor(context, R.color.colorOnPrimary),
        textEnd = ContextCompat.getColor(context, R.color.colorOnSecondary)
    )

    private val actionUpNoFriend = AnimationColor(
        viewStart = ContextCompat.getColor(context, R.color.colorSecondary),
        viewEnd = ContextCompat.getColor(context, R.color.colorPrimary),
        textStart = ContextCompat.getColor(context, R.color.colorOnSecondary),
        textEnd = ContextCompat.getColor(context, R.color.colorOnPrimary)
    )

    private val actionUpFriend = AnimationColor(
        viewStart = ContextCompat.getColor(context, R.color.colorSecondary),
        viewEnd = ContextCompat.getColor(context, R.color.colorSecondary),
        textStart = ContextCompat.getColor(context, R.color.colorOnSecondary),
        textEnd = ContextCompat.getColor(context, R.color.colorOnPrimary)
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
            SoundSelector.DRAGGING_LETTER -> {
                draggingLoopStream = soundPool.play(draggingSoundId,1.0f, 1.0f, 1, -1, 1.0f)
            }
        }
    }

    fun stopInifinteLoop(soundSelector: SoundSelector)
    {
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        when (soundSelector) {
            SoundSelector.DRAGGING_LETTER -> {
                soundPool.stop(draggingLoopStream)
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
