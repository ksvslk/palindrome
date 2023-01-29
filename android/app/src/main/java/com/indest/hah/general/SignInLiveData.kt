package com.indest.hah.general

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.games.Games
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.indest.hah.MainActivity
import com.indest.hah.R
import com.indest.hah.other.C
import com.indest.hah.other.saveBooleanPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped


@ActivityScoped
class SignInLiveData(
    @ApplicationContext private val context: Context
) :
    DefaultLifecycleObserver, LiveData<AccountStatus>() {

    private val TAG = "SignInLiveData"
    lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private lateinit var auth: FirebaseAuth


    init {
        postValue(AccountStatus.NOT_SET)
        auth = FirebaseAuth.getInstance()
    }

    fun setSignedOutState()
    {
        postValue(AccountStatus.SIGNED_OUT)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // Check if user is signed in (non-null) and update UI accordingly.
        // Check if user is signed in (non-null) and update UI accordingly.
        Log.d(TAG, "User : ${auth.currentUser}")
        auth.currentUser?.let {
            Log.d(TAG, "User anonymous: ${it.isAnonymous}")
            Log.d(TAG, "User metadata: ${it.metadata}")
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        resultLauncher =
            (context as MainActivity).activityResultRegistry.register(
                C.SIGN_IN,
                owner,
                ActivityResultContracts.StartActivityForResult()
            ) {
                val intent = it.data
                if (intent != null) {
                    val result = Auth.GoogleSignInApi.getSignInResultFromIntent(intent)
                    val signedInAccount = result?.signInAccount
                    if (result?.isSuccess == true && signedInAccount != null) {
                        postValue(AccountStatus.SIGNED_IN)
                        if (owner is Activity) {
                            showSignedInPopup(context, signedInAccount)
                        }
                        else if (owner is Fragment){
                            showSignedInPopup(owner.requireActivity() as MainActivity, signedInAccount)
                        }
                    } else {
                        postValue(AccountStatus.SIGNED_OUT)
                    }
                }
                else {
                    postValue(AccountStatus.SIGNED_OUT)
                }
            }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        resultLauncher.unregister()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        if (owner is MainActivity) tryToSignInSilently(context as MainActivity)
    }

    fun signIn(intent: Intent) {
        resultLauncher.launch(intent)
    }

    private fun tryToSignInSilently(activity: MainActivity) {
        val signInOptions =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                .requestScopes(Games.SCOPE_GAMES_SNAPSHOTS)
                .requestServerAuthCode(context.getString(R.string.gpg_web_client_id))
                .build()
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (GoogleSignIn.hasPermissions(account, *signInOptions.scopeArray)) {
            Log.d(TAG, "Already signed in.")
            postValue(AccountStatus.SIGNED_IN)
            showSignedInPopup(activity, account)
        } else {
            // Haven't been signed-in before. Try the silent sign-in first.
            val signInClient = GoogleSignIn.getClient(context, signInOptions)
            signInClient
                .silentSignIn()
                .addOnCompleteListener(
                    activity
                ) { task ->
                    if (task.isSuccessful) {
                        // The signed in account is stored in the task's result.
                        val signedInAccount = task.result
                        postValue(AccountStatus.SIGNED_IN)
                        showSignedInPopup(activity, signedInAccount)
                    } else {
                        Log.d(TAG, "Sign-in explicitly using via UI. ${task.exception}")
                        val oldPreference = activity.saveBooleanPreference(
                            context.getString(R.string.interactive_sign_in_done),
                            true
                        )
                        if (!oldPreference) {
                            signIn(signInClient.signInIntent)
                        } else {
                           postValue(AccountStatus.SIGNED_OUT)
                        }
                        // else preferences were already saved (true) and we do not display sign
                        // in dialog for user anymore as per requirements
                    }
                }
        }
    }

    private fun showSignedInPopup(activity: MainActivity, account: GoogleSignInAccount?) {
        account?.let {
            Games.getGamesClient(activity, it)
                .setViewForPopups(activity.findViewById(android.R.id.content))
        }
    }

}