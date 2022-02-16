package com.indest.hah.game

import android.app.Application
import androidx.lifecycle.*
import com.indest.hah.R
import com.indest.hah.letter.Cell
import com.indest.hah.letter.Letter
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

    private val _currentLevel = MutableStateFlow<Level?>(null)
    val currentLevel = _currentLevel.asStateFlow()

    private val _solution = MutableStateFlow(SolutionData(Solution.NONE, -1, -1, -1))
    val solution = _solution.asStateFlow()

    var windowSizeClassWidth: WindowSizeClass = WindowSizeClass.NOT_SET
    var isBookMode: Boolean = false

    init {
        viewModelScope.launch {
            levelRepository.latestLevelFlow()
                .collect {
                    _currentLevel.value = it
                }
        }
    }

    private fun updateSolutionState(index: Int, guess: String) {
        viewModelScope.launch {
            val solution = _currentLevel.value?.levelInfo?.solution?.replace(" ", "")
            if (guess.length == solution?.length) {
                val playerGuess = guess.map { char ->
                    if ("^([?!.,’])".contains(char)) "" else char
                }.joinToString("")

                when {
                    guess == solution -> {
                        updateLevelProgress(LevelProgress.INTERMEDIATE)
                        _solution.emit(
                            SolutionData(
                                solution = Solution.CORRECT,
                                index = index,
                                score = _currentLevel.value?.levelInfo?.score!!,
                                maxScore = _currentLevel.value?.levelInfo?.levelId!! * 100
                            )
                        )
                    }
                    playerGuess.reversed() == playerGuess -> _solution.emit(
                        SolutionData(
                            solution = Solution.BOGUS_PALINDROME,
                            index = index,
                            score = _currentLevel.value?.levelInfo?.score!!,
                            maxScore = _currentLevel.value?.levelInfo?.levelId!! * 100
                        )
                    )
                    else -> _solution.emit(
                        SolutionData(
                            solution = Solution.WRONG,
                            index = index,
                            score = _currentLevel.value?.levelInfo?.score!!,
                            maxScore = _currentLevel.value?.levelInfo?.levelId!! * 100
                        )
                    )
                }
            } else {
                _solution.emit(
                    SolutionData(
                        solution = Solution.NONE,
                        index = -1,
                        score = -1,
                        maxScore = -1
                    )
                )
            }
        }
    }

    fun latestLevelFlow(): Flow<Level> {
        return levelRepository.latestLevelFlow()
            .distinctUntilChangedBy(keySelector = { level -> level.levelInfo.levelId })
    }

    fun updateLetters(topIndex: Int, bottomLetter: Char, bottomIndex: Int, topLetter: Char) {
        viewModelScope.launch {
            currentLevel.value?.levelInfo?.let { levelInfo ->
                val currentSolution = levelRepository.updateLetters(
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
            currentLevel.value?.levelInfo?.let { levelInfo ->
                val pointsToSubtract = levelInfo.levelId * 100 / 10
                val newScore = if (levelInfo.score - pointsToSubtract < 0) 0
                else levelInfo.score - pointsToSubtract
                levelRepository.updateScore(newScore, levelInfo.levelId)
            }
        }
    }

    fun setLetterLocked(index: Int) {
        viewModelScope.launch {
            currentLevel.value?.levelInfo?.let { levelInfo ->
                levelRepository.setLetterLocked(
                    homeIndex = index,
                    level = levelInfo.levelId
                )
            }
        }
    }

    fun updateFriendIndex(index: Int, newIndex: Int) {
        viewModelScope.launch {
            currentLevel.value?.levelInfo?.let { levelInfo ->
                val currentSolution = levelRepository.updateFriendIndex(
                    homeIndex = index,
                    newFriendIndex = newIndex,
                    level = levelInfo.levelId
                )
                updateSolutionState(index, currentSolution)
            }
        }
    }

    fun changeLevel(nr: Int) {
        viewModelScope.launch {
            currentLevel.value?.levelInfo?.let { levelInfo ->
                val hintInterval = getApplication<Application>().getString(R.string.hint_suggestion)
                levelRepository.setCountTo(hintInterval, 0)
                levelRepository.updateNextLevelTimestamp(
                    levelInfo.levelId,
                    nr
                )
            }
        }
    }

    private suspend fun updateLevelProgress(progress: LevelProgress) {
        currentLevel.value?.levelInfo?.let { levelInfo ->
            val updatedLevel = levelInfo.copy(
                progress = progress,
                puzzle = levelInfo.puzzle,
                score = levelInfo.score
            )
            levelRepository.updateLevelAndBestScore(
                updatedLevel,
                levelInfo.score
            )
        }
    }

    fun nextLevel()
    {
        viewModelScope.launch {
            currentLevel.value?.levelInfo?.let { levelInfo ->
                val hintInterval = getApplication<Application>().getString(R.string.hint_suggestion)
                levelRepository.setCountTo(hintInterval, 0)
                levelRepository.resetLevelLettersInfo(levelInfo.levelId)
                levelRepository.unlockNextLevel(levelInfo.levelId)
                updateLevelProgress(LevelProgress.REPLAY)
                val levelNr = levelInfo.levelId + 1
                levelRepository.updateNextLevelTimestamp(
                    levelInfo.levelId,
                    levelNr
                )
            }
        }
    }

    fun allLevelsFlow(): Flow<List<LevelData>> {
        return flow {
            emit(levelRepository.getAllLevels())
        }
    }

    fun persistInitialLettersInfo(cells: Sequence<Cell<*, *>>) {
        viewModelScope.launch {
            currentLevel.value?.levelInfo?.let { levelInfo ->
                val isAlreadyInitialized =
                    levelRepository.isLettersInitialized(levelInfo.levelId)
                if (!isAlreadyInitialized) {
                    cells.filterIsInstance<Letter>().forEachIndexed { _, it ->
                        levelRepository.insertLetterInfo(
                            level = levelInfo.levelId,
                            homeIndex = it.homeIndex,
                            friendIndex = it.friendIndex,
                            letter = it.text.first(),
                            locked = false,
                        )
                    }
                    val updatedLevel = levelInfo.copy(
                        progress = LevelProgress.IN_PROGRESS,
                        puzzle = levelInfo.puzzle,
                        score = levelInfo.levelId * 100
                    )
                    levelRepository.updateLevel(updatedLevel)
                }
            }
        }
    }

    enum class Solution {
        CORRECT,
        WRONG,
        BOGUS_PALINDROME,
        NONE
    }

    data class SolutionData(val solution: Solution, val index: Int, val score: Int, val maxScore: Int)
}