package com.indest.hah

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Rect
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
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.Insets
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.forEach
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.games.AuthenticationResult
import com.google.android.gms.games.GamesSignInClient
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import com.google.android.gms.games.Player
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.BaseTransientBottomBar.ANIMATION_MODE_FADE
import com.google.android.material.snackbar.Snackbar
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PlayGamesAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import com.indest.hah.push.StagenatorPush
import com.indest.hah.ads.RewardType
import com.indest.hah.databinding.ActivityMainBinding
import com.indest.hah.fragments.CompetitionFragment
import com.indest.hah.fragments.PrizeFragment
import com.indest.hah.fx.SoundPlayer
import com.indest.hah.game.viewmodel.CompetitionAnnouncement
import com.indest.hah.game.viewmodel.FirebaseCustom
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.general.AccountStatus
import com.indest.hah.general.GeneralViewModel
import com.indest.hah.other.C
import com.indest.hah.other.countryCodeToEmoji
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
    val generalViewModel: GeneralViewModel by viewModels()
    private var consentInformation: ConsentInformation? = null
    private var consentForm: ConsentForm? = null
    // lateinit var adView:AdView

    // how long before trying reconnect to firebase
    private var reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS
    private val handler = Handler(Looper.getMainLooper())

    @Inject
    lateinit var soundPlayer: SoundPlayer

    @Inject
    lateinit var snapshotHelper: SnapshotHelper
    private lateinit var auth: FirebaseAuth

    companion object {
        const val PREFERENCE_NAME = "shared_preference"
        const val PREFERENCE_MODE = Context.MODE_PRIVATE
        const val FIRST_TIME_MIGRATION = "first_time_migration"
        const val SELECTED_LANGUAGE = "selected_language"
        const val STATUS_DONE = "status_done"
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        StagenatorPush.handleClaimIntent(this, intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StagenatorPush.handleClaimIntent(this, intent)
        if (auth.currentUser != null) StagenatorPush.register(this)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 7101)
        }
        // Check if the migration has already been done or not
        if (getString(FIRST_TIME_MIGRATION) != STATUS_DONE) {
            // Fetch the selected language from wherever it was stored. In this case its SharedPref
            getString(SELECTED_LANGUAGE)?.let {
                // Set this locale using the AndroidX library that will handle the storage itself
                val localeList = LocaleListCompat.forLanguageTags(it)
                AppCompatDelegate.setApplicationLocales(localeList)
                // Set the migration flag to ensure that this is executed only once
                putString(FIRST_TIME_MIGRATION, STATUS_DONE)
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        levelViewModel.setAccountStatus(AccountStatus.LOADING)

        val gamesSignInClient = PlayGames.getGamesSignInClient(this)
        gamesSignInClient.isAuthenticated.addOnCompleteListener { isAuthenticatedTask: Task<AuthenticationResult> ->
            handleAuthTask(gamesSignInClient, isAuthenticatedTask)
        }
        requestLatestConsentInformation()
        generalViewModel.toString()
        applyCustomWindowInsets()
        setUpNavController()
        collectVariousEvents()
        collectPlayerLevelsForNotification()
        collectNewLevelCount()
        levelViewModel.downloadPromos()

        binding.root.doOnNextLayout {
            val exclusions = listOf(Rect(0, 0, it.width, it.height))
            ViewCompat.setSystemGestureExclusionRects(binding.root, exclusions)
        }
        binding.levelFlag.setOnLongClickListener {
            levelViewModel.resetLevel()
            true
        }
    }

    override fun onStop() {
        super.onStop()
        levelViewModel.saveGameImmediately()
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
            levelViewModel.currentlySignedInPlayerName = "???"
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
                levelViewModel.newPlayerLevels.collectLatest { newLevel ->
                    if (newLevel.lastAddedLevel == -1 && newLevel.multipleLevels == -1 && newLevel.playerName == "") return@collectLatest
                    if (newLevel.multipleLevels == 1) {
                        showNewLevelMessage(
                            newLevel.playerName,
                            newLevel.lastAddedLevel,
                            countryCode = newLevel.countryCode,
                            showPlayButton = newLevel.showPlayButton
                        )
                    } else if (newLevel.multipleLevels > 1) {
                        showNewLevelMessage(
                            newLevel.playerName,
                            lastAddedLevel = -1,
                            multipleLevels = newLevel.multipleLevels,
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
        multipleLevels: Int = -1,
        countryCode: String = "",
        showPlayButton: Boolean = true
    ) {
        val flag = if (countryCode != "") {
            "${countryCode.countryCodeToEmoji()} "
        } else ""
        val snackMessage =
            if (multipleLevels == -1) {
                if (!showPlayButton) {
                    "$flag${
                        resources.getString(
                            R.string.new_level_by_language_pack_incomplete,
                            player
                        )
                    }"
                } else {
                    "$flag${resources.getString(R.string.new_level_by, player)}"
                }
            } else {
                "$multipleLevels ${resources.getString(R.string.levels_added)}"
            }

        val snack = if (multipleLevels == -1 && !showPlayButton) {
            Snackbar.make(findViewById(android.R.id.content), snackMessage, 15000)
                .setAnimationMode(ANIMATION_MODE_FADE)
        } else {
            Snackbar.make(findViewById(android.R.id.content), snackMessage, 15000)
                .setAction(resources.getString(R.string.play)) {
                    val navHostFragment =
                        supportFragmentManager.findFragmentById(R.id.fragments_container) as NavHostFragment
                    val navController = navHostFragment.navController

                    // Check if there is only one new level
                    if (multipleLevels == -1) {
                        levelViewModel.changeLevel(lastAddedLevel)
                    } else { // If there are multiple new levels
                        navController.safeNavigate(R.id.action_navigation_level_to_navigation_stages)
                    }
                }
                .setAnimationMode(ANIMATION_MODE_FADE)
        }
        val params = snack.view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP
        snack.view.layoutParams = params
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
                            StagenatorPush.register(this)
                            lifecycleScope.launch {
                                lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED)
                                {
                                    val isReadyForCompetition =
                                        levelViewModel.isEnrollingToCompetition()
                                    val currentInfo =
                                        levelViewModel.getCompetitionStatus()
                                    if (!currentInfo.show && !currentInfo.triedToParticipate) {
                                        if (isReadyForCompetition) {
                                            levelViewModel.setCompetitionStatus(
                                                currentInfo.copy(
                                                    triedToParticipate = true
                                                )
                                            )
                                            tryToParticipateInCompetition()
                                        }
                                    }
                                }
                            }
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

    private fun tryToParticipateInCompetition() {
        try {
            auth.currentUser?.let { user ->
                val playerName = user.displayName
                val uid = user.uid
                val db = Firebase.firestore
                val docRef = db.collection("competitions/reach_level/participants/").document(uid)
                val competitionRef = db.collection("competitions/").document("reach_level")
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    val snapshotComp = transaction.get(competitionRef)
                    val prizesLeft = snapshotComp.getLong("prizes_left")!!
                    val finished = snapshot.getBoolean("finished")
                    val promoLink = snapshot.getString("promo_link")
                    if (prizesLeft <= 0) {
                        CompetitionAnnouncement()
                    } else if (snapshot.exists() && finished == false) {
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
                    if (result.show && result.prizesLeft > 0) {
                        supportFragmentManager.commit {
                            addToBackStack(CompetitionFragment().tag)
                            add(CompetitionFragment(), CompetitionFragment().tag)
                        }
                    }
                    levelViewModel.setCompetitionStatus(result)
                }.addOnFailureListener { e ->
                    levelViewModel.setCompetitionStatus(CompetitionAnnouncement())
                    Log.w(TAG, "Transaction failure - not added to competition", e)
                }
            }
        } catch (_: Exception) { }
    }

    private fun tryToObtainPromoCode() {
        val user = auth.currentUser
        val db = Firebase.firestore
        user?.let {
            val uid = user.uid
            val playerRef = db.collection("competitions/reach_level/participants/").document(uid)
            playerRef.get().addOnSuccessListener { playerDocSnap ->
                if (playerDocSnap.getBoolean("finished") == false) {
                    db.collection("competitions/reach_level/promo_codes/").limit(1)
                        .get()
                        .addOnSuccessListener {
                            if (it.size() == 1) {
                                val promoLink = it.documents[0]["promo_link"]
                                val promoCodeRef =
                                    db.collection("competitions/reach_level/promo_codes/")
                                        .document(it.documents[0].id)
                                val competitionRef =
                                    db.collection("competitions/").document("reach_level")

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
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Redeem link", promoCode)
        clipboard.setPrimaryClip(clip)
        levelViewModel.logCompetitionRelatedEvent(FirebaseCustom.Events.COMPETITION_20_PRIZE)
        supportFragmentManager.commit {
            addToBackStack(PrizeFragment(promoCode).tag)
            add(PrizeFragment(promoCode), PrizeFragment(promoCode).tag)
        }
    }

    private fun announceEndWithoutReward() {
        levelViewModel.setCompetitionStatus(
            levelViewModel.getCompetitionStatus().copy(competitionEnded = true)
        )
        levelViewModel.logCompetitionRelatedEvent(FirebaseCustom.Events.COMPETITION_20_FINISHED)
        supportFragmentManager.commit {
            addToBackStack(PrizeFragment().tag)
            add(PrizeFragment(), PrizeFragment().tag)
        }
    }

    /**
     * Retries the billing service connection with exponential backoff, maxing out at the time
     * specified by RECONNECT_TIMER_MAX_TIME_MILLISECONDS.
     */
    private fun retryFirebaseSignInWithExponentialBackoff(gamesSignInClient: GamesSignInClient) {
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


    override fun onStart() {
        super.onStart()
        val prefs = getSharedPreferences("app_open_prefs", Context.MODE_PRIVATE)
        val count = prefs.getInt("open_count", 0) + 1
        prefs.edit().putInt("open_count", count).apply()
    }

    override fun onResume() {
        super.onResume()
        generalViewModel.refreshPurchasesTriedRedeem()
    }


    private fun submitScore(
        score: Long,
        playerLevels: Boolean = false,
        languageLevels: Boolean = false,
        countryCode: String
    ) {
        val leaderboardId = if (playerLevels) {
            getString(R.string.leaderboard_player_levels_leaderboard)
        } else if (languageLevels) {
            when (countryCode) {
                "cn" -> getString(R.string.leaderboard_chinese)
                "de" -> getString(R.string.leaderboard_german)
                "es" -> getString(R.string.leaderboard_spanish)
                "et" -> getString(R.string.leaderboard_estonian)
                "fi" -> getString(R.string.leaderboard_finnish)
                "fr" -> getString(R.string.leaderboard_french)
                "hi" -> getString(R.string.leaderboard_hindi)
                "id" -> getString(R.string.leaderboard_indonesian)
                "it" -> getString(R.string.leaderboard_italian)
                "ja" -> getString(R.string.leaderboard_japanese)
                "ko" -> getString(R.string.leaderboard_korean)
                "nl" -> getString(R.string.leaderboard_dutch)
                "pl" -> getString(R.string.leaderboard_polish)
                "pt" -> getString(R.string.leaderboard_portuguese)
                "ro" -> getString(R.string.leaderboard_romanian)
                "ru" -> getString(R.string.leaderboard_russian)
                "sv" -> getString(R.string.leaderboard_swedish)
                "tr" -> getString(R.string.leaderboard_turkish)
                "uk" -> getString(R.string.leaderboard_ukrainian)
                else -> {
                    ""
                }
            }
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

                        is LevelViewModel.VariousEvent.SyncedWithCloud -> {
                            showSyncedWithCloudToast()
                        }

                        is LevelViewModel.VariousEvent.SaveGame -> {
                            snapshotHelper.save(
                                this@MainActivity,
                                event.localSaveGame,
                                event.calculatedGameProgress
                            )
                        }

                        is LevelViewModel.VariousEvent.SubmitScore -> {
                            val currentInfo =
                                levelViewModel.getCompetitionStatus()
                            if (!currentInfo.competitionEnded) {
                                if (levelViewModel.currentRelativeLevelNumber == 19 && !currentInfo.triedToOfferPrize) {
                                    levelViewModel.setCompetitionStatus(
                                        currentInfo.copy(
                                            triedToOfferPrize = true
                                        )
                                    )
                                    tryToObtainPromoCode()
                                }
                            }
                            submitScore(
                                event.score,
                                event.playerLevels,
                                event.languageLevels,
                                event.countryCode
                            )
                        }

                        is LevelViewModel.VariousEvent.UnlockAchievement -> {
                            unlockAchievement(event.achievement)
                        }

                        LevelViewModel.VariousEvent.DownloadPromoPicture -> {
                            downloadPromoPicture()
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun collectNewLevelCount() {

        /* TODO: Need to rewrite the VM once the notifications system in place

          lifecycleScope.launch {
              lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED)
              {
                  levelViewModel.countOfNewPlayerLevelsAndLanguageLevels().collect {
                      showNewLevelsCount(it.second + it.first)
                  }
              }
          }*/
    }

    private fun showSyncedWithCloudToast() {
        val text = getString(R.string.synced_with_cloud)
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
        hideSystemBars(binding.container)
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

    fun updateProgressBar(progress: Pair<Int, String>, countryCode: String) {
        binding.levelNumber.text = progress.second
        val drawableId = countryCode.mapCountryCodeToLanguageAndFlag(applicationContext).second
        binding.levelFlag.setImageResource(drawableId)
        binding.levelFlag.invalidate()
        binding.gameProgress.progress = progress.first
    }


    fun downloadHintPictureIfNeeded(levelNr: Int) {
        // let's assume each time we don't have the picture
        levelViewModel.setPictureDownloaded(isPresent = false)

        if (checkIfHintExistsForLevel(levelNr)) {
            levelViewModel.setPictureDownloaded(isPresent = true)
        } else {
            val storage = Firebase.storage
            val gsReference =
                storage.getReferenceFromUrl("${C.FIREBASE_PICS_STORAGE_PATH}level${levelNr}.jpg")
            this.filesDir?.let {
                val localFile = File(it, "level${levelNr}.jpg")
                gsReference.getFile(localFile).addOnSuccessListener {
                    levelViewModel.setPictureDownloaded(isPresent = true)
                }.addOnFailureListener { ex ->
                    levelViewModel.setPictureDownloaded(isPresent = false)
                    Log.e(TAG, "Failed to create local file from storage.", ex)
                }
            }
        }
    }

    private fun downloadPromoPicture() {
        levelViewModel.promoDialogData?.picturePath?.let { picturePath ->
            val storage = Firebase.storage
            val gsReference = storage.getReferenceFromUrl(picturePath)
            this.filesDir?.let {
                val localFile = File(it, "promo.jpg")
                gsReference.getFile(localFile).addOnSuccessListener {
                }.addOnFailureListener { ex ->
                    Log.e(TAG, "Failed to create local file from storage.", ex)
                }
            }
        }
    }

    private fun checkIfHintExistsForLevel(levelNr: Int): Boolean {
        if (levelNr <= C.PLAYER_LEVELS_COVERED_LOCALLY || levelNr in C.LANGUAGE_LEVELS_RANGE) return true
        val outputDir = this.filesDir
        outputDir?.let {
            val localFile = File(it, "level$levelNr.jpg")
            return localFile.exists()
        }
        return false
    }

    private fun putString(key: String, value: String) {
        val editor = getSharedPreferences(PREFERENCE_NAME, PREFERENCE_MODE).edit()
        editor.putString(key, value)
        editor.apply()
    }

    private fun getString(key: String): String? {
        val preference = getSharedPreferences(PREFERENCE_NAME, PREFERENCE_MODE)
        return preference.getString(key, null)
    }

    private fun showNewLevelsCount(newLevelCount: Int) {
        val levelsBadge = binding.root.findViewById<BottomNavigationView>(R.id.bottomNavView)
            .getOrCreateBadge(R.id.navigation_stages)
        if (newLevelCount > 0) {
            levelsBadge.isVisible = true
            levelsBadge.number = newLevelCount
        } else {
            levelsBadge.isVisible = false
        }
    }

}
