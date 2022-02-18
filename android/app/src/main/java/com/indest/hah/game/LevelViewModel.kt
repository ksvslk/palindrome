package com.indest.hah.game

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.indest.hah.R
import com.indest.hah.persistance.LevelProgress
import com.indest.hah.persistance.LevelRepository
import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.util.WindowSizeClass
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

    private val _uiState = MutableStateFlow(UiState.TOUCH_DISABLED)
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

    private fun updateSolutionState(index: Int, guess: String) {
        viewModelScope.launch {
            val solution = _currentLevel.value.levelInfo.solution.replace(" ", "")
            if (guess.length == solution.length) {
                val playerGuess = guess.map { char ->
                    if ("^([?!.,’])".contains(char)) "" else char
                }.joinToString("")
                Log.d(TAG, "guess: $guess, solution: $solution")
                val hintsInterval = getApplication<Application>().getString(R.string.hint_suggestion)

                when {
                    guess == solution -> {
                        _uiState.emit(UiState.TOUCH_DISABLED)
                        levelRepository.updateLevelProgress(
                            _currentLevel.value.levelInfo.levelId,
                            LevelProgress.INTERMEDIATE
                        )
                        _solution.emit(
                            SolutionData(
                                solution = Solution.CORRECT,
                                index = index,
                                score = _currentLevel.value.levelInfo.score,
                                maxScore = _currentLevel.value.levelInfo.levelId * 100,
                                showHintsBadge = false
                            )
                        )
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

    fun latestLevelFlow(): Flow<Level> {
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
                val pointsToSubtract = levelInfo.levelId * 100 / 10
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
                levelRepository.setCountTo(hintInterval, 0)
                levelRepository.nextLevelActions(
                    levelInfo.levelId,
                    chosenLevel
                )
            }
        }
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