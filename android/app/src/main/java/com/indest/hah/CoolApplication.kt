package com.indest.hah

import android.app.Application
import com.google.android.gms.common.wrappers.InstantApps
import com.google.android.material.color.DynamicColors
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

import leakcanary.LeakCanary
import leakcanary.ToastEventListener


@HiltAndroidApp
class CoolApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        val appCheckFactory = if (BuildConfig.DEBUG) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        firebaseAppCheck.installAppCheckProviderFactory(
            appCheckFactory
        )
        FirebaseAnalytics.getInstance(applicationContext).setUserProperty(
            getString(R.string.analytics_user_prop),
            if (InstantApps.isInstantApp(applicationContext)) getString(R.string.status_instant) else getString(
                R.string.status_installed
            )
        )
    /*
      if (BuildConfig.DEBUG)
        {
            LeakCanary.config = LeakCanary.config.run {
                copy(
                    eventListeners = eventListeners.filter {
                        it !is ToastEventListener
                    }
                )
            }
        }
        */

    }
}