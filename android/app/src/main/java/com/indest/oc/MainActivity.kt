package com.indest.oc

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.transition.ChangeTransform
import android.util.Log
import android.view.Window
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.indest.oc.util.Util


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var mainContainer: ConstraintLayout
    private lateinit var playButton: Button
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var consentInformation: ConsentInformation
    private lateinit var consentForm: ConsentForm

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG,"onCreate")
        super.onCreate(savedInstanceState)

        val debugSettings = ConsentDebugSettings.Builder(this)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId("6423719AD54122E0D92F7F6B44825877")
            .build()

        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .setConsentDebugSettings(debugSettings)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        consentInformation.reset();

        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                // The consent information state was updated.
                // You are now ready to check if a form is available.
                if (consentInformation.isConsentFormAvailable) {
                    loadForm();
                }
            }
        ) {
            // Handle the error.
        }

        with(window) {
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            enterTransition = ChangeTransform()
            exitTransition = ChangeTransform()
        }
        setContentView(R.layout.activity_main)

        mainContainer = findViewById(R.id.main_container)
        Util.activateFullscreen(this, mainContainer)

        playButton = findViewById(R.id.play_button)
        playButton.setOnClickListener{
            val intent = Intent(this, GameActivity::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            } else {
                startActivity(intent)
            }
        }

        analytics = FirebaseAnalytics.getInstance(this)
        analytics.logEvent("MainActivity_onCreated_finished", null)

        MobileAds.initialize(this)
        val testDeviceIds = listOf("6423719AD54122E0D92F7F6B44825877")
        val configuration = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        MobileAds.setRequestConfiguration(configuration)
    }

    private fun loadForm() {
        UserMessagingPlatform.loadConsentForm(
            this,
            { consentForm ->
                this@MainActivity.consentForm = consentForm
                if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                    consentForm.show(
                        this@MainActivity
                    ) { // Handle dismissal by reloading form.
                        loadForm()
                    }
                }
            }
        ) {
            /// Handle Error.
        }
    }

    override fun onResume() {
        super.onResume()
        Util.activateFullscreen(this, mainContainer)
    }
}

