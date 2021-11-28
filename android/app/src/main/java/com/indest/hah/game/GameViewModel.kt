package com.indest.hah.game

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.indest.hah.letter.Cell
import com.indest.hah.letter.Letter
import com.indest.hah.persistance.LevelProgress
import com.indest.hah.persistance.LevelRepository
import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.dto.LevelData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.sqrt

@OptIn(InternalCoroutinesApi::class)
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
            levelRepository.updateNextLevelTimestamp(_uiState.value.currentLevel.levelInfo.levelId, nr)
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

    fun updateCompletedLevel(levelScore: Int = 1337) {
        viewModelScope.launch {
            levelRepository.resetLevelLettersInfo(_uiState.value.currentLevel.levelInfo.levelId)
            val updatedLevel = _uiState.value.currentLevel.levelInfo.copy(
                progress = LevelProgress.REPLAY,
                puzzle = _uiState.value.currentLevel.levelInfo.puzzle,
                score = levelScore
            )
            levelRepository.updateLevel(updatedLevel)
        }
    }

    fun isLastLevel(): Boolean {
        return _uiState.value.currentLevel.levelInfo.levelId == levels.value?.size
    }

    fun updateLetters(topIndex: Int, bottomLetter: Char, bottomIndex: Int, topLetter: Char) {
        Log.i("GMV", "updateLetters")
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

            val currentSolution = levelRepository.currentSolution(_uiState.value.currentLevel.levelInfo.levelId)
            val realSolution = _uiState.value.currentLevel.levelInfo.solution
            Log.i(TAG, currentSolution.toString())
            Log.i(TAG, realSolution.toString())
            val similarityScore = getSimilarityScore(currentSolution.map { c -> c.code }, realSolution.map { c -> c.code })
            Log.i(TAG, "Similarity: $similarityScore")

            realSolutionAndGuess.value = Triple(
                _uiState.value.currentLevel.levelInfo.solution.replace(" ", ""),
                levelRepository.currentSolution(_uiState.value.currentLevel.levelInfo.levelId),
                topIndex
            )

        }
    }

    private fun getSimilarityScore(solution: List<Int>, guess: List<Int>): Double {
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        solution.forEachIndexed { i, _ ->
            val guessI = if (guess.contains(i)) guess[i] else 0
            dotProduct += solution[i] * guessI
            normA += solution[i].toDouble().pow(2.0)
            normB += guessI.toDouble().pow(2.0)
        }
        return dotProduct / (sqrt(normA) * sqrt(normB))
    }

    fun updateFriendIndex(index: Int, newIndex: Int) {
        Log.i("GMV", "updateFriendIndex")
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

    fun persistInitialLettersInfo(cells: MutableList<Cell<*, *>>) {
        viewModelScope.launch {
            val isAlreadyInitialized =
                levelRepository.isLettersInitialized(_uiState.value.currentLevel.levelInfo.levelId)
            if (!isAlreadyInitialized) {
                Log.i(TAG, "isAlreadyInitialized is false, starting to insert data (for first time)")
                cells.filterIsInstance<Letter>().forEachIndexed { _, it ->
                    levelRepository.insertLetterInfo(
                        level = _uiState.value.currentLevel.levelInfo.levelId,
                        homeIndex = it.homeIndex,
                        friendIndex = it.friendIndex,
                        letter = it.text.first(),
                        hintUsed = true,
                    )
                }
            }
        }
    }
}

sealed class LatestLevelEvent {
    data class Success(val oldLevel: Level, val currentLevel: Level) : LatestLevelEvent()
    data class Error(val exception: Throwable) : LatestLevelEvent()
}