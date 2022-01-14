package com.indest.hah

import android.app.Activity
import android.content.Context
import android.net.*
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.forEach
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.ads.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.indest.hah.databinding.ActivityGameBinding
import com.indest.hah.util.hideSystemBars
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.games.Games
import com.indest.hah.fragments.GamePlayFragment
import com.indest.hah.fragments.LoadingFragment
import com.indest.hah.fragments.TutorialFragment
import com.indest.hah.game.OverallState
import com.indest.hah.game.OverallState.*
import com.indest.hah.game.OverallViewModel
import com.indest.hah.util.ConnectionLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.net.HttpURLConnection
import java.net.URL

@AndroidEntryPoint
class GameActivity : AppCompatActivity() {

    private val TAG = "GameActivity"
    lateinit var binding: ActivityGameBinding
    private lateinit var navController: NavController
    private lateinit var connectionLiveData: ConnectionLiveData
    private val overallViewModel: OverallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        connectionLiveData = ConnectionLiveData(this)

        binding = ActivityGameBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.container) { _, insets ->
            WindowInsetsCompat.Builder().setInsets(
                WindowInsetsCompat.Type.systemBars(),
                Insets.of(
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    0
                )
            ).build()
        }

        setContentView(binding.root)

        val bottomNavigationView: BottomNavigationView = binding.bottomNavView
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragments_container) as NavHostFragment
        navController = navHostFragment.navController

        bottomNavigationView.setupWithNavController(navController)
        bottomNavigationView.menu.setGroupCheckable(0, true, false)
        bottomNavigationView.menu.forEach { it.isChecked = true }

        MobileAds.initialize(this) {}

    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        Log.i(TAG, "onWindowFocusChanged")
        super.onWindowFocusChanged(hasFocus)
        hideSystemBars(binding.root)
    }

    private fun startListeningConnectionStatus() {
        connectionLiveData.observe(this@GameActivity, { hasNetwork ->
            Log.i(TAG, "hasNetwork: $hasNetwork.")
            lifecycleScope.launch(Dispatchers.IO) {
                Log.i(TAG, "hier")
                val url = URL("https://www.google.com")
                (url.openConnection() as? HttpURLConnection)?.runCatching {
                    requestMethod = "GET"
                    val hasInternet = responseCode == HttpURLConnection.HTTP_OK
                    change(hasInternet, hasNetwork)
                    Log.i(TAG, "got result")
                }?.onFailure {
                    Log.i(TAG, "Something went wrong: ${it.message}")
                }
            }
        })
    }

    private fun change(hasInternet: Boolean, hasNetwork: Boolean) {
        val newState = if (hasNetwork && hasInternet) ONLINE else OFFLINE
        Log.i(TAG, "New state: $newState!")
        if (newState != overallViewModel.uiState.value.state) {
            overallViewModel.changeState(newState)
            Log.i(TAG, "Internet: $newState!")
        } else {
            Log.i(TAG, "currentInternet: ${overallViewModel.uiState.value.state}!")
        }
    }

    private fun signInSilently() {
        val signInOptions =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                .build()
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (GoogleSignIn.hasPermissions(account, *signInOptions.scopeArray)) {
            // Already signed in.
            // The signed in account is stored in the 'account' variable.
            if (account != null) {
                Games.getGamesClient(this, account)
                    .setViewForPopups(findViewById(android.R.id.content))
            }
            overallViewModel.changeState(ONLINE)
            startListeningConnectionStatus()
        } else {
            // Haven't been signed-in before. Try the silent sign-in first.
            val signInClient = GoogleSignIn.getClient(this, signInOptions)
            signInClient
                .silentSignIn()
                .addOnCompleteListener(
                    this
                ) { task ->
                    if (task.isSuccessful) {
                        // The signed in account is stored in the task's result.
                        val signedInAccount = task.result
                        Games.getGamesClient(this, signedInAccount)
                            .setViewForPopups(findViewById(android.R.id.content))
                        Log.i(TAG, "Signed in successfully.")
                        overallViewModel.changeState(ONLINE)
                        startListeningConnectionStatus()
                    } else if (task.isComplete) {
                        overallViewModel.changeState(ONLINE)
                        startListeningConnectionStatus()
                        Log.i(
                            TAG,
                            "Player needs to sign-in explicitly using via UI. ${task.exception}"
                        )
                        getSharedPreferences(
                            getString(R.string.preferences),
                            Context.MODE_PRIVATE
                        )?.let {
                            if (!it.getBoolean(
                                    getString(R.string.interactive_sign_in_done),
                                    false
                                )
                            ) {
                                with(it.edit()) {
                                    putBoolean(
                                        getString(R.string.interactive_sign_in_done),
                                        true
                                    )
                                    apply()
                                }
                                signInToGoogle()
                            }
                        }
                    }
                }
        }
    }

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != Activity.RESULT_OK && it.resultCode != Activity.RESULT_CANCELED) {
                Toast.makeText(
                    this,
                    "Something went wrong :|",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun signInToGoogle() {
        overallViewModel.changeState(LOADING)
        val signInClient = GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN
        )
        activityResultLauncher.launch(signInClient.signInIntent)
    }

    override fun onResume() {
        super.onResume()
        signInSilently()
    }
}
