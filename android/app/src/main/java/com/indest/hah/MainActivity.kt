package com.indest.hah

import android.content.res.Configuration
import android.os.Bundle
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
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.BaseTransientBottomBar.ANIMATION_MODE_SLIDE
import com.google.android.material.snackbar.Snackbar
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.firestore.*
import com.indest.hah.ads.RewardType
import com.indest.hah.databinding.ActivityMainBinding
import com.indest.hah.fragments.ExitGameDialogFragment
import com.indest.hah.fragments.LevelFragment
import com.indest.hah.fragments.LevelSelectFragment
import com.indest.hah.fx.SoundPlayer
import com.indest.hah.game.creation.BookMode
import com.indest.hah.game.creation.WindowSizeClass
import com.indest.hah.game.viewmodel.ContainerState
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.general.GeneralViewModel
import com.indest.hah.general.SignInLiveData
import com.indest.hah.other.C
import com.indest.hah.other.hideSystemBars
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.persistance.dto.PlayerLevel
import com.indest.hah.views.ButtonState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
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
/*    private val fsDb = FirebaseFirestore.getInstance()
    private var liveStoreRegistration: ListenerRegistration? = null*/

    @Inject
    lateinit var signInObserver: SignInLiveData

    @Inject
    lateinit var soundPlayer: SoundPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        levelViewModel.setWindowSizeClass(WindowSizeClass.NOT_SET)
        levelViewModel.setBookMode(BookMode.NOT_SET)
        updateFoldModeInfo()

        lifecycle.addObserver(signInObserver)
        signInObserver.observe(this) {
            it?.let { accountStatus ->
                levelViewModel.setAccountStatus(accountStatus)
                generalViewModel.setAccountStatus(accountStatus)
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
        requestLatestConsentInformation()
        collectPlayerLevels()

/*
        val db = FirebaseFirestore.getInstance()
        val doc = db.collection("user_submitted_levels")

        db.runTransaction { transaction ->
            val snap = transaction.ge
        }
        val lastUserSubmission = db.collection("user_submitted_levels")
            .whereEqualTo("user_name", "Mina")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
        val lastTimeStamp = lastUserSubmission.re

        db.runTransaction { transaction ->
            val snapshot = transaction.get(sfDocRef)

            // Note: this could be done without a transaction
            //       by updating the population using FieldValue.increment()
            val newPopulation = snapshot.getDouble("population")!! + 1
            transaction.update(sfDocRef, "population", newPopulation)

            // Success
            null
        }.addOnSuccessListener { Log.d(TAG, "Transaction success!") }
            .addOnFailureListener { e -> Log.w(TAG, "Transaction failure.", e) }


        db.collection("user_submitted_levels")
            .whereEqualTo("user_name", "Mina")
            .whereLessThanOrEqualTo("timestamp",)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    Log.d(TAG, "${document.id} => ${document.data}")
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }

       db.collection("user_submitted_levels")
            .whereEqualTo("is_featured", true)
            .addSnapshotListener(EventListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@EventListener
                }
                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            showNewLevelMessage(dc.document.data.get("user_name").toString())
                            Log.d(TAG, "New featured level: " + dc.document.data)
                        }
                        DocumentChange.Type.MODIFIED -> Log.d(TAG, "Modified level: " + dc.document.data)
                        DocumentChange.Type.REMOVED -> Log.d(TAG, "Removed level: " + dc.document.data)
                    }
                }
            })*/

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

/*    private fun collectPlayerLevels() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED)
            {
                liveStoreRegistration?.remove()
                val maxLevelId = levelViewModel.getMaxLevelId()
                val query = fsDb.collection("user_submitted_levels")
                    .whereEqualTo("isFeatured", true)
                    .whereGreaterThan("levelId", maxLevelId)

                liveStoreRegistration = query.addSnapshotListener(EventListener { snapshots, e ->
                    if (e != null) {
                        Log.w(TAG, "listen:error", e)
                        return@EventListener
                    }
                    val playerLevels = mutableListOf<PlayerLevel>()
                    snapshots?.let { snap ->
                        for (dc in snap.documentChanges) {
                            when (dc.type) {
                                DocumentChange.Type.ADDED -> {
                                    try {
                                        dc.document.data.let { levelValues ->
                                            val solutionsToCategories =
                                                levelValues["solutionToCategory"] as Map<String, String>
                                            if (solutionsToCategories.isEmpty()) throw Exception("Solutions were empty")
                                            playerLevels.add(
                                                PlayerLevel(
                                                    levelValues["levelId"] as Long,
                                                    levelValues["player"] as String,
                                                    solutionsToCategories
                                                )
                                            )
                                        }
                                    } catch (e: Exception) {
                                        Log.w(
                                            TAG,
                                            "Something went wrong when parsing the user level from FB.",
                                            e
                                        )
                                        return@EventListener
                                    }
                                }
                                DocumentChange.Type.MODIFIED -> Log.d(
                                    TAG,
                                    "Modified level: " + dc.document.data
                                )
                                DocumentChange.Type.REMOVED -> Log.d(
                                    TAG,
                                    "Removed level: " + dc.document.data
                                )
                            }
                        }
                    }
                    Log.d(TAG, "New featured levels: $playerLevels")
                    lifecycleScope.launchWhenCreated {
                        val levelAndCount = levelViewModel.insertPlayerLevels(playerLevels)
                        if (levelAndCount.second == 1) {
                            showNewLevelMessage(
                                levelAndCount.first.author,
                                levelAndCount.first.levelId
                            )
                        } else if (levelAndCount.second > 1) {
                            showNewLevelMessage(
                                levelAndCount.first.author,
                                lastAddedLevel = -1,
                                multipleLevels = levelAndCount.second
                            )
                        }
                    }
                })
            }
        }
    }*/

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
                            addToBackStack(LevelSelectFragment().tag)
                            add(LevelSelectFragment(), LevelSelectFragment().tag)
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

    override fun onResume() {
        super.onResume()
        levelViewModel.setUiStateWhenOnResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        levelViewModel.stopSavingGame()
        levelViewModel.setUiStateWhenOnPause()
    }

    override fun onStop() {
        super.onStop()
        levelViewModel.saveGameImmediately()
        Log.d(TAG, "onStop")
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
        val exitDialog = ExitGameDialogFragment()
        exitDialog.show(supportFragmentManager, C.EXIT_GAME_DIALOG_FRAGMENT)
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        levelViewModel.saveGameImmediately()
        //liveStoreRegistration?.remove()
        finishAndRemoveTask()
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        dialog.dismiss()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        //liveStoreRegistration?.remove()
    }

}