package com.indest.oc.persistance

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LevelRepository @Inject constructor(private val levelDao: LevelDao) {
    suspend fun getAllLevels() = levelDao.getAllLevels()
    suspend fun getLevel(nr:Int) = levelDao.get(nr)
    suspend fun saveLevel(level: Level) = levelDao.update(level)
    suspend fun addNewLevel(level: Level) = levelDao.insert(level)
    suspend fun getLastLevel() = levelDao.getLastLevel()
}