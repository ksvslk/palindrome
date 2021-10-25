package com.indest.heh.game

import androidx.lifecycle.*
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
    private val levelRepository: LevelRepository
) :
    ViewModel() {

    val level = MutableLiveData<Level>()
    val realSolutionAndGuess = MutableLiveData<Pair<String?, String?>>()

    init {
        viewModelScope.launch {
            levelRepository.insertLevel(
                LevelData(4, "RADAR", "RRAAD", "Surprise", LevelProgress.TODO, 0)
            )
            levelRepository.insertLevel(
                LevelData(3, "WOW", "WWO", "Surprise", LevelProgress.TODO, 0)
            )
            levelRepository.insertLevel(
                LevelData(2, "DAD", "ADD", "Parent", LevelProgress.TODO, 0)
            )
            levelRepository.insertLevel(
                LevelData(1, "A NUT FOR A JAR OF TUNA.", "O ARN UOF T RFN JA AAU.T", "Fly", LevelProgress.TODO, 0)
            )
            level.value = levelRepository.getLevel(1)
        }
    }

    fun updateLetters(topIndex:Int, bottomLetter:Char, bottomIndex:Int, topLetter:Char)
    {
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
            realSolutionAndGuess.value = Pair(
                level.value?.levelInfo?.solution?.replace(" ", ""),
                levelRepository.currentSolution(level.value?.levelInfo?.levelId)
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
            realSolutionAndGuess.value = Pair(
                level.value?.levelInfo?.solution?.replace(" ", ""),
                levelRepository.currentSolution(level.value?.levelInfo?.levelId)
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