package com.indest.heh.game

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.indest.heh.letter.Cell
import com.indest.heh.letter.Letter
import com.indest.heh.persistance.LevelProgress
import com.indest.heh.persistance.LevelRepository
import com.indest.heh.persistance.dto.Level
import com.indest.heh.persistance.dto.LevelData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
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
    val currentLevel: LiveData<Level> = levelRepository.getLastLevelTest().asLiveData()
    private val eventChannel = Channel<GameEvent>()
    val eventFlow = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            level.value = levelRepository.getLastLevel()
            levels.value = levelRepository.getAllLevels()
        }
    }

    fun changeLevel(nr: Int) {
        viewModelScope.launch {
            levelRepository.updateNextLevelTimestamp(currentLevel.value!!.levelInfo.levelId, nr)
            eventChannel.send(GameEvent.ChangeLevel(levelRepository.getLevel(nr)))
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
                levelRepository.isLettersInitialized(currentLevel.value!!.levelInfo.levelId)
            Log.i(TAG, "Is initialized: $isAlreadyInitialized")
            if (!isAlreadyInitialized) {
                cells.filterIsInstance<Letter>().forEach {
                    levelRepository.insertLetterInfo(
                        level = currentLevel.value!!.levelInfo.levelId,
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

sealed class GameEvent {
    data class ChangeLevel(val level: Level): GameEvent()
}
