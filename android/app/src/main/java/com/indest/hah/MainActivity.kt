package com.indest.hah

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.forEach
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ReportFragment.Companion.reportFragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.games.AuthenticationResult
import com.google.android.gms.games.GamesSignInClient
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.Player
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.BaseTransientBottomBar.ANIMATION_MODE_FADE
import com.google.android.material.snackbar.Snackbar
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PlayGamesAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.indest.hah.ads.RewardType
import com.indest.hah.databinding.ActivityMainBinding
import com.indest.hah.fx.SoundPlayer
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.general.AccountStatus
import com.indest.hah.general.GeneralViewModel
import com.indest.hah.other.C
import com.indest.hah.other.LevelSelectType
import com.indest.hah.other.hideSystemBars
import com.indest.hah.other.mapCountryCodeToLanguageAndFlag
import com.indest.hah.other.safeNavigate
import com.indest.hah.persistance.dto.SaveGame
import com.indest.hah.savegame.SnapshotHelper
import com.indest.hah.views.ButtonState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.math.min


private const val RECONNECT_TIMER_START_MILLISECONDS = 1L * 1000L
private const val RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L // 15 minutes

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity_"
    lateinit var binding: ActivityMainBinding
    private val levelViewModel: LevelViewModel by viewModels()
    private val generalViewModel: GeneralViewModel by viewModels()
    private var consentInformation: ConsentInformation? = null
    private var consentForm: ConsentForm? = null

    // how long before trying reconnect to firebase
    private var reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS
    private val handler = Handler(Looper.getMainLooper())

    @Inject
    lateinit var soundPlayer: SoundPlayer

    @Inject
    lateinit var snapshotHelper: SnapshotHelper
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth

        levelViewModel.setAccountStatus(AccountStatus.LOADING)

        val gamesSignInClient = PlayGames.getGamesSignInClient(this)
        gamesSignInClient.isAuthenticated.addOnCompleteListener { isAuthenticatedTask: Task<AuthenticationResult> ->
            handleAuthTask(gamesSignInClient, isAuthenticatedTask)
        }
        requestLatestConsentInformation()
        MobileAds.initialize(applicationContext)

        generalViewModel.toString()
        applyCustomWindowInsets()

        setUpNavController()
        collectRewardInfo()
        collectVariousEvents()
        collectPlayerLevelsForNotification()

        binding.root.doOnNextLayout {
            val exclusions = listOf(Rect(0, 0, it.width, it.height))
            ViewCompat.setSystemGestureExclusionRects(binding.root, exclusions)
        }

    }

    override fun onStop() {
        super.onStop()
        levelViewModel.saveGameImmediately()
    }


    fun signInManually() {
        levelViewModel.setAccountStatus(AccountStatus.LOADING)
        val gamesSignInClient = PlayGames.getGamesSignInClient(this)
        gamesSignInClient.signIn()
            .addOnCompleteListener { isAuthenticatedTask: Task<AuthenticationResult> ->
                handleAuthTask(gamesSignInClient, isAuthenticatedTask)
            }
    }


    private fun handleAuthTask(
        gamesSignInClient: GamesSignInClient,
        isAuthenticatedTask: Task<AuthenticationResult>
    ) {
        val isAuthenticated = isAuthenticatedTask.isSuccessful &&
                isAuthenticatedTask.result.isAuthenticated
        if (isAuthenticated) {
            // Continue with Play Games Services
            signInToFirebaseWithCredential(gamesSignInClient)
        } else {
            // Disable your integration with Play Games Services or show a
            // login button to ask  players to sign-in. Clicking it should
            // call GamesSignInClient.signIn().
            levelViewModel.setAccountStatus(AccountStatus.SIGNED_OUT)
            levelViewModel.currentlySignedInPlayerName = "Unknown"
        }
    }


    private fun setPlayerNameAndSignedInStatus(accountStatus: AccountStatus) {
        if (levelViewModel.accountStatus.value !in setOf(
                AccountStatus.SIGNED_IN,
                AccountStatus.SIGNED_IN_WITH_FIREBASE
            )
        ) {
            PlayGames.getPlayersClient(this).currentPlayer.addOnCompleteListener { mTask: Task<Player> ->
                if (mTask.isSuccessful) {
                    levelViewModel.currentlySignedInPlayerName = mTask.result.displayName
                }
                levelViewModel.setAccountStatus(accountStatus)
            }
        }
    }


    private fun collectPlayerLevelsForNotification() {
        lifecycleScope.launch {/*
            lifecycleScope.launchWhenCreated { */
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                levelViewModel.newPlayerLevels.collectLatest { (levelNr, addedLevelsCount, author) ->
                    if (levelNr == -1 && addedLevelsCount == -1 && author == "") return@collectLatest
                    if (addedLevelsCount == 1) {
                        showNewLevelMessage(
                            author,
                            levelNr
                        )
                    } else if (addedLevelsCount > 1) {
                        showNewLevelMessage(
                            author,
                            lastAddedLevel = -1,
                            multipleLevels = addedLevelsCount
                        )
                    }
                    levelViewModel.resetNewPlayersLevels()
                }
            }
        }
    }


    private fun showNewLevelMessage(
        player: String,
        lastAddedLevel: Int,
        multipleLevels: Int = -1
    ) {
        val snackMessage =
            if (multipleLevels == -1) "New level by $player!" else "$multipleLevels levels added!"

        val snack =
            Snackbar.make(findViewById(android.R.id.content), snackMessage, 15000)
                .setAction("Play") {
                    val navHostFragment =
                        supportFragmentManager.findFragmentById(R.id.fragments_container) as NavHostFragment
                    val navController = navHostFragment.navController
                    val currentlyActiveFragment =
                        navHostFragment.navController.currentDestination?.id

                    // Check if there is only one new level
                    if (multipleLevels == -1) {
                        if (currentlyActiveFragment == R.id.navigation_final_screen) { // If on the final screen
                            levelViewModel.changeLevel(lastAddedLevel)
                            navController.safeNavigate(R.id.action_navigation_final_screen_to_navigation_level)
                        } else { // If on any other screen
                            levelViewModel.changeLevel(lastAddedLevel)
                        }
                    } else { // If there are multiple new levels
                        if (currentlyActiveFragment == R.id.navigation_final_screen) { // If on the final screen
                            navController.safeNavigate(
                                R.id.action_navigation_final_screen_to_navigation_level_select,
                                LevelSelectType.PLAYER_LEVELS
                            )
                        } else { // If on any other screen
                            navController.safeNavigate(R.id.action_navigation_level_to_navigation_stages)
                        }
                    }
                }
                .setAnimationMode(ANIMATION_MODE_FADE)

        val params = snack.view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP
        params.topMargin = resources.getDimension(com.intuit.sdp.R.dimen._16sdp).toInt()
        snack.view.layoutParams = params
        /*snack.view.background = ContextCompat.getDrawable(applicationContext, R.drawable.new_level_message_rectangle)*/
        snack.show()
    }

    private fun signInToFirebaseWithCredential(gamesSignInClient: GamesSignInClient) {
        gamesSignInClient
            .requestServerSideAccess(getString(R.string.oauth_game_server_client_id), false)
            .addOnCompleteListener { authTokenTask: Task<String?> ->
                if (authTokenTask.isSuccessful) {
                    authTokenTask.result?.let { serverAuthToken ->
                        val credential = PlayGamesAuthProvider.getCredential(serverAuthToken)
                        auth.signInWithCredential(credential).addOnSuccessListener { _ ->
                            reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS
                            setPlayerNameAndSignedInStatus(AccountStatus.SIGNED_IN_WITH_FIREBASE)
                        }.addOnFailureListener {
                            retryFirebaseSignInWithExponentialBackoff(gamesSignInClient)
                            setPlayerNameAndSignedInStatus(AccountStatus.SIGNED_IN)
                            Log.e(TAG, "Were not able to log in to firebase", it)
                        }
                    }
                } else {
                    retryFirebaseSignInWithExponentialBackoff(gamesSignInClient)
                    setPlayerNameAndSignedInStatus(AccountStatus.SIGNED_IN)
                    Log.w(TAG, "Firebase login failure.")
                }
            }
    }

    /**
     * Retries the billing service connection with exponential backoff, maxing out at the time
     * specified by RECONNECT_TIMER_MAX_TIME_MILLISECONDS.
     */
    private fun retryFirebaseSignInWithExponentialBackoff(gamesSignInClient: GamesSignInClient) {
        if (generalViewModel.isConnectedToInternet()) {
            handler.postDelayed(
                {
                    signInToFirebaseWithCredential(gamesSignInClient)
                },
                reconnectMilliseconds
            )
            reconnectMilliseconds = min(
                reconnectMilliseconds * 2,
                RECONNECT_TIMER_MAX_TIME_MILLISECONDS
            )
        }
    }


    override fun onResume() {
        super.onResume()
        generalViewModel.refreshPurchasesTriedRedeem()
    }


    private fun submitScore(score: Long, playerLevels: Boolean = false) {
        val leaderboardId = if (playerLevels) {
            getString(R.string.leaderboard_player_levels_leaderboard)
        } else {
            getString(R.string.leaderboard_crme_de_la_crme)
        }
        PlayGames.getLeaderboardsClient(this)
            .submitScore(
                leaderboardId,
                score
            )
    }

    private fun unlockAchievement(achievement: String) {
        PlayGames.getAchievementsClient(this).unlock(achievement)
    }

    private fun requestLatestConsentInformation() {
        // Set tag for underage of consent. false means users are not underage.
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(true)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(applicationContext)
        consentInformation?.requestConsentInfoUpdate(
            this,
            params,
            {
                // The consent information state was updated.
                // You are now ready to check if a form is available.
                if (consentInformation?.isConsentFormAvailable == true) {
                    loadForm();
                }
            },
            {
                Log.e(TAG, "Message: ${it.message}. Code: ${it.errorCode}.")
            })
    }

    private fun loadForm() {
        UserMessagingPlatform.loadConsentForm(
            applicationContext,
            { consentForm ->
                this@MainActivity.consentForm = consentForm
                if (consentInformation?.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                    this@MainActivity.consentForm?.show(
                        this@MainActivity
                    ) { // Handle dismissal by reloading form.
                        loadForm()
                    }
                }
            }
        ) {
            Log.e(TAG, "Message: ${it.message}. Code: ${it.errorCode}.")
        }
    }


    private fun collectVariousEvents() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED)
            {
                levelViewModel.variousEventFlow.collect { event ->
                    when (event) {
                        LevelViewModel.VariousEvent.LoadGame -> {
                            val saveGame: SaveGame? = snapshotHelper.load(this@MainActivity)
                            levelViewModel.loadLatestLevelFromCloudOrLocal(saveGame)
                        }

                        is LevelViewModel.VariousEvent.SaveGame -> {
                            snapshotHelper.save(
                                this@MainActivity,
                                event.localSaveGame,
                                event.calculatedGameProgress
                            )
                        }

                        is LevelViewModel.VariousEvent.SubmitScore -> {
                            submitScore(event.score, event.playerLevels)
                        }

                        is LevelViewModel.VariousEvent.UnlockAchievement -> {
                            unlockAchievement(event.achievement)
                        }
                    }
                }
            }
        }
    }

    private fun collectRewardInfo() {
        binding.watchAdTop.setOnClickListener {
            generalViewModel.showRewardedVideo(this, RewardType.SAD_MODE)
            binding.watchAdTop.visibility = View.GONE
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED)
            {
                launch {
                    generalViewModel.rewardState.distinctUntilChangedBy { it }.collect {
                        if (it == RewardType.SAD_MODE) {
                            levelViewModel.resetIntervalCount(getString(R.string.sad_mode_interval))
                            executeSadModeEffects(false)
                            generalViewModel.updateRewardState(RewardType.NONE)
                            levelViewModel.logRewardGiven(RewardType.SAD_MODE)
                        } else if (it == RewardType.SAD_AD_FAILED) {
                            executeSadModeEffects(true)
                            generalViewModel.updateRewardState(RewardType.NONE)
                        }
                    }
                }

                launch {
                    generalViewModel.buttonState.collect { buttonInfo ->
                        if (buttonInfo.sadMode && buttonInfo.state == ButtonState.AD) {
                            if (!binding.container.isSadMode) {
                                executeSadModeEffects(true)
                            }
                            binding.container.isSadMode = true
                            binding.watchAdTop.visibility = View.VISIBLE
                            binding.container.invalidate()
                        } else {
                            binding.watchAdTop.visibility = View.GONE
                            binding.container.isSadMode = false
                            binding.container.invalidate()
                        }
                    }
                }
            }
        }
    }

    private fun showSadModeToast() {
        val text = getString(R.string.entering_sad_mode)
        val duration = Toast.LENGTH_LONG
        val toast = Toast.makeText(applicationContext, text, duration)
        toast.show()
    }

    private fun showNormalModeToast() {
        val text = getString(R.string.entering_normal_mode)
        val duration = Toast.LENGTH_LONG
        val toast = Toast.makeText(applicationContext, text, duration)
        toast.show()
    }


    private fun applyCustomWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.container) { _, insets ->
            WindowInsetsCompat.Builder()
                .setInsets(
                    WindowInsetsCompat.Type.systemBars(),
                    Insets.of(
                        insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                        insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                        insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                        0
                    )
                ).build()
            WindowInsetsCompat.CONSUMED
        }
    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hideSystemBars(binding.root)
    }


    private fun setUpNavController() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragments_container) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView: BottomNavigationView = binding.bottomNavView
        bottomNavigationView.setupWithNavController(navController)
        bottomNavigationView.menu.setGroupCheckable(0, true, false)
        bottomNavigationView.menu.forEach { it.isChecked = true }
    }


    private fun executeSadModeEffects(isSadMode: Boolean) {
        when (isSadMode) {
            true -> {
                showSadModeToast()
                soundPlayer.play(soundPlayer.sounds.sadMode, volume = 0.5f)
            }

            false -> {
                showNormalModeToast()
                soundPlayer.play(soundPlayer.sounds.normalMode, volume = 0.5f)
            }
        }
    }


    fun updateProgressBar(progress: Pair<Int, String>, countryCode:String) {
        binding.levelNumber.text = progress.second
        val drawableId = countryCode.mapCountryCodeToLanguageAndFlag(applicationContext).second
        binding.levelFlag.setImageResource(drawableId)
        binding.levelFlag.invalidate()
        binding.gameProgress.progress = progress.first
    }



    fun downloadHintPictureIfNeeded(levelNr: Int)
    {
        Log.d(TAG, "downloadHintIfNeeded")

        // let's assume each time we don't have the picture
        levelViewModel.setPictureDownloaded(isPresent = false)

        if (checkIfHintExistsForLevel(levelNr))
        {
            levelViewModel.setPictureDownloaded(isPresent = true)
        }
        else {
            val storage = Firebase.storage
            val gsReference = storage.getReferenceFromUrl("${C.FIREBASE_PICS_STORAGE_PATH}level${levelNr}.jpg")
            this.filesDir?.let {
                val localFile = File(it, "level${levelNr}.jpg")
                gsReference.getFile(localFile).addOnSuccessListener {
                    levelViewModel.setPictureDownloaded(isPresent = true)
                    Log.d(TAG, "Picture hint is ready to show.")
                }.addOnFailureListener {
                    levelViewModel.setPictureDownloaded(isPresent = false)
                    Log.e(TAG, "Failed to create local file from storage.", it)
                }
            }
        }
    }

    private fun checkIfHintExistsForLevel(levelNr:Int) : Boolean
    {
        if (levelNr <= C.PLAYER_LEVELS_COVERED_LOCALLY) return true
        val outputDir = this.filesDir
        outputDir?.let {
            val localFile = File(it, "level$levelNr.jpg")
            return localFile.exists()
        }
        return false
    }

}