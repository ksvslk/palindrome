package com.indest.hah.game

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.core.view.drawToBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.games.Games
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.SnapshotsClient.RESOLUTION_POLICY_LAST_KNOWN_GOOD
import com.google.android.gms.games.snapshot.Snapshot
import com.google.android.gms.games.snapshot.SnapshotMetadata
import com.google.android.gms.games.snapshot.SnapshotMetadata.PROGRESS_VALUE_UNKNOWN
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import com.google.android.gms.tasks.Task
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.indest.hah.R
import com.indest.hah.game.enums.*
import com.indest.hah.letter.Cell
import com.indest.hah.letter.Letter
import com.indest.hah.persistance.LevelProgress
import com.indest.hah.persistance.LevelRepository
import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.persistance.dto.Sku
import com.indest.hah.util.C
import com.indest.hah.util.WindowSizeClass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.lang.reflect.Type
import java.util.*
import javax.inject.Inject


@HiltViewModel
class GameViewModel @Inject constructor(
    private val levelRepository: LevelRepository,
    application: Application
) :
    AndroidViewModel(application) {

    private val TAG = "GameViewModel"

    private var currentLevel = Level()
    val levels = MutableLiveData<List<LevelData>>()
    val totalScore = MutableLiveData<Int>()
    val realSolutionAndGuess = MutableLiveData<Triple<String?, String?, Int?>>()
    var windowSizeClassWidth: WindowSizeClass = WindowSizeClass.NOT_SET
    private var billingClient: BillingClient? = null

    private val _rewardedAdAvailable = MutableStateFlow(false)
    val rewardedAdAvailable: StateFlow<Boolean> = _rewardedAdAvailable.asStateFlow()
    private val _userReward = MutableStateFlow(UserRewardEvent.Success())
    val userReward: StateFlow<UserRewardEvent> = _userReward
    private val _loadingState = MutableStateFlow(AppLoadingState())
    val loadingState: StateFlow<AppLoadingState> = _loadingState.asStateFlow()
    private val _internetState = MutableStateFlow(AppInternetState())
    val internetState: StateFlow<AppInternetState> = _internetState.asStateFlow()
    private val _skuDetails = MutableStateFlow(listOf<SkuDetailsWrapper>())
    val skuDetails: StateFlow<List<SkuDetailsWrapper>> = _skuDetails.asStateFlow()

    private val _adReloadTimer = MutableStateFlow(false)
    val adReloadTimer: StateFlow<Boolean> = _adReloadTimer.asStateFlow()
    private val _saveGameTimer = MutableStateFlow(false)
    val saveGameTimer: StateFlow<Boolean> = _adReloadTimer.asStateFlow()

    private fun startSaveGameTimer() {
        object : CountDownTimer(180000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                Log.i(TAG, "Save game timer completed.")
                _saveGameTimer.value = true
                startSaveGameTimer()
            }
        }.start()
    }

    fun startAdReloadTimer() {
        object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                Log.i(TAG, "Ad reload timer completed.")
                _adReloadTimer.value = true
            }
        }.start()
    }

    fun setAdReloadTimer(value: Boolean)
    {
        _adReloadTimer.value = value
    }

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                viewModelScope.launch {
                    for (purchase in purchases) {
                        Log.i(TAG, "Should handle purchase here.")
                        handlePurchase(purchase)
                    }
                }
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                Log.i(TAG, "User cancelled the purchase flow.")
            } else {
                Log.i(TAG, "Billing flow failed with: ${billingResult.responseCode}")
            }

        }

    private val purchaseResponseListener =
        PurchasesResponseListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases.isNotEmpty()) {
                viewModelScope.launch {
                    for (purchase in purchases) {
                        Log.i(
                            TAG,
                            "Handle purchases from PurchasesResponseListener: ${purchase.skus.first()} - ${purchase.purchaseState}"
                        )
                        handlePurchase(purchase)
                    }
                }
            } else {
                Log.i(
                    TAG,
                    "purchaseResponseListener failed with: ${billingResult.responseCode}. Purchases: $purchases"
                )
            }

        }

    init {
        billingClient = BillingClient.newBuilder(application.applicationContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
        billingInitialization(application.applicationContext)
        startSaveGameTimer()
    }

    private fun billingInitialization(context: Context) {
        Log.d(TAG, "Billing Initialization")
        billingClient?.startConnection(object :
            BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    viewModelScope.launch {
                        querySkuDetails()
                        queryPurchasesAsync()
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.i(TAG, "onBillingServiceDisconnected")
                billingInitialization(context)
            }
        })
    }


    suspend fun querySkuDetails() {
        val skuList = levelRepository.getAllSkus().map { it.sku }
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)

        // leverage querySkuDetails Kotlin extension function
        val skuDetailsResult = withContext(Dispatchers.IO) {
            billingClient?.querySkuDetails(params.build())
        }
        if (skuDetailsResult?.billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
            skuDetailsResult.skuDetailsList?.map {
                SkuDetailsWrapper(purchased = false, it)
            }?.let {
                _skuDetails.value = it
            }
        }
    }

    fun queryPurchasesAsync() {
        billingClient?.queryPurchasesAsync(BillingClient.SkuType.INAPP, purchaseResponseListener)
    }

    fun launchPurchaseFlow(skuDetails: SkuDetails, activity: Activity) {
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()
        val responseCode = billingClient?.launchBillingFlow(activity, flowParams)?.responseCode
        Log.i(TAG, "Launch billing flow response code: $responseCode")
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        // Purchase retrieved from BillingClient#queryPurchasesAsync or your PurchasesUpdatedListener.
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                Log.i(TAG, "Purchased: ${purchase.skus.first()}")
                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                    val ackPurchaseResult = withContext(Dispatchers.IO) {
                        billingClient?.acknowledgePurchase(acknowledgePurchaseParams.build())
                    }
                    if (ackPurchaseResult?.responseCode == BillingClient.BillingResponseCode.OK
                    ) {
                        levelRepository.grantSkuEntitlements(purchase.skus.first())
                        _skuDetails.value = skuDetails.value.map {
                            if (it.skuDetails.sku == purchase.skus.first()) {
                                it.copy(purchased = true)
                            } else {
                                it.copy()
                            }
                        }
                    }
                } else {
                    levelRepository.grantSkuEntitlements(purchase.skus.first())
                    _skuDetails.value = skuDetails.value.map {
                        if (it.skuDetails.sku == purchase.skus.first()) {
                            it.copy(purchased = true)
                        } else {
                            it.copy()
                        }
                    }
                }
            }
            Purchase.PurchaseState.PENDING -> {

            }
            else -> {
                levelRepository.removeSkuEntitlements(purchase.skus.first())
                _skuDetails.value = skuDetails.value.map {
                    if (it.skuDetails.sku == purchase.skus.first()) {
                        it.copy(purchased = false)
                    } else {
                        it.copy()
                    }
                }
            }
        }

    }

    private fun changeState(newState: LoadingState) {
        _loadingState.value = AppLoadingState(state = newState)
    }

    fun changeInternetState(newState: InternetState) {
        _internetState.value = AppInternetState(state = newState)
    }

    fun setRewardedAdAvailability(boolean: Boolean) {
        _rewardedAdAvailable.value = boolean
    }

    suspend fun getLatestLevel(): Level {
        currentLevel = levelRepository.getLatestLevel()
        return currentLevel
    }

    fun startGame() {
        viewModelScope.launch {
            levels.value = levelRepository.getAllLevels()
        }
        changeState(LoadingState.READY)
    }

    suspend fun getLevelCount(): Int {
        return levelRepository.getAllLevels().size
    }

    fun rewardUser(rewardType: RewardType) {
        _userReward.value =
            UserRewardEvent.Success(rewardType = rewardType, rewardEarned = true)
    }

    fun resetRewardingUser() {
        _userReward.value =
            UserRewardEvent.Success(rewardType = RewardType.NONE, rewardEarned = false)
    }

    fun changeLevel(nr: Int) {
        viewModelScope.launch {
            levelRepository.updateNextLevelTimestamp(
                currentLevel.levelInfo.levelId,
                nr
            )
        }
    }

    fun getLevelNumber(): Int {
        return currentLevel.levelInfo.levelId
    }

    fun increaseMistakes() {
        viewModelScope.launch {
            val newScore = if (currentLevel.levelInfo.score - 2 < 0) {
                0
            } else {
                currentLevel.levelInfo.score - 2
            }
            levelRepository.updateScore(
                newScore,
                currentLevel.levelInfo.levelId
            )
        }
    }

    fun updateCompletedLevel() {
        viewModelScope.launch {
            updateLevelProgress(LevelProgress.INTERMEDIATE)
        }
    }

    fun unlockNextLevel() {
        viewModelScope.launch {
            levelRepository.resetLevelLettersInfo(currentLevel.levelInfo.levelId)
            levelRepository.unlockNextLevel(currentLevel.levelInfo.levelId)
            updateLevelProgress(LevelProgress.REPLAY)
        }
    }

    private suspend fun updateLevelProgress(progress: LevelProgress) {
        val updatedLevel = currentLevel.levelInfo.copy(
            progress = progress,
            puzzle = currentLevel.levelInfo.puzzle,
            score = currentLevel.levelInfo.score
        )
        levelRepository.updateLevelAndBestScore(
            updatedLevel,
            currentLevel.levelInfo.score
        )
        totalScore.value = levelRepository.getTotalScore()
    }

    fun getCurrentAndMaxScore(): Pair<Int, Int> {
        return Pair(
           currentLevel.levelInfo.score,
           currentLevel.levelInfo.levelId * 100
        )
    }

    fun isLastLevel(): Boolean {
        return currentLevel.levelInfo.levelId == levels.value?.size
    }

    fun updateLetters(topIndex: Int, bottomLetter: Char, bottomIndex: Int, topLetter: Char) {
        Log.i(TAG, "updateLetters")
        viewModelScope.launch {
            levelRepository.updateLetter(
                homeIndex = topIndex,
                letter = bottomLetter,
                level = currentLevel.levelInfo.levelId
            )
            levelRepository.updateLetter(
                homeIndex = bottomIndex,
                letter = topLetter,
                level = currentLevel.levelInfo.levelId
            )

            realSolutionAndGuess.value = Triple(
               currentLevel.levelInfo.solution.replace(" ", ""),
                levelRepository.currentSolution(currentLevel.levelInfo.levelId),
                topIndex
            )
        }
    }

    fun updateFriendIndex(index: Int, newIndex: Int) {
        Log.i(TAG, "updateFriendIndex")
        viewModelScope.launch {
            levelRepository.updateFriendIndex(
                homeIndex = index,
                newFriendIndex = newIndex,
                level = currentLevel.levelInfo.levelId
            )
            realSolutionAndGuess.value = Triple(
               currentLevel.levelInfo.solution.replace(" ", ""),
                levelRepository.currentSolution(currentLevel.levelInfo.levelId),
                index
            )
        }
    }

    fun setLetterLocked(index: Int) {
        Log.i(TAG, "setLetterLocked")
        viewModelScope.launch {
            levelRepository.setLetterLocked(
                homeIndex = index,
                level = currentLevel.levelInfo.levelId
            )
        }
    }

    fun persistInitialLettersInfo(cells: MutableList<Cell<*, *>>) {
        viewModelScope.launch {
            val isAlreadyInitialized =
                levelRepository.isLettersInitialized(currentLevel.levelInfo.levelId)
            if (!isAlreadyInitialized) {
                Log.i(TAG, "initial letters persist")
                cells.filterIsInstance<Letter>().forEachIndexed { _, it ->
                    levelRepository.insertLetterInfo(
                        level = currentLevel.levelInfo.levelId,
                        homeIndex = it.homeIndex,
                        friendIndex = it.friendIndex,
                        letter = it.text.first(),
                        locked = false,
                    )
                }
                val updatedLevel = currentLevel.levelInfo.copy(
                    progress = LevelProgress.IN_PROGRESS,
                    puzzle = currentLevel.levelInfo.puzzle,
                    score = currentLevel.levelInfo.levelId * 100
                )
                levelRepository.updateLevel(updatedLevel)
                levels.value = levelRepository.getAllLevels()
            }
        }
    }

    fun setCategoryRewarded(value: Boolean) {
        viewModelScope.launch {
            levelRepository.updateCategoryRewarded(
                value,
                currentLevel.levelInfo.levelId
            )
        }
    }

    fun getHint(): String {
        return currentLevel.levelInfo.category
    }

    suspend fun isCategoryRewarded(): Boolean {
        return levelRepository.isCategoryRewarded(currentLevel.levelInfo.levelId)
    }

    suspend fun saveSnapshot(activity: Activity, coverImage: Bitmap) {
        _saveGameTimer.value = false
        GoogleSignIn.getLastSignedInAccount(activity.applicationContext)?.let { signInAccount ->
            val snapshotsClient = Games.getSnapshotsClient(
                activity,
                signInAccount
            )
            snapshotsClient.open(C.SAVE_GAME, true, RESOLUTION_POLICY_LAST_KNOWN_GOOD)
                .addOnSuccessListener {
                    it.data?.let { snapshotToWrite ->
                        try {
                            val bytes = snapshotToWrite.snapshotContents.readFully()
                            viewModelScope.launch {
                                writeSnapshot(
                                    snapshotToWrite,
                                    snapshotsClient,
                                    coverImage,
                                    bytes
                                )?.addOnSuccessListener {
                                    Log.d(TAG, "Snapshot was written successfully!")
                                }?.addOnFailureListener { ex ->
                                    Log.e(TAG, "Could not write snapshot: ${ex.message}")
                                }
                            }
                        } catch (e: IOException) {
                            Log.e(TAG, "Error getting bytes from snapshot.: ${e.message}")
                        }
                    }
                }.addOnFailureListener {
                    Log.e(TAG, "Could not open snapshot: ${it.message}")
                }
        }
    }

    private suspend fun writeSnapshot(
        snapshot: Snapshot,
        snapshotsClient: SnapshotsClient,
        coverImage: Bitmap,
        bytes: ByteArray
    ): Task<SnapshotMetadata>? {

        val gson = Gson()
        val allLevels = levelRepository.getSaveGameData()
        val levelsData = gson.toJson(allLevels).toByteArray()
        val currentProgress =
            (allLevels.findLast { it.levelInfo.progress > LevelProgress.TODO }?.levelInfo?.levelId
                ?: 1)
        val desc = "Level progress $currentProgress. Saved at: " + Calendar.getInstance().time

        val levelsListType: Type = object : TypeToken<ArrayList<Level?>?>() {}.type
        val levelsList: List<Level>? = gson.fromJson(bytes.decodeToString(), levelsListType)

        if (levelsList != null) {
            val currentGameModifiedTime = allLevels.maxOf { it.levelInfo.timeModified }
            val snapshotGameModifiedTime = levelsList.maxOf { it.levelInfo.timeModified }

            if ((snapshot.metadata.progressValue < currentProgress)) {
                Log.i(TAG, "Current progress is greater. Updating snapshot.")
                snapshot.snapshotContents.writeBytes(levelsData)
                val metadataChange = SnapshotMetadataChange.Builder()
                    .setCoverImage(coverImage)
                    .setDescription(desc)
                    .setProgressValue(currentProgress.toLong())
                    .build()
                return snapshotsClient.commitAndClose(snapshot, metadataChange)
            } else if ((snapshot.metadata.progressValue == currentProgress.toLong())) {

                val currentLockedLettersCount =
                    allLevels.sumOf { it.lettersProperties.count { letter -> letter.locked } }
                val snapshotLockedLettersCount =
                    levelsList.sumOf { it.lettersProperties.count { letter -> letter.locked } }

                if ((currentLockedLettersCount > snapshotLockedLettersCount) || (snapshotGameModifiedTime < currentGameModifiedTime)) {
                    Log.i(TAG, "Current game was modified more recently. Updating snapshot.")
                    snapshot.snapshotContents.writeBytes(levelsData)
                    val metadataChange = SnapshotMetadataChange.Builder()
                        .setCoverImage(coverImage)
                        .setDescription(desc)
                        .setProgressValue(currentProgress.toLong())
                        .build()
                    return snapshotsClient.commitAndClose(snapshot, metadataChange)
                }
            }

        } else if (snapshot.metadata.progressValue == PROGRESS_VALUE_UNKNOWN) {
            Log.i(TAG, "Current progress is greater. First time snapshot upload.")
            snapshot.snapshotContents.writeBytes(levelsData)
            val metadataChange = SnapshotMetadataChange.Builder()
                .setCoverImage(coverImage)
                .setDescription(desc)
                .setProgressValue(currentProgress.toLong())
                .build()
            return snapshotsClient.commitAndClose(snapshot, metadataChange)
        }

        Log.d(TAG, "Snapshot in cloud had more progress. Return null.")
        return null
    }

    suspend fun loadSnapshot(activity: Activity) {
        coroutineScope {
            val signInAccount = GoogleSignIn.getLastSignedInAccount(activity.applicationContext)
            if (signInAccount != null) {
                val snapshotsClient = Games.getSnapshotsClient(
                    activity,
                    signInAccount
                )
                snapshotsClient.open(C.SAVE_GAME, true, RESOLUTION_POLICY_LAST_KNOWN_GOOD)
                    .addOnSuccessListener {
                        it.data?.let { snapshot ->
                            try {
                                val bytes = snapshot.snapshotContents.readFully()
                                viewModelScope.launch {
                                    if (updateLocalDatabase(snapshot, bytes)) {
                                        Log.i(TAG, "Start game with game loaded from cloud.")
                                        changeState(LoadingState.LOADING)
                                        startGame()
                                    } else {
                                        Log.i(TAG, "Start game normally.")
                                        startGame()
                                    }
                                }
                            } catch (e: IOException) {
                                Log.e(TAG, "Error getting bytes from snapshot.: ${e.message}")
                            }
                        }
                    }.addOnFailureListener {
                        Log.e(TAG, "Could not open snapshot: ${it.message}")
                    }
            }
        }
    }

    private suspend fun updateLocalDatabase(snapshot: Snapshot, bytes: ByteArray?): Boolean {
        val gson = Gson()
        val allLevels = levelRepository.getSaveGameData()
        val levelsListType: Type = object : TypeToken<ArrayList<Level?>?>() {}.type
        val levelsList: List<Level>? = gson.fromJson(bytes?.decodeToString(), levelsListType)

        Log.i(TAG, "Trying to update local database if needed...")

        if (levelsList != null) {

            val currentGameModifiedTime = allLevels.maxOf { it.levelInfo.timeModified }
            val snapshotGameModifiedTime = levelsList.maxOf { it.levelInfo.timeModified }
            val currentProgress =
                (allLevels.findLast { it.levelInfo.progress > LevelProgress.TODO }?.levelInfo?.levelId
                    ?: 1)

            if ((snapshot.metadata.progressValue > currentProgress)) {
                Log.i(TAG, "Snapshot progress is greater. Updating local database.")
                levelRepository.updateLevels(levelsList)
                Toast.makeText(
                    getApplication(),
                    "Restoring game from cloud.",
                    Toast.LENGTH_LONG
                ).show()
                return true
            } else if ((snapshot.metadata.progressValue == currentProgress.toLong())) {
                val currentLockedLettersCount =
                    allLevels.sumOf { it.lettersProperties.count { letter -> letter.locked } }
                val snapshotLockedLettersCount =
                    levelsList.sumOf { it.lettersProperties.count { letter -> letter.locked } }

                if ((snapshotLockedLettersCount > currentLockedLettersCount) || (snapshotGameModifiedTime > currentGameModifiedTime)) {
                    Log.i(TAG, "Snapshot was modified more recently. Updating local database.")
                    levelRepository.updateLevels(levelsList)
                    Toast.makeText(
                        getApplication(),
                        "Restoring game from cloud.",
                        Toast.LENGTH_LONG
                    ).show()
                    return true
                }
            }
        }
        return false
    }
}


sealed class UserRewardEvent {
    data class Success(
        val rewardType: RewardType = RewardType.NONE,
        val rewardEarned: Boolean = false
    ) : UserRewardEvent()

    data class Error(val exception: Throwable) : UserRewardEvent()
}
