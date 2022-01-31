package com.indest.hah.game

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.SkuDetails
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.games.Games
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.SnapshotsClient.RESOLUTION_POLICY_LAST_KNOWN_GOOD
import com.google.android.gms.games.snapshot.Snapshot
import com.google.android.gms.games.snapshot.SnapshotMetadata
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import com.google.android.gms.tasks.Task
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
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

    var windowSizeClassWidth: WindowSizeClass = WindowSizeClass.NOT_SET
    var skuDetailsList: List<SkuDetails>? = null

    fun changeState(newState: LoadingState) {
        _loadingState.value = AppLoadingState(state = newState)
    }

    fun changeInternetState(newState: InternetState) {
        _internetState.value = AppInternetState(state = newState)
    }

    fun setRewardedAdAvailability(boolean: Boolean) {
        _rewardedAdAvailable.value = boolean
    }

    fun startGame() {
        viewModelScope.launch {
            changeState(LoadingState.READY)
            levels.value = levelRepository.getAllLevels()
            levelRepository.getLastLevel()
                .collect { newLevel ->
                    Log.i(TAG, _uiState.value.currentLevel.toString())
                    Log.i(TAG, newLevel.toString())
                    val oldLevel = _uiState.value.currentLevel
                    _uiState.value = LevelEvent.Success(oldLevel, newLevel)
                }
        }
    }

    fun rewardUser(rewardType: RewardType) {
        _userReward.value = UserRewardEvent.Success(rewardType = rewardType, rewardEarned = true)
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
                        }
                        catch (e:IOException)
                        {
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
        bytes:ByteArray
    ): Task<SnapshotMetadata>? {

        val gson = Gson()
        val allLevels = levelRepository.getSaveGameData()
        val levelsData = gson.toJson(allLevels).toByteArray()
        val currentProgress = (allLevels.findLast { it.levelInfo.progress > LevelProgress.TODO }?.levelInfo?.levelId
            ?: 1)
        val currentScore = allLevels.sumOf { it.levelInfo.score }
        val desc = "Level progress $currentProgress. Saved at: " + Calendar.getInstance().time

        val levelsListType: Type = object : TypeToken<ArrayList<Level?>?>() {}.type
        val levelsList: List<Level>? = gson.fromJson(bytes.decodeToString(), levelsListType)
        val currentScoreIsGreater = currentScore > (levelsList?.sumOf { it.levelInfo.score } ?: 0)

        if ((snapshot.metadata.progressValue < currentProgress) || (snapshot.metadata.progressValue == currentProgress.toLong() && currentScoreIsGreater)) {
            snapshot.snapshotContents.writeBytes(levelsData)
            val metadataChange = SnapshotMetadataChange.Builder()
                .setCoverImage(coverImage)
                .setDescription(desc)
                .setProgressValue(currentProgress.toLong())
                .build()
            return snapshotsClient.commitAndClose(snapshot, metadataChange)
        }

        Log.d(TAG, "Snapshot in cloud had more progress.")
        return null
    }

    suspend fun loadSnapshot(activity: Activity) {
        coroutineScope {
            val signInAccount = GoogleSignIn.getLastSignedInAccount(activity.applicationContext)
            if (signInAccount != null)
            {
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
                                    updateLocalDatabase(snapshot, bytes)
                                    startGame()
                                }
                            }
                            catch (e:IOException)
                            {
                                Log.e(TAG, "Error getting bytes from snapshot.: ${e.message}")
                            }
                        }
                    }.addOnFailureListener {
                        Log.e(TAG, "Could not open snapshot: ${it.message}")
                    }
            }

        }

    }

    private suspend fun updateLocalDatabase(snapshot: Snapshot, bytes: ByteArray?) {
        val gson = Gson()
        val allLevels = levelRepository.getAllLevels()
        val levelsListType: Type = object : TypeToken<ArrayList<Level?>?>() {}.type
        val levelsList: List<Level>? = gson.fromJson(bytes?.decodeToString(), levelsListType)

        if (levelsList != null)
        {
            Log.i(TAG, "Update local database")

            val snapShotScoreIsGreater =
                levelsList.sumOf { it.levelInfo.score } > allLevels.sumOf { it.score }
            val currentProgress = (allLevels.findLast { it.progress > LevelProgress.TODO }?.levelId
                ?: 1)
            Log.i(TAG, "currentProgress: $currentProgress, snapshot.metadata.progressValue: ${snapshot.metadata.progressValue}")
            if ((snapshot.metadata.progressValue > currentProgress) || (snapshot.metadata.progressValue == currentProgress.toLong() && snapShotScoreIsGreater)) {
                levelRepository.updateLevels(levelsList)
            }
        }
    }
}

sealed class LevelEvent {
    data class Success(val oldLevel: Level, val currentLevel: Level) : LevelEvent()
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