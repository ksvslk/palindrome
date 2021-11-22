package com.indest.heh.game

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.indest.heh.letter.Cell
import com.indest.heh.letter.Letter
import com.indest.heh.persistance.LevelRepository
import com.indest.heh.persistance.dto.Level
import com.indest.heh.persistance.dto.LevelData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayViewModel @Inject constructor(
    private val levelRepository: LevelRepository,
    application: Application
) :
    AndroidViewModel(application) {

    private var currentLevel:Level = Level()
    private var allLevels:List<LevelData> = emptyList()
    private val eventChannel = Channel<GameEvent>()
    val eventFlow = eventChannel.receiveAsFlow()

    init {
        startGame()
        viewModelScope.launch {
            allLevels = levelRepository.getAllLevels()
        }
    }

    fun startGame() = viewModelScope.launch {
        currentLevel = levelRepository.getLastLevel()
        eventChannel.send(GameEvent.StartGame(currentLevel))
    }

    fun loadLevel(nr:Int) = viewModelScope.launch {
        currentLevel = levelRepository.getLevel(nr)
        eventChannel.send(GameEvent.LoadLevel(currentLevel))
    }

    fun updateLetters(topIndex: Int, bottomLetter: Char, bottomIndex: Int, topLetter: Char) {
        Log.i("GMV", "updateLetters")
        viewModelScope.launch {
            levelRepository.updateLetter(
                homeIndex = topIndex,
                letter = bottomLetter,
                level = currentLevel.levelInfo.levelId
            )
            levelRepository.updateLetter(
                homeIndex = bottomIndex,
                letter = topLetter,
                level = currentLevel.levelInfo.levelId
            )

        }
    }

    fun updateFriendIndex(index: Int, newIndex: Int) {
        Log.i("GMV", "updateFriendIndex")
        viewModelScope.launch {
            levelRepository.updateFriendIndex(
                homeIndex = index,
                newFriendIndex = newIndex,
                level = currentLevel.levelInfo.levelId
            )
        }
    }

    fun persistInitialLettersInfo(cells: MutableList<Cell<*, *>>) {
        viewModelScope.launch {
            cells.filterIsInstance<Letter>().forEach {
                levelRepository.insertLetterInfo(
                    level = currentLevel.levelInfo.levelId,
                    homeIndex = it.homeIndex,
                    friendIndex = it.friendIndex,
                    letter = it.text.first(),
                    hintUsed = false
                )
            }
        }
    }

    fun getAllLevels() = allLevels
    fun getCurrentLevel() = currentLevel
}

sealed class GameEvent {
    data class StartGame(val level: Level) : GameEvent()
    data class LoadLevel(val level: Level) : GameEvent()
    data class Error(val exception: Throwable) : GameEvent()
}