package com.indest.hah.other

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.auth.api.Auth
import com.indest.hah.MainActivity
import com.indest.hah.game.LevelViewModel


class SignInLifecycleObserver(private val registry: ActivityResultRegistry) :
    DefaultLifecycleObserver {

    private val TAG = "SignInLifecycleObserver"
    lateinit var resultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(owner: LifecycleOwner) {
        resultLauncher =
            registry.register(C.SIGN_IN, owner, ActivityResultContracts.StartActivityForResult()) {
                val mainActivity = (owner as? MainActivity)
                val intent = it.data
                if (intent != null && mainActivity != null) {
                    val result = Auth.GoogleSignInApi.getSignInResultFromIntent(intent)
                    val signedInAccount = result?.signInAccount

                    if (result?.isSuccess == true && signedInAccount != null)
                    {
                        mainActivity.startWithCloudSave(signedInAccount)
                    }
                    else {
                        mainActivity.startWithLocalGame()
                    }
                }
            }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        resultLauncher.unregister()
    }

    fun signIn(intent: Intent, activity: Activity) {
        (activity as? MainActivity)?.setState(LevelViewModel.UiState.LOADING)
        resultLauncher.launch(intent)
    }
}