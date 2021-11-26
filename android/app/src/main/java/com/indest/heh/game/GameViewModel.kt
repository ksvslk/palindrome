package com.indest.heh.game

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.indest.heh.letter.Cell
import com.indest.heh.letter.Letter
import com.indest.heh.persistance.LevelProgress
import com.indest.heh.persistance.LevelRepository
import com.indest.heh.persistance.dto.Level
import com.indest.heh.persistance.dto.LevelData
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
    val level = MutableLiveData<Level>()
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
            //_uiState.value = LatestLevelEvent.Success(_uiState.value.currentLevel,levelRepository.getLevel(nr))
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

    fun refreshLevel() {
        viewModelScope.launch {
            if (level.value != null) {
                level.value =
                    levelRepository.getLevel(_uiState.value.currentLevel.levelInfo.levelId)
            }
        }
    }

    fun isLastLevel(): Boolean {
        return level.value?.levelInfo?.levelId == levels.value?.size
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
                level.value?.levelInfo?.solution?.replace(" ", ""),
                levelRepository.currentSolution(level.value?.levelInfo?.levelId),
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
                level.value?.levelInfo?.solution?.replace(" ", ""),
                levelRepository.currentSolution(level.value?.levelInfo?.levelId),
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
                cells.filterIsInstance<Letter>().forEachIndexed { index, it ->
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