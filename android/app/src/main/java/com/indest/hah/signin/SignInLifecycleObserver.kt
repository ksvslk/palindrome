package com.indest.hah.signin

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class SignInLifecycleObserver(private val registry: ActivityResultRegistry) :
    DefaultLifecycleObserver {

    lateinit var resultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(owner: LifecycleOwner) {
       resultLauncher = registry.register("signin", owner, ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != Activity.RESULT_OK && it.resultCode != Activity.RESULT_CANCELED) {
                Log.i("SignInLifecycleObserver", "Sign in: it.resultCode != Activity.RESULT_OK && it.resultCode != Activity.RESULT_CANCELED")
            }
        }
    }

    fun signIn(intent: Intent) {
        resultLauncher.launch(intent)
    }
}