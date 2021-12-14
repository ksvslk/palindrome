package com.indest.hah.game

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.rewarded.RewardedAd
import com.indest.hah.letter.Cell
import com.indest.hah.letter.Letter
import com.indest.hah.persistance.LevelProgress
import com.indest.hah.persistance.LevelRepository
import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.dto.LevelData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val levelRepository: LevelRepository,
    application: Application
) :
    AndroidViewModel(application) {

    private val TAG = "GameViewModel"

    val levels = MutableLiveData<List<LevelData>>()
    val realSolutionAndGuess = MutableLiveData<Triple<String?, String?, Int?>>()
    private val _uiState = MutableStateFlow(LatestLevelEvent.Success(Level(), Level()))
    val uiState: StateFlow<LatestLevelEvent> = _uiState

    var rewardedAd: RewardedAd? = null

    init {
        viewModelScope.launch {
            levels.value = levelRepository.getAllLevels()
            levelRepository.getLastLevel()
                .collect { newLevel ->
                    val oldLevel = _uiState.value.currentLevel
                    _uiState.value = LatestLevelEvent.Success(oldLevel, newLevel)
                }
        }
    }

    fun changeLevel(nr: Int) {
        viewModelScope.launch {
            levelRepository.updateNextLevelTimestamp(
                _uiState.value.currentLevel.levelInfo.levelId,
                nr
            )
        }
    }

    fun changeToNextLevel() {
        viewModelScope.launch {
            levelRepository.updateNextLevelTimestamp(
                _uiState.value.currentLevel.levelInfo.levelId,
                _uiState.value.currentLevel.levelInfo.levelId + 1
            )
        }
    }

    fun getLevelNumber():Int {
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
            levelRepository.resetLevelLettersInfo(_uiState.value.currentLevel.levelInfo.levelId)
            updateLevelProgress(LevelProgress.REPLAY)
        }
    }

    private suspend fun updateLevelProgress(progress: LevelProgress)
    {
        val updatedLevel = _uiState.value.currentLevel.levelInfo.copy(
            progress = progress,
            puzzle = _uiState.value.currentLevel.levelInfo.puzzle,
            score = _uiState.value.currentLevel.levelInfo.score
        )
        levelRepository.updateLevel(updatedLevel)
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
                levelRepository.updateScore(
                    _uiState.value.currentLevel.levelInfo.levelId * 100,
                    _uiState.value.currentLevel.levelInfo.levelId
                )
                updateLevelProgress(LevelProgress.IN_PROGRESS)
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
}

sealed class LatestLevelEvent {
    data class Success(val oldLevel: Level, val currentLevel: Level) : LatestLevelEvent()
    data class Error(val exception: Throwable) : LatestLevelEvent()
}