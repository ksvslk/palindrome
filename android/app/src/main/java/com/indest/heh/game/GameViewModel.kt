package com.indest.heh.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.indest.heh.letter.Cell
import com.indest.heh.letter.Letter
import com.indest.heh.persistance.LevelProgress
import com.indest.heh.persistance.LevelRepository
import com.indest.heh.persistance.dto.LetterProperties
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

    val levels = MutableLiveData<List<LevelData>>()

    val level = MutableLiveData<Level>()

    val realSolutionAndGuess =  MutableLiveData<Triple<String?, String?, Int?>>()

    init {
        viewModelScope.launch {
            level.value = levelRepository.getLevel(1)
            levels.value = levelRepository.getAllLevels()
        }
    }

    fun changeLevel(nr: Int) {
        viewModelScope.launch {
            level.value = levelRepository.getLevel(nr)
        }
    }

    fun updateCurrentLevel(levelScore: Int = 1337)
    {
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

    fun isLastLevel():Boolean {
       return level.value?.levelInfo?.levelId == levels.value?.size
    }

    fun updateLetters(topIndex: Int, bottomLetter: Char, bottomIndex: Int, topLetter: Char) {
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
            cells.filterIsInstance<Letter>().forEach {
                levelRepository.insertLetterInfo(
                    level = level.value!!.levelInfo.levelId,
                    homeIndex = it.homeIndex,
                    friendIndex = it.friendIndex,
                    letter = it.text.first()
                )
            }
        }
    }

}