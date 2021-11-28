package com.indest.hah.letter.effects

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat
import com.indest.hah.R
import com.indest.hah.letter.effects.ColorSelector.*
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class Effects @Inject constructor(@ActivityContext val context: Context) {

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Transition durations
    val onClickColorChangeDuration: Long = 200L
    val onReleaseColorChangeDuration: Long = 200L
    val animateToHomeDurationMultiplier = 50

    // Audio, sound and vibration
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val soundPool: SoundPool = SoundPool.Builder().build()

    // Sound effect IDs
    private val switchLettersSoundId = soundPool.load(context, R.raw.double_finger, 1)
    private val placeLetterSoundId = soundPool.load(context, R.raw.block_hit, 1)

    // Colors
    private val actionDownNoFriend = AnimationColor(
        viewStart = ContextCompat.getColor(context, R.color.colorPrimary),
        viewEnd = ContextCompat.getColor(context, R.color.colorSecondary),
        textStart = ContextCompat.getColor(context, R.color.colorOnPrimary),
        textEnd = ContextCompat.getColor(context, R.color.colorOnPrimary)
    )

    private val actionDownFriend = AnimationColor(
        viewStart = ContextCompat.getColor(context, R.color.colorSecondary),
        viewEnd = ContextCompat.getColor(context, R.color.colorSecondary),
        textStart = ContextCompat.getColor(context, R.color.colorOnPrimary),
        textEnd = ContextCompat.getColor(context, R.color.colorOnPrimary)
    )

    private val actionUpNoFriend = AnimationColor(
        viewStart = ContextCompat.getColor(context, R.color.colorSecondary),
        viewEnd = ContextCompat.getColor(context, R.color.colorPrimary),
        textStart = ContextCompat.getColor(context, R.color.colorOnPrimary),
        textEnd = ContextCompat.getColor(context, R.color.colorOnPrimary)
    )

    private val actionUpFriend = AnimationColor(
        viewStart = ContextCompat.getColor(context, R.color.colorSecondary),
        viewEnd = ContextCompat.getColor(context, R.color.colorSecondary),
        textStart = ContextCompat.getColor(context, R.color.colorOnPrimary),
        textEnd = ContextCompat.getColor(context, R.color.colorOnPrimary)
    )

    fun playSound(soundSelector: SoundSelector) {
        context.getSharedPreferences("heh_pref", Context.MODE_PRIVATE)?.let {

            if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL ||
                !it.getBoolean(context.getString(R.string.sound_on_pref), true)
            ) return

            when (soundSelector) {
                SoundSelector.PLACE_LETTER_IN_EMPTY_BOX -> {
                    soundPool.play(placeLetterSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
                }
                SoundSelector.SWITCH_LETTERS -> {
                    soundPool.play(switchLettersSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
                }
                else -> {}
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
        context.getSharedPreferences("heh_pref", Context.MODE_PRIVATE)?.let {
            if (audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT && it.getBoolean(
                    context.getString(
                        R.string.vibration_on_pref
                    ), true
                )
            ) {
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

}
