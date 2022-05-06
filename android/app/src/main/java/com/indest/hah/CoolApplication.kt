package com.indest.hah

import android.app.Application
import com.google.android.gms.games.PlayGamesSdk
import dagger.hilt.android.HiltAndroidApp
/*import leakcanary.LeakCanary
import leakcanary.ToastEventListener*/


@HiltAndroidApp
class CoolApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PlayGamesSdk.initialize(this)

/*       if (BuildConfig.DEBUG)
        {
            LeakCanary.config = LeakCanary.config.run {
                copy(
                    eventListeners = eventListeners.filter {
                        it !is ToastEventListener
                    }
                )
            }
        }*/

    }
}