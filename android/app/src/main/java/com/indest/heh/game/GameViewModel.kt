package com.indest.heh.game

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.indest.heh.letter.Cell
import com.indest.heh.letter.Letter
import com.indest.heh.persistance.LevelProgress
import com.indest.heh.persistance.LevelRepository
import com.indest.heh.persistance.dto.Level
import com.indest.heh.persistance.dto.LevelData
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val level = MutableLiveData<Level>()
    val realSolutionAndGuess = MutableLiveData<Triple<String?, String?, Int?>>()

    val currentLevel = liveData {
        if (level.value != null) {
            Log.i(TAG, "Refresh level")
            emit(levelRepository.getLevel(level.value!!.levelInfo.levelId))
        } else {
            Log.i(TAG, "Load 1st level (not updated)")
            emit(levelRepository.getLevel(1))
        }
    }

    init {
        viewModelScope.launch {
            level.value = levelRepository.getLastLevel()
            levels.value = levelRepository.getAllLevels()
        }
    }

    fun changeLevel(nr: Int) {
        viewModelScope.launch {
            level.value = levelRepository.getLevel(nr)
        }
    }

    fun updateCompletedLevel(levelScore: Int = 1337) {
        viewModelScope.launch {
            levelRepository.resetLevelLettersInfo(level.value!!.levelInfo.levelId)
            val updatedLevel = level.value!!.levelInfo.copy(
                progress = LevelProgress.REPLAY,
                puzzle = level.value!!.levelInfo.puzzle,
                score = levelScore
            )
            levelRepository.updateLevel(updatedLevel)
        }
    }

    fun refreshLevel() {
        viewModelScope.launch {
            if (level.value != null) {
                level.value = levelRepository.getLevel(level.value!!.levelInfo.levelId)
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
                level = level.value!!.levelInfo.levelId
            )
            levelRepository.updateLetter(
                homeIndex = bottomIndex,
                letter = topLetter,
                level = level.value!!.levelInfo.levelId
            )

            realSolutionAndGuess.value = Triple(
                level.value?.levelInfo?.solution?.replace(" ", ""),
                levelRepository.currentSolution(level.value?.levelInfo?.levelId),
                topIndex
            )

        }
    }

    fun updateFriendIndex(index: Int, newIndex: Int) {
        Log.i("GMV", "updateFriendIndex")
        viewModelScope.launch {
            levelRepository.updateFriendIndex(
                homeIndex = index,
                newFriendIndex = newIndex,
                level = level.value!!.levelInfo.levelId
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
                levelRepository.isLettersInitialized(level.value!!.levelInfo.levelId)
            if (!isAlreadyInitialized) {
                cells.filterIsInstance<Letter>().forEach {

                    levelRepository.insertLetterInfo(
                        level = level.value!!.levelInfo.levelId,
                        homeIndex = it.homeIndex,
                        friendIndex = it.friendIndex,
                        letter = it.text.first(),
                        hintUsed = true
                    )
                }
            }
        }
    }
}
