package com.indest.hah

import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowMetricsCalculator
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.games.AuthenticationResult
import com.google.android.gms.games.GamesSignInClient
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.Player
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.BaseTransientBottomBar.ANIMATION_MODE_SLIDE
import com.google.android.material.snackbar.Snackbar
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PlayGamesAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.indest.hah.ads.RewardType
import com.indest.hah.databinding.ActivityMainBinding
import com.indest.hah.fragments.*
import com.indest.hah.fx.SoundPlayer
import com.indest.hah.game.creation.BookMode
import com.indest.hah.game.creation.WindowSizeClass
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.general.AccountStatus
import com.indest.hah.general.GeneralViewModel
import com.indest.hah.other.C
import com.indest.hah.other.hideSystemBars
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.persistance.dto.SaveGame
import com.indest.hah.savegame.SnapshotHelper
import com.indest.hah.views.ButtonState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity(),
    ExitGameDialogFragment.ExitGameDialogListener {

    private val TAG = "MainActivity"
    lateinit var binding: ActivityMainBinding
    private val levelViewModel: LevelViewModel by viewModels()
    private val generalViewModel: GeneralViewModel by viewModels()
    private var consentInformation: ConsentInformation? = null
    private var consentForm: ConsentForm? = null

    @Inject
    lateinit var soundPlayer: SoundPlayer

    @Inject
    lateinit var snapshotHelper: SnapshotHelper
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        levelViewModel.setWindowSizeClass(WindowSizeClass.NOT_SET)
        levelViewModel.setBookMode(BookMode.NOT_SET)
        updateFoldModeInfo()
        signInWithFirebase()

        val gamesSignInClient = PlayGames.getGamesSignInClient(this)
        gamesSignInClient.isAuthenticated.addOnCompleteListener { isAuthenticatedTask: Task<AuthenticationResult> ->
            handleAuthTask(gamesSignInClient, isAuthenticatedTask)
        }
        requestLatestConsentInformation()

        MobileAds.initialize(applicationContext) {
        }

        generalViewModel.toString()
        applyCustomWindowInsets()
        windowSizeClassUpdater()
        setUpNavController()
        progressBarUpdater()
        collectRewardInfo()
        collectVariousEvents()
        collectPlayerLevels()

        lifecycleScope.launch {
            levelViewModel.collectPlayerLevels()
        }
        binding.root.doOnNextLayout {
            val exclusions = listOf(Rect(0, 0, it.width, it.height))
            ViewCompat.setSystemGestureExclusionRects(binding.root, exclusions)
        }

    }


    fun signInManually() {
        levelViewModel.setAccountStatus(AccountStatus.LOADING)
        generalViewModel.setAccountStatus(AccountStatus.LOADING)
        val gamesSignInClient = PlayGames.getGamesSignInClient(this)
        gamesSignInClient.signIn()
            .addOnCompleteListener { isAuthenticatedTask: Task<AuthenticationResult> ->
                handleAuthTask(gamesSignInClient, isAuthenticatedTask)
            }
    }


    private fun signInWithFirebase() {
        levelViewModel.setAccountStatus(AccountStatus.LOADING)
        generalViewModel.setAccountStatus(AccountStatus.LOADING)
        val gamesSignInClient = PlayGames.getGamesSignInClient(this)
        gamesSignInClient.isAuthenticated.addOnCompleteListener { isAuthenticatedTask: Task<AuthenticationResult> ->
            handleAuthTask(gamesSignInClient, isAuthenticatedTask)
        }
    }

    private fun handleAuthTask(
        gamesSignInClient: GamesSignInClient,
        isAuthenticatedTask: Task<AuthenticationResult>
    ) {
        if (!isAuthenticatedTask.isSuccessful) {
            // Disable Play Games Services integration
            levelViewModel.setAccountStatus(AccountStatus.SIGNED_OUT)
            generalViewModel.setAccountStatus(AccountStatus.SIGNED_OUT)
            levelViewModel.currentlySignedInPlayerName = "Unknown"
            return
        }

        val authenticationResult = isAuthenticatedTask.result
        if (!authenticationResult.isAuthenticated) {
            // Disable Play Games Services integration
            levelViewModel.setAccountStatus(AccountStatus.SIGNED_OUT)
            generalViewModel.setAccountStatus(AccountStatus.SIGNED_OUT)
            levelViewModel.currentlySignedInPlayerName = "Unknown"
            return
        }

        // Enable Play Games Services integration
        signInToFirebaseWithCredential(gamesSignInClient)
    }

    private fun setPlayerNameAndSignedInStatus(accountStatus: AccountStatus)
    {
        PlayGames.getPlayersClient(this).currentPlayer.addOnCompleteListener { mTask: Task<Player> ->
            levelViewModel.currentlySignedInPlayerName = mTask.result.displayName
            levelViewModel.setAccountStatus(accountStatus)
            generalViewModel.setAccountStatus(accountStatus)
            levelViewModel.setUiStateLoading()
        }
    }

    private fun collectPlayerLevels() {
        lifecycleScope.launch {
            lifecycleScope.launchWhenCreated {
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
                    if (multipleLevels == -1) {
                        supportFragmentManager.commit {
                            levelViewModel.setUiStateWhenOnPause()
                            levelViewModel.changeLevel(lastAddedLevel)
                        }
                    } else {
                        supportFragmentManager.commit {
                            addToBackStack(PlayerLevelSelectFragment().tag)
                            add(PlayerLevelSelectFragment(), PlayerLevelSelectFragment().tag)
                        }
                    }
                }
                .setTextColor(
                    ContextCompat.getColor(
                        applicationContext,
                        R.color.playNewLevelTextColor
                    )
                )
                .setAnimationMode(ANIMATION_MODE_SLIDE)
                .setActionTextColor(
                    ContextCompat.getColor(
                        applicationContext,
                        R.color.playNewLevelColor
                    )
                )
        val params = snack.view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP
        params.topMargin = resources.getDimension(R.dimen._16sdp).toInt()
        snack.view.layoutParams = params
        snack.view.background =
            ContextCompat.getDrawable(applicationContext, R.drawable.new_level_message_rectangle)
        snack.show()
    }

    private fun signInToFirebaseWithCredential(gamesSignInClient: GamesSignInClient) {
        gamesSignInClient
            .requestServerSideAccess(getString(R.string.oauth_game_server_client_id), false)
            .addOnCompleteListener { authTokenTask: Task<String?> ->
                if (authTokenTask.isSuccessful) {
                    authTokenTask.result?.let { serverAuthToken ->
                        val credential = PlayGamesAuthProvider.getCredential(serverAuthToken)
                        auth.signInWithCredential(credential).addOnSuccessListener { authResult ->
                            setPlayerNameAndSignedInStatus(AccountStatus.SIGNED_IN_WITH_FIREBASE)
                        }.addOnFailureListener {
                            setPlayerNameAndSignedInStatus(AccountStatus.SIGNED_IN)
                            Log.e(TAG, "Were not able to log in to firebase", it)
                        }
                    }
                } else {
                    setPlayerNameAndSignedInStatus(AccountStatus.SIGNED_IN)
                    Log.w(TAG, "Firebase login failure.")
                }
            }
    }


    override fun onResume() {
        super.onResume()
        levelViewModel.setUiStateWhenOnResume()
        generalViewModel.refreshPurchasesTriedRedeem()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        levelViewModel.setUiStateWhenOnPause()
    }

    override fun onStop() {
        levelViewModel.saveGameImmediately()
        super.onStop()
        Log.d(TAG, "onStop")
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
                            snapshotHelper.save(this@MainActivity, event.localSaveGame, event.maxLevelInProgress)
                        }
                        is LevelViewModel.VariousEvent.SubmitScore -> {
                            submitScore(event.score, event.playerLevels)
                        }
                        is LevelViewModel.VariousEvent.UnlockAchievement -> {
                            unlockAchievement(event.achievement)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun collectRewardInfo() {
        binding.watchAdTop.setOnClickListener {
            levelViewModel.setUiStateBusy()
            generalViewModel.showRewardedVideo(this, RewardType.SAD_MODE)
            binding.watchAdTop.visibility = View.GONE
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED)
            {
                launch {
                    generalViewModel.rewardState.distinctUntilChangedBy { it }.collect {
                        if (it == RewardType.SAD_MODE) {
                            levelViewModel.setUiStatePlayWhenBusy()
                            levelViewModel.resetIntervalCount(getString(R.string.sad_mode_interval))
                            executeSadModeEffects(false)
                            generalViewModel.updateRewardState(RewardType.NONE)
                            levelViewModel.logRewardGiven(RewardType.SAD_MODE)
                        } else if (it == RewardType.SAD_AD_FAILED) {
                            levelViewModel.setUiStatePlayWhenBusy()
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

    private fun updateFoldModeInfo() {
        lifecycleScope.launch(Dispatchers.Main) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                WindowInfoTracker.getOrCreate(this@MainActivity)
                    .windowLayoutInfo(this@MainActivity)
                    .collectLatest { newLayoutInfo ->
                        val foldingFeature: FoldingFeature? =
                            newLayoutInfo.displayFeatures.find { it is FoldingFeature } as? FoldingFeature
                        val isBookMode = foldingFeature?.state == FoldingFeature.State.FLAT
                        if (isBookMode) levelViewModel.setBookMode(bookMode = BookMode.BOOK_MODE)
                        else levelViewModel.setBookMode(bookMode = BookMode.NOPE)
                    }
            }
        }
    }

    private fun windowSizeClassUpdater() {
        binding.container.addView(object : View(this) {
            override fun onConfigurationChanged(newConfig: Configuration?) {
                super.onConfigurationChanged(newConfig)
                computeWindowSizeClasses()
            }
        })
        computeWindowSizeClasses()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Log.d(TAG, "onConfigurationChanged")
        super.onConfigurationChanged(newConfig)
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
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        window.decorView.setOnApplyWindowInsetsListener { view, windowInsets ->
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            view.onApplyWindowInsets(windowInsets)
        }
    }


    private fun computeWindowSizeClasses() {
        Log.d(TAG, "computeWindowSizeClasses")
        val metrics = WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(this)

        val widthDp = metrics.bounds.width() /
                resources.displayMetrics.density
        val widthWindowSizeClass = when {
            widthDp < 600f -> WindowSizeClass.COMPACT
            widthDp < 840f -> WindowSizeClass.MEDIUM
            else -> WindowSizeClass.EXPANDED
        }
        levelViewModel.setWindowSizeClass(widthWindowSizeClass)
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

    private fun progressBarUpdater() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                levelViewModel.currentLevel.distinctUntilChangedBy { it.levelInfo.levelId }
                    .collect {
                        if (it.levelInfo.levelId < 1000) {
                            setLevelProgressIndicator(it.levelInfo, it.solutions.size)
                        } else if (it.levelInfo.levelId > 1000) {
                            val maxLevels = levelViewModel.getMaxLevelId() - 1000
                            setLevelProgressIndicatorPlayerPack(
                                it.levelInfo,
                                it.solutions.size,
                                maxLevels
                            )
                        }
                    }
            }
        }
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

    private fun setLevelProgressIndicatorPlayerPack(
        level: LevelData,
        solutionsCount: Int,
        maxLevels: Int
    ) {
        binding.levelNumber.text =
            if (solutionsCount > 1) "${level.levelId - 1000}* - ${level.author}" else "${level.levelId - 1000} - ${level.author}"
        binding.gameProgress.progress =
            ((level.levelId - 1000).toFloat() / maxLevels.toFloat() * 100.0).toInt()
    }

    private fun setLevelProgressIndicator(level: LevelData, solutionsCount: Int) {
        val levelsCount = 100
        binding.levelNumber.text =
            if (solutionsCount > 1) "${level.levelId}*" else "${level.levelId}"
        binding.gameProgress.progress =
            (level.levelId.toFloat() / levelsCount.toFloat() * 100.0).toInt()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed")
        levelViewModel.saveGameImmediately()
        val exitDialog = ExitGameDialogFragment()
        exitDialog.show(supportFragmentManager, C.EXIT_GAME_DIALOG_FRAGMENT)
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        finishAndRemoveTask()
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        dialog.dismiss()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

}