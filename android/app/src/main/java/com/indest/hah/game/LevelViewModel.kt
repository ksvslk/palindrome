package com.indest.hah.game

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.games.Games
import com.indest.hah.R
import com.indest.hah.persistance.level.LevelProgress
import com.indest.hah.persistance.level.LevelRepository
import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.game.levelcreation.WindowSizeClass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

    private val _solution = MutableStateFlow(SolutionData(Solution.NONE, -1, -1, -1, false))
    val solution = _solution.asStateFlow()

    var windowSizeClassWidth: WindowSizeClass = WindowSizeClass.NOT_SET
    var isBookMode: Boolean = false

    init {
        viewModelScope.launch {
            levelRepository.init() // First time app starts
            levelRepository.latestLevelFlow()
                .collect {
                    _currentLevel.value = it
                }
        }
    }

    fun setState(newUiState: UiState) {
        viewModelScope.launch {
            _uiState.emit(newUiState)
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
        if (_currentLevel.value.levelInfo.solution == "LEVEL")
        {
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
                        _uiState.emit(UiState.TOUCH_DISABLED)
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

    fun latestDistinctLevelFlow(): Flow<Level> {
        return _currentLevel.distinctUntilChangedBy(keySelector = { level ->
            level.levelInfo.levelId
        })
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

    enum class UiState {
        LOADING,
        TOUCH_ENABLED,
        TOUCH_DISABLED
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