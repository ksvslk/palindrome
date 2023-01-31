package com.indest.hah.fx

import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.media.SoundPool
import com.google.android.material.color.MaterialColors
import com.indest.hah.R
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
object EffectsModule {

    @Provides
    @ActivityScoped
    fun provideSoundPool(): SoundPool {
        return SoundPool.Builder().build()
    }

    @Provides
    @ActivityScoped
    fun provideAudioManager(@ApplicationContext context: Context): AudioManager {
        return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    @Provides
    @ActivityScoped
    fun provideSounds(@ApplicationContext context: Context, soundPool: SoundPool): Sounds {
        return Sounds(
            scoreSound = soundPool.load(context, R.raw.score_tick, 1),
            correctSound = soundPool.load(context, R.raw.correct, 1),
            hintNotification = soundPool.load(context, R.raw.hint_notification, 1),
            levelTransition = soundPool.load(context, R.raw.level_transition, 1),
            noteSounds = arrayListOf(
                soundPool.load(context, R.raw.c3_outer, 1),
                soundPool.load(context, R.raw.c4_outer, 1),
                soundPool.load(context, R.raw.g3_outer, 1),
                soundPool.load(context, R.raw.c4_outer, 1),
                soundPool.load(context, R.raw.c5_outer, 1),
                soundPool.load(context, R.raw.g4_outer, 1),
                soundPool.load(context, R.raw.c5_outer, 1),
                soundPool.load(context, R.raw.g5_outer, 1),
                soundPool.load(context, R.raw.c5_outer, 1),
                soundPool.load(context, R.raw.c3_outer, 1),
                soundPool.load(context, R.raw.c5_outer, 1),
                soundPool.load(context, R.raw.c6_outer, 1),
            ),
            sadMode = soundPool.load(context, R.raw.sad_mode, 1),
            normalMode = soundPool.load(context, R.raw.normal_mode, 1),
            switchLettersSoundId = soundPool.load(context, R.raw.double_finger, 1),
            placeLetterSoundId = soundPool.load(context, R.raw.block_hit, 1),
            letterRewardedSoundId = soundPool.load(context, R.raw.hint_revealed, 1),
            triumph = soundPool.load(context, R.raw.triumph, 1)
        )
    }

    @Provides
    @ActivityScoped
    fun provideColors(@ActivityContext context: Context): Colors {
        val successTextColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnTertiaryContainer, Color.RED)
        val currentBoxColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceVariant, Color.RED)
        val boxSuccessColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorTertiaryContainer, Color.RED)
        val lockedColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, Color.RED)
        return Colors(
            successTextColor = successTextColor,
            currentBoxColor = currentBoxColor,
            boxSuccessColor = boxSuccessColor,
            lockedColor = lockedColor
        )
    }

}