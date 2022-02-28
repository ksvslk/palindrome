package com.indest.hah

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import leakcanary.LeakCanary
import leakcanary.ToastEventListener

@HiltAndroidApp
class CoolApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LeakCanary.config = LeakCanary.config.run {
            copy(
                eventListeners = eventListeners.filter {
                    it !is ToastEventListener
                }
            )
        }

 /*       StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )*/
    }
}