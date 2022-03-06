package com.indest.hah.game

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.games.Games
import com.indest.hah.R
import com.indest.hah.game.levelcreation.WindowSizeClass
import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.persistance.dto.SaveGame
import com.indest.hah.persistance.level.LevelProgress
import com.indest.hah.persistance.level.LevelRepository
import com.indest.hah.savegame.SnapshotHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class LevelViewModel @Inject constructor(
    private val levelRepository: LevelRepository,
    application: Application
) :
    AndroidViewModel(application) {

    private val TAG = "LevelViewModel"

    private val _uiState = MutableStateFlow(UiState.LOADING)
    val uiState = _uiState.asStateFlow()

    private val _currentLevel = MutableStateFlow(Level())
    val currentLevel = _currentLevel.asStateFlow()

    val latestDistinctLevelFlow =
        _currentLevel
            .mapNotNull { if (it.levelInfo.levelId != 0) it else null }
            .distinctUntilChangedBy(keySelector = { level -> level.levelInfo.levelId  })
            .onEach {
                if (it.levelInfo.progress == LevelProgress.INTERMEDIATE) {
                    _uiState.value = UiState.LEVEL_FINISHED
                } else {
                    _uiState.value = UiState.READY
                }
            }

    private val _solution = MutableStateFlow(
        SolutionData(Solution.NONE, -1, -1, -1, false)
    )
    val solution = _solution.asStateFlow()

    var windowSizeClassWidth: WindowSizeClass = WindowSizeClass.NOT_SET
    var isBookMode: Boolean = false

    @Inject lateinit var cloudSave:SnapshotHelper
    private var saveGameJob:Job? = null

    private val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        Log.e(
            TAG,
            "Error syncing cloud with local. Start collecting level from  local database without up to date information."
        )
        collectLevels()
    }

    init {
        viewModelScope.launch {
            _uiState.value = UiState.LOADING
            levelRepository.init() // First time app starts
        }
    }

    fun loadLatestLevelFromCloudOrLocal() {
        viewModelScope.launch(exceptionHandler) {
            val saveGame = cloudSave.load()
            Log.d(TAG, "Get save game from cloud.")
            levelRepository.updateLocalGame(saveGame)
            Log.d(TAG, "Update local game to: ${saveGame.game.findLast { it.levelInfo.progress == LevelProgress.IN_PROGRESS }?.levelInfo}")
            collectLevels()
            startSavingGame()
        }
    }

    fun stopSavingGame()
    {
        Log.d(TAG, "Stop saving game.")
        saveGameJob?.cancel()
    }

    fun collectLevels()
    {
        Log.d(TAG, "Start collecting levels (_currentLevel flow).")
        _uiState.value = UiState.READY
        viewModelScope.launch {
            levelRepository.latestLevelFlow()
                .collect { _currentLevel.value = it }
        }
        startSavingGame()
    }

    fun setState(newUiState: UiState) {
        viewModelScope.launch {
            _uiState.emit(newUiState)
        }
    }

    fun updateLetters(topIndex: Int, bottomLetter: Char, bottomIndex: Int, topLetter: Char) {

        viewModelScope.launch {
            _currentLevel.value.levelInfo.let { levelInfo ->
                val currentSolution = levelRepository.switchLetters(
                    homeIndex1 = topIndex,
                    homeIndex2 = bottomIndex,
                    letter1 = bottomLetter,
                    letter2 = topLetter,
                    level = levelInfo.levelId
                )
                updateSolutionState(topIndex, currentSolution)
            }
        }
    }

    fun increaseMistakes() {
        viewModelScope.launch {
            _currentLevel.value.levelInfo.let { levelInfo ->
                val pointsToSubtract = levelInfo.levelId * 100 / 20
                val newScore = if (levelInfo.score - pointsToSubtract < 0) 0
                else levelInfo.score - pointsToSubtract
                levelRepository.updateScore(newScore, levelInfo.levelId)
            }
        }
    }

    fun setLetterLocked(index: Int) {
        viewModelScope.launch {
            _currentLevel.value.levelInfo.let { levelInfo ->
                levelRepository.setLetterLocked(
                    homeIndex = index,
                    level = levelInfo.levelId
                )
                saveGameImmediately()
                startSavingGame()
            }
        }
    }

    fun updateFriendIndex(index: Int, newIndex: Int) {
        viewModelScope.launch {
            _currentLevel.value.levelInfo.let { levelInfo ->
                val currentSolution = levelRepository.updateFriendIndex(
                    homeIndex = index,
                    newFriendIndex = newIndex,
                    level = levelInfo.levelId
                )
                updateSolutionState(index, currentSolution)
            }
        }
    }

    fun changeLevel(chosenLevel: Int) {
        viewModelScope.launch {
            _currentLevel.value.levelInfo.let { levelInfo ->
                val hintInterval = getApplication<Application>().getString(R.string.hint_suggestion)
                val scoreInterval =
                    getApplication<Application>().getString(R.string.player_centered_scores)
                val currentScoreIntervalCount = levelRepository.getCount(scoreInterval)

                // Loading each level gives +1 so to balance it out we will -1 each level
                // change made from "Level Select" screen as this is not part of the intended gameplay.
                if (currentScoreIntervalCount > 0) {
                    levelRepository.setCountTo(scoreInterval, currentScoreIntervalCount - 1)
                }

                levelRepository.setCountTo(hintInterval, 0)
                levelRepository.nextLevelActions(
                    levelInfo.levelId,
                    chosenLevel
                )
            }
        }
    }

    suspend fun isIntervalAttained(interval: String): Boolean {
        return levelRepository.increaseCount(interval)
    }

    fun executeLevelCompleteActions() {
        val levelNr = _currentLevel.value.levelInfo.levelId
        val hintInterval = getApplication<Application>().getString(R.string.hint_suggestion)
        viewModelScope.launch {
            levelRepository.executeLevelCompleteTransaction(levelNr, hintInterval)
        }
    }

    fun allLevelsFlow(): Flow<List<LevelData>> {
        return flow {
            emit(levelRepository.getAllLevels())
        }
    }

    fun startSavingGame()
    {
        if (saveGameJob == null || saveGameJob?.isCompleted == true)
        {
            Log.d(TAG, "Start saving game. Job was null/completed.")

            saveGameJob = viewModelScope.launch {
                while(isActive) {
                    delay(30000)
                    Log.d(TAG, "Save game to cloud.")
                    val saveGame = levelRepository.getLocalSaveGame()
                    cloudSave.save(saveGame)
                }
            }
        }
        else if (saveGameJob?.isCancelled == true) {
            Log.d(TAG, "Start saving game (job was Cancelled")
            saveGameJob?.start()
        }
    }

    fun saveGameImmediately()
    {
        viewModelScope.launch {
            val saveGame = levelRepository.getLocalSaveGame()
            cloudSave.save(saveGame)
        }
    }

    private fun submitScore() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            GoogleSignIn.getLastSignedInAccount(context)?.let {
                val totalScore = levelRepository.getTotalUpdatedScore(
                    _currentLevel.value.levelInfo.levelId
                )
                Games.getLeaderboardsClient(context, it)
                    .submitScore(
                        context.getString(R.string.leaderboard_crme_de_la_crme),
                        totalScore
                    )
            }
        }
    }

    private fun unlockAchievement(achievement: String) {
        val context = getApplication<Application>()
        GoogleSignIn.getLastSignedInAccount(context)?.let { signedInAccount ->
            Games.getAchievementsClient(context, signedInAccount)
                .unlock(achievement)
        }
    }

    private fun updateAndConsiderFlawlessAchievements(
        solutionData: SolutionData
    ) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            if (solutionData.score == solutionData.maxScore) {
                if (levelRepository.increaseCount(context.getString(R.string.achievement_flawless_5))) {
                    unlockAchievement(context.getString(R.string.achievement_flawless_5))
                }
                if (levelRepository.increaseCount(context.getString(R.string.achievement_flawless_15))) {
                    unlockAchievement(context.getString(R.string.achievement_flawless_15))
                }
            } else {
                levelRepository.setCountTo(context.getString(R.string.achievement_flawless_5), 0)
                levelRepository.setCountTo(context.getString(R.string.achievement_flawless_15), 0)
            }
        }
    }

    private fun resolveLevelCompleteAchievements(solutionData: SolutionData) {
        val context = getApplication<Application>()

        // Flawless 5 and Flawless 15 achievements
        updateAndConsiderFlawlessAchievements(solutionData)

        // Zero mistakes achievement
        if (_currentLevel.value.levelInfo.levelId >= 21 &&
            solutionData.score == solutionData.maxScore
        ) {
            unlockAchievement(context.getString(R.string.achievement_zero_mistakes))
        }

        // Complete Level achievement
        if (_currentLevel.value.levelInfo.solution == "LEVEL") {
            unlockAchievement(context.getString(R.string.achievement_complete_level))
        }

        // Greenhorn, Proficient, Crackerjack, Chief and Authority achievements
        when (_currentLevel.value.levelInfo.levelId) {
            20 -> unlockAchievement(context.getString(R.string.achievement_greenhorn))
            40 -> unlockAchievement(context.getString(R.string.achievement_proficient))
            60 -> unlockAchievement(context.getString(R.string.achievement_crackerjack))
            80 -> unlockAchievement(context.getString(R.string.achievement_chief))
            100 -> unlockAchievement(context.getString(R.string.achievement_authority))
        }
    }

    private fun updateSolutionState(index: Int, guess: String) {
        viewModelScope.launch {
            val solution = _currentLevel.value.levelInfo.solution.replace(" ", "")
            if (guess.length == solution.length) {
                val playerGuess = guess.map { char ->
                    if ("^([?!.,’])".contains(char)) "" else char
                }.joinToString("")
                Log.d(TAG, "guess: $guess, solution: $solution")
                val hintsInterval =
                    getApplication<Application>().getString(R.string.hint_suggestion)

                when {
                    guess == solution -> {
                        _uiState.emit(UiState.BUSY)
                        submitScore()
                        levelRepository.updateLevelProgress(
                            _currentLevel.value.levelInfo.levelId,
                            LevelProgress.INTERMEDIATE
                        )
                        val solutionData = SolutionData(
                            solution = Solution.CORRECT,
                            index = index,
                            score = _currentLevel.value.levelInfo.score,
                            maxScore = _currentLevel.value.levelInfo.levelId * 100,
                            showHintsBadge = false
                        )
                        _solution.emit(solutionData)
                        resolveLevelCompleteAchievements(solutionData)
                        levelRepository.setCountTo(hintsInterval, 0)
                    }
                    playerGuess.reversed() == playerGuess -> {
                        val showHintsBadge = levelRepository.increaseCount(hintsInterval)
                        _solution.emit(
                            SolutionData(
                                solution = Solution.BOGUS_PALINDROME,
                                index = index,
                                score = _currentLevel.value.levelInfo.score,
                                maxScore = _currentLevel.value.levelInfo.levelId * 100,
                                showHintsBadge = showHintsBadge
                            )
                        )
                        // Wrong Palindrome achievement
                        unlockAchievement(getApplication<Application>().getString(R.string.achievement_wrong_palindrome))
                    }
                    else -> {
                        val showHintsBadge = levelRepository.increaseCount(hintsInterval)
                        _solution.emit(
                            SolutionData(
                                solution = Solution.WRONG,
                                index = index,
                                score = _currentLevel.value.levelInfo.score,
                                maxScore = _currentLevel.value.levelInfo.levelId * 100,
                                showHintsBadge = showHintsBadge
                            )
                        )
                    }
                }
            } else {
                _solution.emit(
                    SolutionData(
                        solution = Solution.NONE,
                        index = -1,
                        score = -1,
                        maxScore = -1,
                        showHintsBadge = false
                    )
                )
            }
        }
    }

    enum class UiState {
        LOADING,
        READY,
        LEVEL_FINISHED,
        BUSY
    }

    enum class Solution {
        CORRECT,
        WRONG,
        BOGUS_PALINDROME,
        NONE

    }

    data class SolutionData(
        val solution: Solution,
        val index: Int,
        val score: Int,
        val maxScore: Int,
        val showHintsBadge: Boolean
    )

}