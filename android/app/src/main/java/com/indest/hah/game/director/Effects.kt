package com.indest.hah.game.director

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat
import com.indest.hah.R
import com.indest.hah.fx.SoundPlayer
import com.indest.hah.game.director.data.AnimationColor
import com.indest.hah.game.director.enums.ColorSelector
import com.indest.hah.game.director.enums.ColorSelector.*
import com.indest.hah.game.director.enums.SoundSelector
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class Effects @Inject constructor(
    @ActivityContext val context: Context,
    private val soundPlayer: SoundPlayer,
) {

    // Transition durations
    val onClickColorChangeDuration: Long = 200L
    val onReleaseColorChangeDuration: Long = 200L
    val animateToHomeDurationMultiplier = 50

    // Audio, sound and vibration
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

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
        context.getSharedPreferences(
            context.getString(R.string.app_preferences),
            Context.MODE_PRIVATE
        )?.let {
            if (soundPlayer.isRingerModeNotNormal() || !it.getBoolean(context.getString(R.string.sound_on_pref), true)) return
            when (soundSelector) {
                SoundSelector.PLACE_LETTER_IN_EMPTY_BOX -> {
                    soundPlayer.play(soundPlayer.sounds.placeLetterSoundId)
                }
                SoundSelector.SWITCH_LETTERS -> {
                    soundPlayer.play(soundPlayer.sounds.switchLettersSoundId)
                }
                SoundSelector.LETTER_REWARDED -> {
                    soundPlayer.play(soundPlayer.sounds.letterRewardedSoundId)
                }
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

    fun getSuccessBoxColor():Int
    {
        return ContextCompat.getColor(context, R.color.successBoxColor)
    }

    fun getLockedColor(): Int {
        return ContextCompat.getColor(context, R.color.letterRewardedColor)
    }

    fun getLockedTextColor(): Int {
        return ContextCompat.getColor(context, R.color.textOnRewardedColor)
    }

    fun addShortVibration(length: Long = 20) {
        context.getSharedPreferences(
            context.getString(R.string.app_preferences),
            Context.MODE_PRIVATE
        )?.let {
            if (soundPlayer.isRingerModeNotSilent() && it.getBoolean(
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
