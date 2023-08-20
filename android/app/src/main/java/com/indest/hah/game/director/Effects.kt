package com.indest.hah.game.director

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
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

    // Colors
    private val actionDownNoFriend = AnimationColor(
        /*
        viewStart = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceVariant, Color.RED),
        viewEnd = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSecondaryContainer, Color.RED),
        textStart = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.RED),
        textEnd = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSecondaryContainer, Color.RED)
         */
        viewStart = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceVariant, Color.RED),
        viewEnd = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSecondaryContainer, Color.RED),
        textStart = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.RED),
        textEnd = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSecondaryContainer, Color.RED)
    )

    private val actionDownFriend = AnimationColor(
        viewStart = MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimaryContainer, Color.RED),
        viewEnd = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSecondaryContainer, Color.RED),
        textStart = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnPrimaryContainer, Color.RED),
        textEnd = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSecondaryContainer, Color.RED)
    )

    private val actionUpNoFriend = AnimationColor(
        viewStart = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSecondaryContainer, Color.RED),
        viewEnd = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceVariant, Color.RED),
        textStart = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSecondaryContainer, Color.RED),
        textEnd = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.RED)
    )

    private val actionUpFriend = AnimationColor(
        viewStart = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSecondaryContainer, Color.RED),
        viewEnd = MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimaryContainer, Color.RED),
        textStart = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSecondaryContainer, Color.RED),
        textEnd = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnPrimaryContainer, Color.RED)
    )

    fun playSound(soundSelector: SoundSelector) {
        context.getSharedPreferences(
            context.getString(R.string.app_preferences),
            Context.MODE_PRIVATE
        )?.apply {
            if (soundPlayer.isRingerModeNotNormal() || !this.getBoolean(
                    context.getString(R.string.sound_on_pref),
                    true
                )
            ) return
            when (soundSelector) {
                SoundSelector.PLACE_LETTER_IN_EMPTY_BOX -> {
                    soundPlayer.play(soundPlayer.sounds.placeLetterSoundId)
                }
                SoundSelector.SWITCH_LETTERS -> {
                    soundPlayer.play(soundPlayer.sounds.switchLettersSoundId)
                }
                SoundSelector.LETTER_REWARDED -> {
                    soundPlayer.play(soundPlayer.sounds.letterRewardedSoundId, volume = 0.02f)
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

    fun getSuccessBoxColor(): Int {
        return MaterialColors.getColor(context, com.google.android.material.R.attr.colorTertiaryContainer, Color.RED)
    }

    fun getSuccessTextColor(): Int {
        return MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnTertiaryContainer, Color.RED)
    }

    fun getLockedColor(): Int {
        return MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, Color.RED)
    }

    fun getLockedTextColor(): Int {
        return MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnPrimary, Color.RED)
    }

}
