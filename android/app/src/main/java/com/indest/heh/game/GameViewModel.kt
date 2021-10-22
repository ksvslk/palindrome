package com.indest.heh.game

import android.util.Log
import androidx.lifecycle.*
import com.indest.heh.letter.Cell
import com.indest.heh.letter.CellDirector
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
    private val levelRepository: LevelRepository
) :
    ViewModel() {

    val level: LiveData<Level> = liveData {
        temporaryLevelsInsertion()
        val level = levelRepository.getLastLevel()
        emit(level)
    }

    private fun temporaryLevelsInsertion() // TODO: temp
    {
        viewModelScope.launch {
            levelRepository.insertLevel(
                LevelData(1, "WOW", "WWO", "Surprise", LevelProgress.TODO, 0)
            )
            levelRepository.insertLevel(
                LevelData(2, "DAD", "ADD", "Parent", LevelProgress.TODO, 0)
            )
        }
    }

    fun updateLetter(index:Int, letter:Char)
    {
        viewModelScope.launch {
            levelRepository.updateLetter(
                homeIndex = index,
                letter = letter,
                level = level.value!!.levelInfo.levelId
            )
        }
    }
    fun updateFriendIndex(index:Int, newIndex:Int)
    {
        viewModelScope.launch {
            levelRepository.updateFriendIndex(
                homeIndex = index,
                newFriendIndex = newIndex,
                level = level.value!!.levelInfo.levelId
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