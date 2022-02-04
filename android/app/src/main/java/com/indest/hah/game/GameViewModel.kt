package com.indest.hah.game

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
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
import com.indest.hah.util.C
import com.indest.hah.util.WindowSizeClass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.lang.reflect.Type
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap


@HiltViewModel
class GameViewModel @Inject constructor(
    private val levelRepository: LevelRepository,
    application: Application
) :
    AndroidViewModel(application) {

    private val TAG = "GameViewModel"

    val levels = MutableLiveData<List<LevelData>>()
    val totalScore = MutableLiveData<Int>()
    val realSolutionAndGuess = MutableLiveData<Triple<String?, String?, Int?>>()

    private val _uiState = MutableStateFlow(LevelEvent.Success(Level(), Level()))
    val uiState: StateFlow<LevelEvent> = _uiState

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

    var windowSizeClassWidth: WindowSizeClass = WindowSizeClass.NOT_SET

    private var billingClient: BillingClient? = null

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    Log.i(TAG, "Should handle purchase here.")
                    //handlePurchase(purchase)
                    // TODO: give the goods
                }
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                Log.i(TAG, "User cancelled the purchase flow.")
            } else {
                Log.i(TAG, "Billing flow failed with: ${billingResult.responseCode}")
            }

        }

    init {
        billingClient = BillingClient.newBuilder(application.applicationContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
        billingInitialization(application.applicationContext)
    }

    private fun billingInitialization(context: Context) {
        Log.d(TAG, "Billing Initialization")
        billingClient?.startConnection(object:
            BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    viewModelScope.launch {
                        querySkuDetails(context)
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.i(TAG, "onBillingServiceDisconnected")
                billingInitialization(context)
            }
        })
    }

    suspend fun querySkuDetails(context: Context) {
        val skuList = ArrayList<String>()
        skuList.add(context.getString(R.string.remove_letter_ads_sku))
        skuList.add(context.getString(R.string.remove_clue_ads_sku))
        skuList.add(context.getString(R.string.remove_inbetween_ads_sku))
        skuList.add(context.getString(R.string.remove_all_ads_sku))
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)

        // leverage querySkuDetails Kotlin extension function
        val skuDetailsResult = withContext(Dispatchers.IO) {
            billingClient?.querySkuDetails(params.build())
        }
        skuDetailsResult?.skuDetailsList?.map {
            SkuDetailsWrapper(purchased = false, it)
        }?.let {
            _skuDetails.value = it
        }
    }

    fun launchPurchaseFlow(skuDetails: SkuDetails, activity: Activity) {
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()
        val responseCode = billingClient?.launchBillingFlow(activity, flowParams)?.responseCode
        Log.i(TAG, "Launch billing flow response code: $responseCode")
    }

    suspend fun handlePurchase(purchase: Purchase) {
        // Purchase retrieved from BillingClient#queryPurchasesAsync or your PurchasesUpdatedListener.
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                    val ackPurchaseResult = withContext(Dispatchers.IO) {
                        billingClient?.acknowledgePurchase(acknowledgePurchaseParams.build())
                    }

                }
            }
            Purchase.PurchaseState.PENDING -> {

            }
        }
        // Verify the purchase.
        // Ensure entitlement was not already granted for this purchaseToken.
        // Grant entitlement to the user.

        val consumeParams =
            ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        val consumeResult = withContext(Dispatchers.IO) {
            billingClient?.consumePurchase(consumeParams)
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

    suspend fun getLatestLevel():Level
    {
        val latest = levelRepository.getLatestLevel()
        _uiState.value = LevelEvent.Success(_uiState.value.currentLevel, latest)
        return latest
    }

    fun startGame() {
        viewModelScope.launch {
            levels.value = levelRepository.getAllLevels()
        }
        changeState(LoadingState.READY)
    }

    suspend fun getLevelCount():Int
    {
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
                _uiState.value.currentLevel.levelInfo.levelId,
                nr
            )
        }
    }

    fun getLevelNumber(): Int {
        return _uiState.value.currentLevel.levelInfo.levelId
    }

    fun increaseMistakes() {
        viewModelScope.launch {
            val newScore = if (_uiState.value.currentLevel.levelInfo.score - 2 < 0) {
                0
            } else {
                _uiState.value.currentLevel.levelInfo.score - 2
            }
            levelRepository.updateScore(
                newScore,
                _uiState.value.currentLevel.levelInfo.levelId
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
            levelRepository.resetLevelLettersInfo(_uiState.value.currentLevel.levelInfo.levelId)
            levelRepository.unlockNextLevel(_uiState.value.currentLevel.levelInfo.levelId)
            updateLevelProgress(LevelProgress.REPLAY)
        }
    }

    private suspend fun updateLevelProgress(progress: LevelProgress) {
        val updatedLevel = _uiState.value.currentLevel.levelInfo.copy(
            progress = progress,
            puzzle = _uiState.value.currentLevel.levelInfo.puzzle,
            score = _uiState.value.currentLevel.levelInfo.score
        )
        levelRepository.updateLevelAndBestScore(
            updatedLevel,
            _uiState.value.currentLevel.levelInfo.score
        )
        totalScore.value = levelRepository.getTotalScore()
    }

    fun getCurrentAndMaxScore(): Pair<Int, Int> {
        return Pair(
            _uiState.value.currentLevel.levelInfo.score,
            _uiState.value.currentLevel.levelInfo.levelId * 100
        )
    }

    fun isLastLevel(): Boolean {
        return _uiState.value.currentLevel.levelInfo.levelId == levels.value?.size
    }

    fun updateLetters(topIndex: Int, bottomLetter: Char, bottomIndex: Int, topLetter: Char) {
        Log.i(TAG, "updateLetters")
        viewModelScope.launch {
            levelRepository.updateLetter(
                homeIndex = topIndex,
                letter = bottomLetter,
                level = _uiState.value.currentLevel.levelInfo.levelId
            )
            levelRepository.updateLetter(
                homeIndex = bottomIndex,
                letter = topLetter,
                level = _uiState.value.currentLevel.levelInfo.levelId
            )

            realSolutionAndGuess.value = Triple(
                _uiState.value.currentLevel.levelInfo.solution.replace(" ", ""),
                levelRepository.currentSolution(_uiState.value.currentLevel.levelInfo.levelId),
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
                level = _uiState.value.currentLevel.levelInfo.levelId
            )
            realSolutionAndGuess.value = Triple(
                _uiState.value.currentLevel.levelInfo.solution.replace(" ", ""),
                levelRepository.currentSolution(_uiState.value.currentLevel.levelInfo.levelId),
                index
            )
        }
    }

    fun setLetterLocked(index: Int) {
        Log.i(TAG, "setLetterLocked")
        viewModelScope.launch {
            levelRepository.setLetterLocked(
                homeIndex = index,
                level = _uiState.value.currentLevel.levelInfo.levelId
            )
        }
    }

    fun persistInitialLettersInfo(cells: MutableList<Cell<*, *>>) {
        viewModelScope.launch {
            val isAlreadyInitialized =
                levelRepository.isLettersInitialized(_uiState.value.currentLevel.levelInfo.levelId)
            if (!isAlreadyInitialized) {
                Log.i(TAG, "initial letters persist")
                cells.filterIsInstance<Letter>().forEachIndexed { _, it ->
                    levelRepository.insertLetterInfo(
                        level = _uiState.value.currentLevel.levelInfo.levelId,
                        homeIndex = it.homeIndex,
                        friendIndex = it.friendIndex,
                        letter = it.text.first(),
                        locked = false,
                    )
                }
                val updatedLevel = _uiState.value.currentLevel.levelInfo.copy(
                    progress = LevelProgress.IN_PROGRESS,
                    puzzle = _uiState.value.currentLevel.levelInfo.puzzle,
                    score = _uiState.value.currentLevel.levelInfo.levelId * 100
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
                _uiState.value.currentLevel.levelInfo.levelId
            )
        }
    }

    fun isCategoryRewarded(): Boolean {
        return _uiState.value.currentLevel.levelInfo.categoryRewarded
    }

    suspend fun saveSnapshot(activity: Activity, coverImage: Bitmap) {
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

                val currentLockedLettersCount = allLevels.sumOf { it.lettersProperties.count { letter -> letter.locked }}
                val snapshotLockedLettersCount = levelsList.sumOf{ it.lettersProperties.count { letter -> letter.locked }}

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

        }
        else if(snapshot.metadata.progressValue == PROGRESS_VALUE_UNKNOWN)
        {
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
                                    if(updateLocalDatabase(snapshot, bytes))
                                    {
                                        Log.i(TAG, "Start game with game loaded from cloud.")
                                        changeState(LoadingState.LOADING)
                                        startGame()
                                    }
                                    else {
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
                val currentLockedLettersCount = allLevels.sumOf { it.lettersProperties.count { letter -> letter.locked }}
                val snapshotLockedLettersCount = levelsList.sumOf{ it.lettersProperties.count { letter -> letter.locked }}

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

sealed class LevelEvent {
    data class Success(
        val oldLevel: Level,
        val currentLevel: Level,
    ) : LevelEvent()
    data class Error(val exception: Throwable) : LevelEvent()
}

sealed class UserRewardEvent {
    data class Success(
        val rewardType: RewardType = RewardType.NONE,
        val rewardEarned: Boolean = false,
        val doubleCheck: Boolean = false
    ) : UserRewardEvent()

    data class Error(val exception: Throwable) : UserRewardEvent()
}
