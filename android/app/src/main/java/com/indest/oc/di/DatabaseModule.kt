package com.indest.oc.di

import android.content.Context
import com.indest.oc.persistance.LevelDao
import com.indest.oc.persistance.LevelDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): LevelDatabase {
        return LevelDatabase.getInstance(context)
    }

    @Provides
    fun provideLevelDao(levelDatabase: LevelDatabase): LevelDao {
        return levelDatabase.levelDao
    }

}
