package com.indest.hah

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.indest.hah.persistance.dto.*
import com.indest.hah.savegame.SnapshotHelper
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import uk.co.jemos.podam.api.PodamFactory
import uk.co.jemos.podam.api.PodamFactoryImpl


@RunWith(MockitoJUnitRunner::class)
class SaveGameConvertTest {

    private val TAG = "SaveGameConvertTest"
    @Mock
    private lateinit var mockContext: Context

    @Test
    fun saveGame_toBytes_toSaveGame_isEqualTo_initial_saveGame() {
        val snapshotHelper = SnapshotHelper(mockContext)
        val randomSaveGame = getRandomSaveGame()
        val saveGameBytes = snapshotHelper.gameToBytes(randomSaveGame)
        val loadedSaveGame = snapshotHelper.bytesToGame(saveGameBytes)
        assertThat(randomSaveGame).isEqualTo(loadedSaveGame)
    }

    @Test
    fun saveGame_toBytes_toSaveGame_random_value_isEqualTo_initial_saveGame_random_value() {
        val snapshotHelper = SnapshotHelper(mockContext)
        val randomSaveGame = getRandomSaveGame()
        val saveGameBytes = snapshotHelper.gameToBytes(randomSaveGame)
        val loadedSaveGame = snapshotHelper.bytesToGame(saveGameBytes)
        val randomLevelIndex = randomSaveGame.game.indexOf(randomSaveGame.game.random())
        assertThat(randomSaveGame.game[randomLevelIndex].levelInfo.puzzle).isEqualTo(loadedSaveGame.game[randomLevelIndex].levelInfo.puzzle)
    }

    private fun getRandomSaveGame():SaveGame
    {
        val podamFactory: PodamFactory = PodamFactoryImpl()
        val levels = mutableListOf<Level>()
        val intervals = mutableListOf<Interval>()

        for (i in 1..100)
        {
            val levelData = podamFactory.manufacturePojo(LevelData::class.java)
            val lettersPropertiesList = mutableListOf<LetterProperties>()

            for (j in 1..5)
            {
                lettersPropertiesList.add(podamFactory.manufacturePojo(LetterProperties::class.java))
            }

            levels.add(
                Level(
                    levelInfo = levelData,
                    lettersProperties = lettersPropertiesList
                )
            )
        }

        for (i in 1..5)
        {
            intervals.add(podamFactory.manufacturePojo(Interval::class.java))
        }

        return SaveGame(game = levels, interval = intervals)
    }

}