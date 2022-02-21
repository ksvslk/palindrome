package com.indest.hah

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.indest.hah.persistance.LevelRepository
import dagger.hilt.android.HiltAndroidApp
import leakcanary.LeakCanary
import leakcanary.ToastEventListener
import java.util.concurrent.Executors
import javax.inject.Inject

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