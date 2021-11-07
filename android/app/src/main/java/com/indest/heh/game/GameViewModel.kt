package com.indest.heh.game

import android.app.Application
import androidx.lifecycle.*
import com.google.gson.Gson
import com.indest.heh.letter.Cell
import com.indest.heh.letter.Letter
import com.indest.heh.persistance.LevelProgress
import com.indest.heh.persistance.LevelRepository
import com.indest.heh.persistance.dto.Level
import com.indest.heh.persistance.dto.LevelData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val levelRepository: LevelRepository,
    application: Application
) :
    AndroidViewModel(application) {

    val levels = MutableLiveData<List<LevelData>>()
    val level = MutableLiveData<Level?>()
    val realSolutionAndGuess = MutableLiveData<Triple<String?, String?, Int?>>()

    init {
        val isEmpty = runBlocking { levelRepository.isDatabaseEmpty() }
        val string = if (isEmpty) {
            application.assets.open("puzzles.json").bufferedReader().use {
                it.readText()
            }
        } else ""
        viewModelScope.launch {
            if (isEmpty) {
                val gson = Gson()
                val puzzles = gson.fromJson(string, Array<LevelData>::class.java)
                insertLevels(puzzles)
            }
            level.value = levelRepository.getLevel(1)
            levels.value = levelRepository.getAllLevels()
        }

    }

    fun changeLevel(nr: Int) {
        viewModelScope.launch {
            level.value = levelRepository.getLevel(nr)
        }
    }

    fun isLastLevel():Boolean {
       return level.value?.levelInfo?.levelId == levels.value?.size
    }

    private fun insertLevels(levels: Array<LevelData>) {
        viewModelScope.launch {

            for (i in levels.indices) {
                val sol = levels[i].solution.replace(" ", "")
                val shuffledChars = sol.toCharArray()
                while (shuffledChars.concatToString() == sol) shuffledChars.shuffle()
                val puzzle = shuffledChars.concatToString()

                val category = levels[i].category
                val progress = LevelProgress.TODO
                val score = 0

                levelRepository.insertLevel(
                    LevelData(
                        levelId = i + 1,
                        solution = levels[i].solution,
                        puzzle = puzzle,
                        category = category,
                        progress = progress,
                        score = score
                    )
                )
            }

        }
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