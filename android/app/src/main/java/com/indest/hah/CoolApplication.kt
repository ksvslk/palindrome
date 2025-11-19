package com.indest.hah

import android.app.Application
import com.google.android.gms.games.PlayGamesSdk
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.material.color.DynamicColors
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class CoolApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            // Initialize GMA Next Gen SDK on a background thread.
            MobileAds.initialize(
                applicationContext,
                // Sample AdMob app ID: ca-app-pub-3940256099942544~3347511713
                InitializationConfig.Builder(getString(R.string.admob_app_id)).build()
            ) {
                // Adapter initialization is complete.
            }
            // Other methods on MobileAds can now be called.
        }
        Purchases.configure(
            PurchasesConfiguration.Builder(this, "goog_sJuPoXLNRgjxXfZjxgKMbhZjPKn").build()
        )
        DynamicColors.applyToActivitiesIfAvailable(this)
        PlayGamesSdk.initialize(this)
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
            getString(
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