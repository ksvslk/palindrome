package com.indest.heh

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.indest.heh.persistance.LevelDatabase
import com.indest.heh.persistance.LevelDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Before
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class LevelInfoDatabaseTest {

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

    }

    @Test
    @Throws(Exception::class)
    fun insertAndUpdateLevel() = testScope.runBlockingTest {

    }

    @Test
    @Throws(Exception::class)
    fun getAllLevels() = testScope.runBlockingTest {

    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }
}