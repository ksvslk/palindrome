package com.indest.hah

import android.content.Context
import androidx.room.Embedded
import androidx.room.Relation
import com.google.common.truth.Truth.assertThat
import com.indest.hah.persistance.dto.*
import com.indest.hah.persistance.level.LevelProgress
import com.indest.hah.savegame.SnapshotHelper
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import uk.co.jemos.podam.api.PodamFactory
import uk.co.jemos.podam.api.PodamFactoryImpl

@RunWith(MockitoJUnitRunner::class)
class SaveGameConvertTest {


    @Serializable
    data class LevelDataOld(
        val levelId: Int,
        var puzzle: String,
        val categoryRewarded: Boolean,
        val progress: LevelProgress,
        val score: Int,
        val best_score: Int,
        val timeModified: Long
    )


    @Serializable
    data class SaveGameOld(
        val game:List<LevelOld>,
        val interval:List<Interval>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SaveGameOld

            if (game != other.game) return false
            if (interval != other.interval) return false

            return true
        }

        override fun hashCode(): Int {
            var result = game.hashCode()
            result = 31 * result + interval.hashCode()
            return result
        }
    }

    @Serializable
    class LevelOld(
        @Embedded val levelInfo: LevelDataOld,
        @Relation(
            parentColumn = "level_id",
            entityColumn = "owner_id"
        )
        val lettersProperties: List<LetterProperties>,
    ):java.io.Serializable {
        constructor() : this(
            LevelDataOld(0,"_EMPTY_CONSTRUCTOR_",false, LevelProgress.IN_PROGRESS,0, 0, 0),
            emptyList()
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as LevelOld

            if (levelInfo != other.levelInfo) return false
            if (lettersProperties != other.lettersProperties) return false

            return true
        }

        override fun hashCode(): Int {
            var result = levelInfo.hashCode()
            result = 31 * result + lettersProperties.hashCode()
            return result
        }
    }


    private val TAG = "SaveGameConvertTest"
    @Mock
    private lateinit var mockContext: Context

    @Test
    fun saveGame_toBytes_toSaveGame_isEqualTo_initial_saveGame() {
        val snapshotHelper = SnapshotHelper()
        val randomSaveGame = getRandomSaveGame()
        val saveGameBytes = snapshotHelper.gameToBytes(randomSaveGame)
        val loadedSaveGame = snapshotHelper.bytesToGame(saveGameBytes)
        assertThat(randomSaveGame).isEqualTo(loadedSaveGame)
    }

    @Test
    fun saveGame_toBytes_toSaveGame_random_value_isEqualTo_initial_saveGame_random_value() {
        val snapshotHelper = SnapshotHelper()
        val randomSaveGame = getRandomSaveGame()
        val saveGameBytes = snapshotHelper.gameToBytes(randomSaveGame)
        val loadedSaveGame = snapshotHelper.bytesToGame(saveGameBytes)
        val randomLevelIndex = randomSaveGame.game.indexOf(randomSaveGame.game.random())
        assertThat(randomSaveGame.game[randomLevelIndex].levelInfo.puzzle).isEqualTo(loadedSaveGame.game[randomLevelIndex].levelInfo.puzzle)
    }

    @Test
    fun saveGameOld_toBytes_toSaveGameNew_isPossible() {
        val snapshotHelper = SnapshotHelper()
        val randomSaveGame = getRandomSaveGameOld()
        val saveGameBytes = gameToBytesOld(randomSaveGame)
        val s = snapshotHelper.bytesToGame(saveGameBytes)
        println(s.game[0].levelInfo)
    }

    private fun gameToBytesOld(saveGame: SaveGameOld): ByteArray {
        return Json.encodeToString(saveGame).toByteArray()
    }

    private fun getRandomSaveGameOld():SaveGameOld
    {
        val podamFactory: PodamFactory = PodamFactoryImpl()
        val levels = mutableListOf<LevelOld>()
        val intervals = mutableListOf<Interval>()

        for (i in 1..100)
        {
            val levelData = podamFactory.manufacturePojo(LevelDataOld::class.java)
            val lettersPropertiesList = mutableListOf<LetterProperties>()

            for (j in 1..5)
            {
                lettersPropertiesList.add(podamFactory.manufacturePojo(LetterProperties::class.java))
            }

            levels.add(
                LevelOld(levelData,lettersPropertiesList)
            )
        }

        for (i in 1..5)
        {
            intervals.add(podamFactory.manufacturePojo(Interval::class.java))
        }

        return SaveGameOld(game = levels, interval = intervals)
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
                Level(levelData,lettersPropertiesList,emptyList())
            )
        }

        for (i in 1..5)
        {
            intervals.add(podamFactory.manufacturePojo(Interval::class.java))
        }

        return SaveGame(game = levels, interval = intervals)
    }

}