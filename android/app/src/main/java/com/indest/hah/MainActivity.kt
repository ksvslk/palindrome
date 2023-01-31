package com.indest.hah

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.PersistableBundle
import android.text.TextUtils.replace
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
import androidx.core.view.forEach
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.commit
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowMetricsCalculator
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkRequest
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.*
import com.indest.hah.ads.RewardType
import com.indest.hah.databinding.ActivityMainBinding
import com.indest.hah.fragments.*
import com.indest.hah.fx.SoundPlayer
import com.indest.hah.game.creation.BookMode
import com.indest.hah.game.creation.WindowSizeClass
import com.indest.hah.game.viewmodel.ContainerState
import com.indest.hah.game.viewmodel.FirebaseCustom
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.general.AccountStatus
import com.indest.hah.general.CompetitionAnnouncement
import com.indest.hah.general.GeneralViewModel
import com.indest.hah.other.C
import com.indest.hah.other.hideSystemBars
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.persistance.dto.SaveGame
import com.indest.hah.persistance.level.LevelProgress
import com.indest.hah.savegame.SnapshotHelper
import com.indest.hah.persistance.dto.PlayerLevel
import com.indest.hah.views.ButtonState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

        val gamesSignInClient = PlayGames.getGamesSignInClient(this)
        gamesSignInClient.isAuthenticated.addOnCompleteListener { isAuthenticatedTask: Task<AuthenticationResult> ->
            Log.d(TAG, "addOnCompleteListener")

            if (!isAuthenticatedTask.isSuccessful) {
                // Disable Play Games Services integration
                levelViewModel.setAccountStatus(AccountStatus.SIGNED_OUT)
                generalViewModel.setAccountStatus(AccountStatus.SIGNED_OUT)
                Log.d(TAG, "SIGNED_OUT")
                levelViewModel.currentlySignedInPlayerName = "Unknown"
                return@addOnCompleteListener
            }

            val authenticationResult = isAuthenticatedTask.result
            if (!authenticationResult.isAuthenticated) {
                // Disable Play Games Services integration
                levelViewModel.setAccountStatus(AccountStatus.SIGNED_OUT)
                generalViewModel.setAccountStatus(AccountStatus.SIGNED_OUT)
                Log.d(TAG, "SIGNED_OUT")
                levelViewModel.currentlySignedInPlayerName = "Unknown"
                return@addOnCompleteListener
            }

            // Enable Play Games Services integration
            Log.d(TAG, "SIGNED_IN")
            signInToFirebaseWithCredential(gamesSignInClient)
            levelViewModel.setAccountStatus(AccountStatus.SIGNED_IN)
            generalViewModel.setAccountStatus(AccountStatus.SIGNED_IN)

            PlayGames.getPlayersClient(this).currentPlayer.addOnCompleteListener { mTask: Task<Player> ->
                levelViewModel.currentlySignedInPlayerName = mTask.result.displayName
            }
        }

        MobileAds.initialize(applicationContext) {
        }

        generalViewModel.toString()
        applyCustomWindowInsets()
        windowSizeClassUpdater()
        setUpNavController()
        progressBarUpdater()
        collectRewardInfo()
        collectVariousEvents()
        requestLatestConsentInformation()
        collectPlayerLevels()
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
        Log.d(TAG, "signInToFirebaseWithCredential")
        gamesSignInClient
            .requestServerSideAccess(getString(R.string.oauth_game_server_client_id), false)
            .addOnCompleteListener { authTokenTask: Task<String?> ->
                if (authTokenTask.isSuccessful) {
                    Log.d(TAG, "authTokenTask: isSuccessful")
                    authTokenTask.result?.let { serverAuthToken ->
                        val credential = PlayGamesAuthProvider.getCredential(serverAuthToken)
                        auth.signInWithCredential(credential).addOnSuccessListener {
                            Log.d(TAG, "UID: ${it.user?.uid}")
                            Log.d(TAG, "Signed in to firebase")
                            lifecycleScope.launchWhenCreated {
                                lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                                    Log.d(TAG, "Start collecting")
                                    levelViewModel.uiState.collect { uiState ->
                                        var maxLevel: Int? = null
                                        val currentLevel = uiState.level.levelInfo.levelId
                                        levelViewModel.allLevelsFlow().collect { levels ->
                                            maxLevel =
                                                levels.firstOrNull { it.second.progress == LevelProgress.TODO }?.second?.levelId
                                        }
                                        val currentInfo = levelViewModel.getCompetitionStatus()
                                        if (uiState.state == ContainerState.PLAY) {
                                            if (!currentInfo.show && !currentInfo.triedToParticipate) {
                                                if (currentLevel == 1 && maxLevel == 2) {
                                                    levelViewModel.setCompetitionStatus(
                                                        currentInfo.copy(
                                                            triedToParticipate = true
                                                        )
                                                    )
                                                    tryToParticipateInCompetition()
                                                }
                                            }
                                            if (!currentInfo.competitionEnded) {
                                                if (currentLevel == 25 && maxLevel == 26 && !currentInfo.triedToOfferPrize) {
                                                    levelViewModel.setCompetitionStatus(
                                                        currentInfo.copy(
                                                            triedToOfferPrize = true
                                                        )
                                                    )
                                                    tryToObtainPromoCode()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "Firebase login failure.")
                }
            }
    }

    private fun tryToParticipateInCompetition() {
        Log.d(TAG, "tryToParticipateInCompetition")
        auth.currentUser?.let { user ->
            Log.d(TAG, "user:exists")
            val playerName = user.displayName
            val uid = user.uid
            Log.d(TAG, "Current user: $playerName and uid: $uid")

            val db = Firebase.firestore
            val docRef = db.collection("competitions/reach_25_1/participants/").document(uid)
            val competitionRef = db.collection("competitions/").document("reach_25_1")
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val snapshotComp = transaction.get(competitionRef)
                val prizesLeft = snapshotComp.getLong("prizes_left")!!
                val finished = snapshot.getBoolean("finished")
                val promoLink = snapshot.getString("promo_link")

                if (prizesLeft <= 0) {
                    CompetitionAnnouncement()
                } else if (snapshot.exists() && finished == false) {
                    showCompetitionToast(prizesLeft)
                    CompetitionAnnouncement(show = true, prizesLeft)
                } else if (snapshot.exists() && promoLink?.isNotEmpty() == true) {
                    CompetitionAnnouncement()
                } else {
                    val data = hashMapOf(
                        "finished" to false,
                        "player_name" to playerName,
                        "promo_link" to ""
                    )
                    transaction.set(docRef, data)
                    CompetitionAnnouncement(show = true, prizesLeft)
                }
            }.addOnSuccessListener { result ->
                Log.d(TAG, "Transaction completed successfully. State is $result.")
                levelViewModel.setCompetitionStatus(result)
            }.addOnFailureListener { e ->
                levelViewModel.setCompetitionStatus(CompetitionAnnouncement())
                Log.w(TAG, "Transaction failure - not added to competition", e)
            }
        }
    }

    private fun showCompetitionToast(prizesLeft: Long) {
        val toast = Toast.makeText(
            applicationContext,
            "Reach level 25 to win Instant Clues! Only $prizesLeft prizes left!",
            Toast.LENGTH_LONG
        )
        toast.show()
        levelViewModel.logCompetitionRelatedEvent(FirebaseCustom.Events.COMPETITION_25_ANNOUNCED)
    }

    private fun tryToObtainPromoCode() {
        Log.d(TAG, "tryToObtainPromoCode")
        val user = auth.currentUser
        val db = Firebase.firestore
        user?.let {
            Log.d(TAG, "got firebase user")
            val uid = user.uid
            val playerRef = db.collection("competitions/reach_25_1/participants/").document(uid)
            playerRef.get().addOnSuccessListener { playerDocSnap ->
                if (playerDocSnap.getBoolean("finished") == false) {
                    Log.d(TAG, "Not finished yet")
                    db.collection("competitions/reach_25_1/promo_codes/").limit(1)
                        .get()
                        .addOnSuccessListener {
                            if (it.size() == 1) {
                                val promoLink = it.documents[0]["promo_link"]
                                Log.d(TAG, "Found promo code: $promoLink")

                                val promoCodeRef =
                                    db.collection("competitions/reach_25_1/promo_codes/")
                                        .document(it.documents[0].id)
                                val competitionRef =
                                    db.collection("competitions/").document("reach_25_1")

                                val playerData = hashMapOf(
                                    "finished" to true,
                                    "promo_link" to promoLink,
                                )
                                db.runBatch { batch ->
                                    batch.delete(promoCodeRef)
                                    batch.update(playerRef, playerData)
                                    batch.update(
                                        competitionRef,
                                        "prizes_left",
                                        FieldValue.increment(-1)
                                    )
                                }.addOnSuccessListener {
                                    Log.d(TAG, "Successfully transferred promo code to player!")
                                    announceEndWithReward(promoCode = promoLink as String)
                                }.addOnFailureListener { ex ->
                                    Log.d(
                                        TAG,
                                        "Something went wrong when transferring promo code to player!",
                                        ex
                                    )
                                    announceEndWithoutReward()
                                }
                            } else {
                                playerRef.update("finished", true)
                                announceEndWithoutReward()
                            }
                        }
                        .addOnFailureListener {
                            Log.d(
                                TAG,
                                "Something went wrong while obtaining promo code! No promo codes left, user already rewarded or not enrolled!"
                            )
                        }
                } else {
                    Log.d(TAG, "Player already finished!")
                }
            }.addOnFailureListener {
                Log.d(TAG, "Did not participate in competition. Do nothing.")
            }
        }
    }

    private fun announceEndWithReward(promoCode: String) {
        levelViewModel.setCompetitionStatus(
            levelViewModel.getCompetitionStatus().copy(competitionEnded = true)
        )
        val redeemLink = "https://play.google.com/redeem?code=$promoCode"
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Redeem link", promoCode)
        clipboard.setPrimaryClip(clip)
        levelViewModel.logCompetitionRelatedEvent(FirebaseCustom.Events.COMPETITION_25_PRIZE)
        supportFragmentManager.commit {
            addToBackStack(PrizeFragment(redeemLink).tag)
            add(PrizeFragment(redeemLink), PrizeFragment(redeemLink).tag)
        }
    }

    private fun announceEndWithoutReward() {
        levelViewModel.setCompetitionStatus(
            levelViewModel.getCompetitionStatus().copy(competitionEnded = true)
        )
        levelViewModel.logCompetitionRelatedEvent(FirebaseCustom.Events.COMPETITION_25_FINISHED)
        supportFragmentManager.commit {
            addToBackStack(PrizeFragment().tag)
            add(PrizeFragment(), PrizeFragment().tag)
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

    private fun submitScore(score: Long, playerLevels:Boolean = false) {
        val leaderboardId = if (playerLevels) {
            getString(R.string.leaderboard_player_levels_leaderboard)
        }
        else {
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
            .setTagForUnderAgeOfConsent(false)
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
                            snapshotHelper.save(this@MainActivity, event.localSaveGame)
                        }
                        is LevelViewModel.VariousEvent.SubmitScore -> {
                            submitScore(event.score)
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
                        if (it.levelInfo.levelId < 1000)
                        {
                            setLevelProgressIndicator(it.levelInfo, it.solutions.size)
                        }
                        else if (it.levelInfo.levelId > 1000) {
                            val maxLevels = levelViewModel.getMaxLevelId() - 1000
                            setLevelProgressIndicatorPlayerPack(it.levelInfo, it.solutions.size, maxLevels)
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

    private fun setLevelProgressIndicatorPlayerPack(level: LevelData, solutionsCount: Int, maxLevels:Int) {
        binding.levelNumber.text =
            if (solutionsCount > 1) "${level.levelId-1000}* - ${level.author}" else "${level.levelId-1000} - ${level.author}"
        binding.gameProgress.progress =
            (level.levelId.toFloat() / maxLevels.toFloat() * 100.0).toInt()
    }

    private fun setLevelProgressIndicator(level: LevelData, solutionsCount: Int) {
        val levelsCount = 100
        binding.levelNumber.text =
            if (solutionsCount > 1) "${level.levelId}*" else "${level.levelId}"
        binding.gameProgress.progress =
            (level.levelId.toFloat() / levelsCount.toFloat() * 100.0).toInt()
    }

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