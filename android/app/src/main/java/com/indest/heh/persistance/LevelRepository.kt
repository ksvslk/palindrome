package com.indest.oc.persistance

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LevelRepository @Inject constructor(private val levelDao: LevelDao) {
    suspend fun getLevel(nr:Int) = levelDao.get(nr)
    suspend fun getLastLevel() = levelDao.getLastLevel()
    suspend fun updateLevelPuzzle(level: Int, newPuzzle: String) =
        levelDao.updateLevelPuzzle(level, newPuzzle)
    suspend fun insertLetterInfo(level: Int, homeIndex:Int, friendIndex:Int, letter:Char) =
        levelDao.insertLetterInfo(level, homeIndex, friendIndex, letter)
    suspend fun deleteLetterInfo() = levelDao.deleteLetterInfo()
    suspend fun insertLevel(level: Level) = levelDao.insert(level)
    suspend fun deleteLevel(level: Level) = levelDao.delete(level)
    suspend fun switchHomeIndexes(index1:Int, index2:Int, level:Int) =
        levelDao.switchHomeIndexes(index1, index2, level)
    suspend fun updateFriendIndex(homeIndex:Int, newFriendIndex:Int, level: Int) =
        levelDao.updateFriendIndex(homeIndex, newFriendIndex, level)
    suspend fun updateLetter(homeIndex:Int, letter:Char, level: Int) =
        levelDao.updateLetter(homeIndex, letter, level)
}