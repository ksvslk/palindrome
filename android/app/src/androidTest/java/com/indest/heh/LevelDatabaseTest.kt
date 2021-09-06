package com.indest.oc

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.indest.oc.persistance.Level
import com.indest.oc.persistance.LevelDatabase
import com.indest.oc.persistance.LevelDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class LevelDatabaseTest {

    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)

    private lateinit var levelDao: LevelDao
    private lateinit var db: LevelDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(
            context, LevelDatabase::class.java)
                .allowMainThreadQueries()
                .setTransactionExecutor(testDispatcher.asExecutor())
                .setQueryExecutor(testDispatcher.asExecutor())
                .build()
        levelDao = db.levelDao
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetLevel() = testScope.runBlockingTest {
            val level = Level(1,"WOW","WWO", arrayListOf(0,1,2,6,7,8))
            levelDao.insert(level)
            val insertedLevel = levelDao.get(1)
            assertEquals(insertedLevel.boxIndexes, arrayListOf(0,1,2,6,7,8))
    }

    @Test
    @Throws(Exception::class)
    fun insertAndUpdateLevel() = testScope.runBlockingTest {
        val level = Level(1,"DAD","DDA", arrayListOf(0,1,2,6,7,8))
        levelDao.insert(level)
        levelDao.update(Level(1,"DAD","DDA", arrayListOf(2,1,0,7,6,8)))
        val updatedLevel = levelDao.get(1)
        assertEquals(updatedLevel.boxIndexes, arrayListOf(2,1,0,7,6,8))
    }

    @Test
    @Throws(Exception::class)
    fun getAllLevels() = testScope.runBlockingTest {
        val level1 = Level(1,"DAD","DDA", arrayListOf(0,1,2,6,7,8))
        val level2 = Level(2,"LEVEL","LELEV", arrayListOf(0,1,2,6,7,8))
        val level3 = Level(3,"TACOCAT","TCAOACT", arrayListOf(0,1,2,6,7,8))
        levelDao.insert(level1)
        levelDao.insert(level2)
        levelDao.insert(level3)
        val allLevels = levelDao.getAllLevels()
        assertEquals(allLevels.size, 3)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }
}