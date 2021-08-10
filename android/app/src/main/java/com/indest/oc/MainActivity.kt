package com.indest.oc

import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.transition.ChangeTransform
import android.util.Log
import android.view.Window
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import com.indest.oc.fragments.ExitGameDialogFragment
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.indest.oc.declarations.getInsetsController

class MainActivity : AppCompatActivity(), ExitGameDialogFragment.ExitGameDialogListener {

    private val TAG = "MainActivity"
    private lateinit var mainContainer: ConstraintLayout
    private lateinit var playButton: Button
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var consentInformation: ConsentInformation
    private lateinit var consentForm: ConsentForm
    private lateinit var insetsController: WindowInsetsControllerCompat

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG,"onCreate")
        super.onCreate(savedInstanceState)

        with(window) {
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            enterTransition = ChangeTransform()
            exitTransition = ChangeTransform()
        }
        setContentView(R.layout.activity_main)
        mainContainer = findViewById(R.id.main_container)

        insetsController = getInsetsController(mainContainer)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        val debugSettings = ConsentDebugSettings.Builder(this)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId("6423719AD54122E0D92F7F6B44825877")
            .build()

        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .setConsentDebugSettings(debugSettings)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.reset()

        /*
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                // The consent information state was updated.
                // You are now ready to check if a form is available.
                if (consentInformation.isConsentFormAvailable) {
                    loadForm()
                }
            }
        ) {
            Log.e(TAG, "Consent info update failed.")
            enabledPlayButton()
        }
        */

        playButton = findViewById(R.id.play_button)
        playButton.alpha = 0.5f
        enabledPlayButton()

        //analytics = FirebaseAnalytics.getInstance(this)
        //analytics.logEvent("MainActivity_onCreated_finished", null)

        //MobileAds.initialize(this)
        val testDeviceIds = listOf("6423719AD54122E0D92F7F6B44825877")
        val configuration = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        //MobileAds.setRequestConfiguration(configuration)
    }

    private fun enabledPlayButton()
    {
        playButton.alpha = 1.0f
        playButton.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
    }

    private fun loadForm() {
        UserMessagingPlatform.loadConsentForm(
            this,
            { consentForm ->
                this@MainActivity.consentForm = consentForm
                enabledPlayButton()
                TODO("change this back")
                if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.NOT_REQUIRED)  {
                    consentForm.show(
                        this@MainActivity
                    ) { // Handle dismissal by reloading form.
                        loadForm()
                    }
                }
                else
                {
                  enabledPlayButton()
                }
            }
        ) {
            Log.e(TAG, "Consent form loading failed.")
            enabledPlayButton()
        }
    }

    override fun onResume() {
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        super.onResume()
    }

    override fun onBackPressed() {
        showExitDialog()
    }

    private fun showExitDialog() {
        // Create an instance of the dialog fragment and show it
        val dialog = ExitGameDialogFragment()
        dialog.show(supportFragmentManager, "ExitGameDialogFragment")
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        finish()
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        dialog.dismiss()
    }
}

