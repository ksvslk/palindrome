package com.indest.hah.savegame

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object SaveModule {

    @Provides
    @ViewModelScoped
    fun provideCloudSave(@ApplicationContext context: Context): SnapshotHelper {
        return SnapshotHelper(context)
    }
}