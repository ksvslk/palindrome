package com.indest.hah.game.viewmodel

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.*
import com.indest.hah.R
import com.indest.hah.ads.RewardType
import com.indest.hah.general.AccountStatus
import com.indest.hah.other.C
import com.indest.hah.other.getCountryTagOrDefault
import com.indest.hah.other.getLevelScore
import com.indest.hah.persistance.dto.Interval
import com.indest.hah.persistance.dto.LanguageLevel
import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.persistance.dto.NewLevelNotification
import com.indest.hah.persistance.dto.PlayerLevel
import com.indest.hah.persistance.dto.RegularLevel
import com.indest.hah.persistance.dto.SaveGame
import com.indest.hah.persistance.level.LevelProgress
import com.indest.hah.persistance.level.LevelRepository
import dagger.hilt.android.internal.Contexts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import java.util.Arrays
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max


@HiltViewModel
class LevelViewModel @Inject constructor(
    private val levelRepository: LevelRepository,
    application: Application
) :
    AndroidViewModel(application) {

    private val TAG = "LevelViewModel"

    private val _accountStatus = MutableStateFlow(AccountStatus.NOT_SET)
    val accountStatus = _accountStatus.asStateFlow()

    private val _pictureHintDownloaded = MutableStateFlow(false)
    val pictureHintDownloaded = _pictureHintDownloaded.asStateFlow()

    private val _currentLevel = MutableStateFlow(Level())
    val currentLevel = _currentLevel.asStateFlow()

    private val gameEventChannel = Channel<GameEvent>()
    val gameEventFlow = gameEventChannel.receiveAsFlow()

    private val variousEventChannel = Channel<VariousEvent>()
    val variousEventFlow = variousEventChannel.receiveAsFlow()

    private val submissionChannel = Channel<SubmissionEvent>()
    val submissionEventFlow = submissionChannel.receiveAsFlow()

    private val firebaseAnalytics = FirebaseAnalytics.getInstance(application.applicationContext)
    private val fsDb = FirebaseFirestore.getInstance()
    private var liveStoreRegistration: ListenerRegistration? = null

    private val _newPlayerLevels =
        MutableStateFlow(NewLevelNotification(-1, -1, "", ""))
    val newPlayerLevels = _newPlayerLevels.asStateFlow()

    var currentlySignedInPlayerName = "???"
    var tempSubmissionPalindrome = ""
    var tempCountrySelected = Pair("gb", 0)
    var currentRelativeLevelNumber = -1

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(
            TAG,
            "Error syncing cloud with local. Start collecting level from  local database without up to date information.",
            throwable
        )
        startCollectingLevelUpdate()
    }

    fun updateColumnCount(newCols: Int) {
        viewModelScope.launch {
            levelRepository.updateColumnCount(newCols, currentLevel.value.levelInfo.levelId)
        }
    }

    private fun startCollectingLevelUpdate() {
        viewModelScope.launch {
            val tag = getCountryTagOrDefault()
            levelRepository.matchFirstLevelWithUserLocale(tag)
            levelRepository.latestLevelFlow().collect {
                val changeInLevelId = it.levelInfo.levelId != _currentLevel.value.levelInfo.levelId
                _currentLevel.value = it
                if (isCurrentLevelLegit() && (changeInLevelId)) {
                    val progress = getProgressAndText()
                    gameEventChannel.send(GameEvent.DisplayLevel(_currentLevel.value, progress))
                }
            }
        }
    }


    private fun isCurrentLevelLegit() =
        _currentLevel.value.levelInfo.levelId > 0 &&
                _currentLevel.value.solutions.isNotEmpty() &&
                _currentLevel.value.levelInfo.puzzle != ""


    private suspend fun getProgressAndText(): Pair<Int, String> {
        val levelId = _currentLevel.value.levelInfo.levelId
        val author = _currentLevel.value.levelInfo.author
        val solutionsCount = _currentLevel.value.solutions.count()

        var result = Pair(-1, "")
        // Game levels or player levels
        if (levelId in 1001..9999) {
            val maxLevels = levelRepository.getMaxLevelId() - 1000
            val text =
                if (solutionsCount > 1) "${levelId - 1000}* - $author" else "${levelId - 1000} - $author"
            val progress = ((levelId - 1000).toFloat() / maxLevels.toFloat() * 100.0).toInt()
            result = Pair(progress, text)
            currentRelativeLevelNumber = levelId
        } else if (levelId > 10000) {
            val language = _currentLevel.value.levelInfo.countryCode
            val levels = levelRepository.getLevelsForCountry(language)
            val levelsCount = levels.count()
            val levelShownInUi =
                levels.indexOfFirst { levelData -> levelData.levelId == levelId } + 1
            val authorIfExists = if (author.isNotEmpty()) " - $author" else ""
            val text =
                if (solutionsCount > 1) "$levelShownInUi*$authorIfExists" else "$levelShownInUi$authorIfExists"
            val progress = (levelShownInUi.toFloat() / levelsCount.toFloat() * 100.0).toInt()
            result = Pair(progress, text)
            currentRelativeLevelNumber = levelShownInUi
        } else {
            val levelsCount = levelRepository.getMaxGameLevelId()
            val text = if (solutionsCount > 1) "$levelId*" else "$levelId"
            val progress = (levelId / levelsCount.toFloat() * 100.0).toInt()
            result = Pair(progress, text)
            currentRelativeLevelNumber = levelId
        }
        return result
    }


    private val saveGameExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Could not write snapshot or save game: ${throwable.message}")
    }

    fun setPictureDownloaded(isPresent: Boolean) {
        _pictureHintDownloaded.value = isPresent
    }

    fun setAccountStatus(accountStatus: AccountStatus) {
        _accountStatus.value = accountStatus
        viewModelScope.launch {
            when (accountStatus) {
                AccountStatus.SIGNED_IN -> {
                    variousEventChannel.send(VariousEvent.LoadGame)
                }

                AccountStatus.SIGNED_OUT -> {

                    if (!isCurrentLevelLegit()) {
                        startCollectingLevelUpdate()
                    }
                }

                AccountStatus.LOADING -> {
                    if (!isCurrentLevelLegit()) {
                        gameEventChannel.send(GameEvent.ShowLoading)
                    } else {
                        val progress = getProgressAndText()
                        gameEventChannel.send(GameEvent.DisplayLevel(_currentLevel.value, progress))
                    }
                }

                AccountStatus.SIGNED_IN_WITH_FIREBASE -> {
                    if (_accountStatus.value != AccountStatus.SIGNED_IN) {
                        variousEventChannel.send(VariousEvent.LoadGame)
                    }
                }

                AccountStatus.NOT_SET -> {}
            }
        }
    }


    init {
        /*
        // Initial db creation
        viewModelScope.launch {
            addRegularLevels()
            addPlayerLevelsUntil1080()
            addLanguagePacks()
            addIntervals()
        }
         */
    }

    suspend fun isShowScores(): Boolean {
        val showScoresString =
            getApplication<Application>().getString(R.string.player_centered_scores)
        val limitReached = levelRepository.isLimitAttained(showScoresString)
        if (limitReached) resetIntervalCount(showScoresString)
        return limitReached
    }


    suspend fun isLastLevel(): Triple<Boolean, Boolean, String> {
        val lastGameLevel =
            levelRepository.isFinalLevel(_currentLevel.value.levelInfo.levelId, 1, 100)
        val lastPlayerLevel =
            levelRepository.isFinalLevel(_currentLevel.value.levelInfo.levelId, 1001, 10000)
        val isLanguageLevelFinal = levelRepository.isLanguagePackFinalLevel(
            _currentLevel.value.levelInfo.levelId,
            currentLevel.value.levelInfo.countryCode
        )
        val finalLevelInCountryCode =
            if (isLanguageLevelFinal) _currentLevel.value.levelInfo.countryCode else ""
        return Triple(lastGameLevel == 1, lastPlayerLevel == 1, finalLevelInCountryCode)
    }


    fun loadLatestLevelFromCloudOrLocal(saveGame: SaveGame?) {
        viewModelScope.launch(exceptionHandler) {
            if (saveGame != null) {
                levelRepository.updateLocalGame(saveGame)
                startCollectingLevelUpdate()
            } else {
                startCollectingLevelUpdate()
            }
            launch {
                subscribeToRealTimePlayerLevelUpdates()
            }
        }
    }


    fun updateLetters(topIndex: Int, bottomLetter: Char, bottomIndex: Int, topLetter: Char) {
        viewModelScope.launch {
            _currentLevel.value.levelInfo.let { levelInfo ->
                val currentSolution = levelRepository.switchLetters(
                    homeIndex1 = topIndex,
                    homeIndex2 = bottomIndex,
                    letter1 = bottomLetter,
                    letter2 = topLetter,
                    level = levelInfo.levelId
                )
                updateSolutionState(topIndex, currentSolution)
            }
        }
    }


    fun increaseMistakes() {
        viewModelScope.launch {
            _currentLevel.value.levelInfo.let { levelInfo ->
                val pointsToSubtract =
                    if (levelInfo.levelId <= 100) levelInfo.levelId * 100 / 20 else levelInfo.puzzle.length * 100 / 20
                val newScore = if (levelInfo.score - pointsToSubtract < 0) 0
                else levelInfo.score - pointsToSubtract
                levelRepository.updateScore(newScore, levelInfo.levelId)
            }
        }
    }

    fun setLetterLocked(index: Int) {
        viewModelScope.launch {
            _currentLevel.value.levelInfo.let { levelInfo ->
                levelRepository.setLetterLocked(
                    homeIndex = index,
                    level = levelInfo.levelId
                )
                saveGameImmediately()
            }
        }
    }


    fun updateFriendIndex(index: Int, newIndex: Int) {
        viewModelScope.launch {
            _currentLevel.value.levelInfo.let { levelInfo ->
                val currentSolution = levelRepository.updateFriendIndex(
                    homeIndex = index,
                    newFriendIndex = newIndex,
                    level = levelInfo.levelId
                )
                updateSolutionState(index, currentSolution)
            }
        }
    }

    fun resetLetterInfo(levelNr: Int) {
        viewModelScope.launch {
            levelRepository.resetLetterInfo(levelNr)
        }
    }

    fun changeLevel(chosenLevel: Int) {
        if (_currentLevel.value.levelInfo.progress == LevelProgress.INTERMEDIATE) {
            executeLevelCompleteActions(chosenLevel)
        } else {
            viewModelScope.launch {
                _currentLevel.value.levelInfo.let { levelInfo ->
                    val hintInterval =
                        getApplication<Application>().getString(R.string.hint_suggestion)
                    levelRepository.chooseLevelTransaction(
                        levelInfo.levelId,
                        chosenLevel,
                        hintInterval
                    )
                }
            }
        }
    }


    fun resetIntervalCount(interval: String) {
        viewModelScope.launch {
            levelRepository.setCountTo(interval, 0)
        }
    }

    fun executeLevelCompleteActions(
        changeLevelTo: Int = _currentLevel.value.levelInfo.levelId + 1,
        justNext: Boolean = false
    ) {
        val levelNr = _currentLevel.value.levelInfo.levelId

        val hintInterval = getApplication<Application>().getString(R.string.hint_suggestion)
        viewModelScope.launch {
            val levelToLoad = if (justNext) levelRepository.getNextLevelForLanguageLevel(
                levelNr,
                _currentLevel.value.levelInfo.countryCode
            )!! else changeLevelTo
            levelRepository.executeLevelCompleteTransaction(levelNr, hintInterval, levelToLoad)
            levelRepository.increaseCount(getApplication<Application>().getString(R.string.sad_mode_interval))
            levelRepository.increaseCount(getApplication<Application>().getString(R.string.player_centered_scores))
            if (levelToLoad - _currentLevel.value.levelInfo.levelId == 1) saveGameImmediately()
            logLevelStart()
        }
    }

    fun allPalindromesFlow(): Flow<Map<String,List<String>>> {
        return flow {
            emit(levelRepository.getAllPalindromes())
        }
    }

    fun allPlayerLevelsFlow(): Flow<List<Pair<Boolean, LevelData>>> {
        return flow {
            emit(levelRepository.getAllLevels().filter { it.second.levelId in 1000..9999 })
        }
    }

    fun countOfNewPlayerLevelsAndLanguageLevels(): Flow<Pair<Int, Int>> {
        return flow {
            emit(
                Pair(
                    levelRepository.getAllLevels().count {
                        it.second.progress == LevelProgress.TODO &&
                                it.second.timeModified == 0L &&
                                it.second.levelId in 1080..9999
                    },
                    levelRepository.getAllLevels().count {
                        it.second.timeModified == 0L &&
                                it.second.levelId > 10000 &&
                                it.second.author != "" &&
                                it.second.countryCode != "gb" &&
                                it.second.countryCode != ""
                    })
            )
        }
    }



    fun allGameLevelsFlow(): Flow<List<Pair<Boolean, LevelData>>> {
        return flow {
            emit(levelRepository.getAllLevels().filter { it.second.levelId <= 100 })
        }
    }

    fun getLevelsForCountry(cc: String): Flow<List<LevelData>> {
        return flow {
            emit(levelRepository.getLevelsForCountry(cc))
        }
    }

    fun allUniqueCountries(): Flow<List<String>> {
        return flow {
            emit(levelRepository.getUniqueCountries())
        }
    }


    fun allUniqueCountriesWithLevelCount(): Flow<List<Triple<String, Int, Int?>>> {
        return flow {
            emit(levelRepository.getUniqueCountriesWithNewLevelCount())
        }
    }

    fun saveGameImmediately() {
        viewModelScope.launch(saveGameExceptionHandler) {
            if (_accountStatus.value in setOf(
                    AccountStatus.SIGNED_IN,
                    AccountStatus.SIGNED_IN_WITH_FIREBASE
                )
            ) {
                val saveGame = levelRepository.getLocalSaveGame()
                val calculatedGameProgress = levelRepository.getMaximumTimestamp()
                variousEventChannel.send(VariousEvent.SaveGame(saveGame, calculatedGameProgress))
            }
        }
    }


    private fun submitScore() {
        viewModelScope.launch {
            if (_accountStatus.value in setOf(
                    AccountStatus.SIGNED_IN,
                    AccountStatus.SIGNED_IN_WITH_FIREBASE
                )
            ) {
                val totalScore = levelRepository.getTotalUpdatedScore(
                    level = _currentLevel.value.levelInfo.levelId,
                    countryCode = _currentLevel.value.levelInfo.countryCode
                )
                variousEventChannel.send(
                    VariousEvent.SubmitScore(
                        score = totalScore,
                        playerLevels = _currentLevel.value.levelInfo.levelId in 1001..9999,
                        languageLevels = _currentLevel.value.levelInfo.levelId > 10000,
                        countryCode = _currentLevel.value.levelInfo.countryCode
                    )
                )
            }
        }
    }


    private fun unlockAchievement(achievement: String) {
        viewModelScope.launch {
            if (_accountStatus.value in setOf(
                    AccountStatus.SIGNED_IN,
                    AccountStatus.SIGNED_IN_WITH_FIREBASE
                )
            ) {
                variousEventChannel.send(VariousEvent.UnlockAchievement(achievement))
            }
        }
    }


    sealed class GameEvent {
        data class DisplayLevel(val level: Level, val progress: Pair<Int, String>) : GameEvent()
        data class SolutionResult(
            val solutionData: SolutionData,
            val isLastLevel: Triple<Boolean, Boolean, String>
        ) : GameEvent()

        object ShowLoading : GameEvent()
    }


    sealed class VariousEvent {
        data class UnlockAchievement(val achievement: String) : VariousEvent()
        data class SubmitScore(
            val score: Long,
            val playerLevels: Boolean,
            val languageLevels: Boolean,
            val countryCode: String
        ) : VariousEvent()

        data class SaveGame(
            val localSaveGame: com.indest.hah.persistance.dto.SaveGame,
            val calculatedGameProgress: Long
        ) :
            VariousEvent()

        object LoadGame : VariousEvent()
    }


    sealed class SubmissionEvent {
        object Succeeded : SubmissionEvent()
        object Failed : SubmissionEvent()
    }


    private fun updateAndConsiderFlawlessAchievements(
        solutionData: SolutionData
    ) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            if (solutionData.score == solutionData.maxScore) {
                if (levelRepository.increaseCountAndGetResult(context.getString(R.string.achievement_flawless_5))) {
                    unlockAchievement(context.getString(R.string.achievement_flawless_5))
                }
                if (levelRepository.increaseCountAndGetResult(context.getString(R.string.achievement_flawless_15))) {
                    unlockAchievement(context.getString(R.string.achievement_flawless_15))
                }
            } else {
                levelRepository.setCountTo(context.getString(R.string.achievement_flawless_5), 0)
                levelRepository.setCountTo(context.getString(R.string.achievement_flawless_15), 0)
            }
        }
    }


    private fun resolveLevelCompleteAchievements(solutionData: SolutionData) {
        val context = getApplication<Application>()

        // Flawless 5 and Flawless 15 achievements
        updateAndConsiderFlawlessAchievements(solutionData)

        // Zero mistakes achievement
        if (_currentLevel.value.levelInfo.levelId in 21..100 &&
            solutionData.score == solutionData.maxScore
        ) {
            unlockAchievement(context.getString(R.string.achievement_zero_mistakes))
        }

        // Complete Level achievement
        if (_currentLevel.value.solutions.any { it.palindrome == "LEVEL" }) {
            unlockAchievement(context.getString(R.string.achievement_complete_level))
        }

        // Greenhorn, Proficient, Crackerjack, Chief and Authority achievements
        when (_currentLevel.value.levelInfo.levelId) {
            20 -> unlockAchievement(context.getString(R.string.achievement_greenhorn))
            40 -> unlockAchievement(context.getString(R.string.achievement_proficient))
            60 -> unlockAchievement(context.getString(R.string.achievement_crackerjack))
            80 -> unlockAchievement(context.getString(R.string.achievement_chief))
            100 -> unlockAchievement(context.getString(R.string.achievement_authority))
        }
        unlockRelativeLanguageLevelAchievement()
    }

    private fun unlockRelativeLanguageLevelAchievement() {
        viewModelScope.launch {
            val res = getApplication<Application>().applicationContext.resources
            if (currentRelativeLevelNumber == 10) {
                when (_currentLevel.value.levelInfo.countryCode) {
                    "cn" -> unlockAchievement(res.getString(R.string.achievement_chinese_linguist))
                    "de" -> unlockAchievement(res.getString(R.string.achievement_german_linguist))
                    "es" -> unlockAchievement(res.getString(R.string.achievement_spanish_linguist))
                    "et" -> unlockAchievement(res.getString(R.string.achievement_estonian_linguist))
                    "fi" -> unlockAchievement(res.getString(R.string.achievement_finnish_linguist))
                    "fr" -> unlockAchievement(res.getString(R.string.achievement_french_linguist))
                    "id" -> unlockAchievement(res.getString(R.string.achievement_indonesian_linguist))
                    "it" -> unlockAchievement(res.getString(R.string.achievement_italian_linguist))
                    "ja" -> unlockAchievement(res.getString(R.string.achievement_japanese_linguist))
                    "ko" -> unlockAchievement(res.getString(R.string.achievement_korean_linguist))
                    "nl" -> unlockAchievement(res.getString(R.string.achievement_dutch_linguist))
                    "pl" -> unlockAchievement(res.getString(R.string.achievement_polish_linguist))
                    "pt" -> unlockAchievement(res.getString(R.string.achievement_portuguese_linguist))
                    "ro" -> unlockAchievement(res.getString(R.string.achievement_romanian_linguist))
                    "sv" -> unlockAchievement(res.getString(R.string.achievement_swedish_linguist))
                    "tr" -> unlockAchievement(res.getString(R.string.achievement_turkish_linguist))
                    "uk" -> unlockAchievement(res.getString(R.string.achievement_ukrainian_linguist))
                }
            }
        }

    }


    private fun isSolutionCorrect(guess: String): Boolean {
        return _currentLevel.value.solutions.any {
            it.palindrome.replace(Regex("[\\s-]+"), "") == guess
        }
    }


    private fun updateSolutionState(index: Int, guess: String) {
        viewModelScope.launch {
            val solution =
                _currentLevel.value.solutions.first().palindrome.replace(Regex("[\\s-]+"), "")
            var solutionData = SolutionData()
            if (guess.length == solution.length) {
                val playerGuess = guess.map { char ->
                    if ("^([?!.,’':-]–)".contains(char)) "" else char
                }.joinToString("")

                val hintsInterval =
                    getApplication<Application>().getString(R.string.hint_suggestion)

                val maxScore = _currentLevel.value.levelInfo.levelId.getLevelScore(
                    _currentLevel.value.levelInfo.puzzle
                )

                when {
                    isSolutionCorrect(guess) -> {
                        submitScore()
                        levelRepository.updateLevelProgress(
                            _currentLevel.value.levelInfo.levelId,
                            LevelProgress.INTERMEDIATE
                        )
                        solutionData = SolutionData(
                            solution = Solution.CORRECT,
                            index = index,
                            score = _currentLevel.value.levelInfo.score,
                            maxScore = maxScore,
                            showHintsBadge = false
                        )
                        resolveLevelCompleteAchievements(solutionData)
                        levelRepository.setCountTo(hintsInterval, 0)
                        logLevelEnd()
                    }

                    playerGuess.reversed() == playerGuess -> {
                        val showHintsBadge =
                            levelRepository.increaseCountAndGetResult(hintsInterval)
                        solutionData = SolutionData(
                            solution = Solution.BOGUS_PALINDROME,
                            index = index,
                            score = _currentLevel.value.levelInfo.score,
                            maxScore = maxScore,
                            showHintsBadge = showHintsBadge
                        )
                        // Wrong Palindrome achievement
                        unlockAchievement(getApplication<Application>().getString(R.string.achievement_wrong_palindrome))
                    }

                    else -> {
                        val showHintsBadge =
                            levelRepository.increaseCountAndGetResult(hintsInterval)
                        solutionData = SolutionData(
                            solution = Solution.WRONG,
                            index = index,
                            score = _currentLevel.value.levelInfo.score,
                            maxScore = maxScore,
                            showHintsBadge = showHintsBadge
                        )
                    }
                }
            }
            val isLastLevel = isLastLevel()
            gameEventChannel.send(GameEvent.SolutionResult(solutionData, isLastLevel))
        }
    }

    fun getCurrentHintInLocale(): String {
        val currentLocaleName = if (!AppCompatDelegate.getApplicationLocales().isEmpty) {
            // Fetches the current Application Locale from the list
            AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag()
        } else {
            // Fetches the default System Locale
            Locale.getDefault().toLanguageTag()
        }?.substring(0, 2)
        return when (currentLocaleName) {
            "en" -> _currentLevel.value.solutions.first().hint
            "zh" -> _currentLevel.value.solutions.first().hint_cn
            "de" -> _currentLevel.value.solutions.first().hint_de
            "es" -> _currentLevel.value.solutions.first().hint_es
            "et" -> _currentLevel.value.solutions.first().hint_et
            "fi" -> _currentLevel.value.solutions.first().hint_fi
            "fr" -> _currentLevel.value.solutions.first().hint_fr
            "in" -> _currentLevel.value.solutions.first().hint_id // note
            "id" -> _currentLevel.value.solutions.first().hint_id // same country
            "it" -> _currentLevel.value.solutions.first().hint_it
            "ja" -> _currentLevel.value.solutions.first().hint_ja
            "ko" -> _currentLevel.value.solutions.first().hint_ko
            "nl" -> _currentLevel.value.solutions.first().hint_nl
            "pl" -> _currentLevel.value.solutions.first().hint_pl
            "pt" -> _currentLevel.value.solutions.first().hint_pt
            "ro" -> _currentLevel.value.solutions.first().hint_ro
            "sv" -> _currentLevel.value.solutions.first().hint_sv
            "tr" -> _currentLevel.value.solutions.first().hint_tr
            "uk" -> _currentLevel.value.solutions.first().hint_uk
            else -> {
                _currentLevel.value.solutions.first().hint + "*"
            }
        }
    }

    private fun logLevelStart() {
        viewModelScope.launch(Dispatchers.Default) {
            firebaseAnalytics.logEvent(
                FirebaseAnalytics.Event.LEVEL_START, bundleOf(
                    Pair(
                        FirebaseAnalytics.Param.LEVEL,
                        _currentLevel.value.levelInfo.levelId
                    ),
                    Pair(
                        FirebaseCustom.Params.LEVEL_SOLUTION,
                        _currentLevel.value.solutions.first().palindrome
                    )
                )
            )
        }
    }


    private fun logLevelEnd() {
        viewModelScope.launch(Dispatchers.Default) {
            val maxScore =
                _currentLevel.value.levelInfo.levelId.getLevelScore(_currentLevel.value.levelInfo.puzzle)
            val scoreToMaxRatio = (_currentLevel.value.levelInfo.score / maxScore).toFloat()
            val parameters = Bundle().apply {
                this.putInt(FirebaseAnalytics.Param.LEVEL, _currentLevel.value.levelInfo.levelId)
                this.putFloat(FirebaseCustom.Params.SCORE_TO_MAX_RATIO, scoreToMaxRatio)
                this.putString(
                    FirebaseCustom.Params.LEVEL_SOLUTION,
                    _currentLevel.value.solutions.first().palindrome
                )
            }
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LEVEL_END, parameters)
        }
    }


    fun logTutorialBegin() {
        viewModelScope.launch(Dispatchers.Default) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.TUTORIAL_BEGIN, bundleOf())
        }
    }


    fun logTutorialComplete() {
        viewModelScope.launch(Dispatchers.Default) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.TUTORIAL_COMPLETE, bundleOf())
        }
    }


    fun logPremiumHintUsage(hintType: RewardType) {
        viewModelScope.launch(Dispatchers.Default) {
            val currentLevel = _currentLevel.value.levelInfo.levelId
            val currentSolution = levelRepository.getCurrentSolution(currentLevel)

            firebaseAnalytics.logEvent(
                FirebaseCustom.Events.PREMIUM_HINT_USAGE,
                bundleOf(
                    Pair(FirebaseCustom.Params.HINT_TYPE, hintType.name),
                    Pair(FirebaseAnalytics.Param.LEVEL, currentLevel),
                    Pair(FirebaseCustom.Params.CURRENT_SOLUTION, currentSolution),
                    Pair(
                        FirebaseCustom.Params.LEVEL_SOLUTION,
                        _currentLevel.value.solutions.first().palindrome
                    ),
                    Pair(FirebaseAnalytics.Param.VALUE, 1),
                )
            )
        }
    }

    fun logRewardGiven(rewardType: RewardType) {
        viewModelScope.launch(Dispatchers.Default) {
            val currentLevel = _currentLevel.value.levelInfo.levelId
            val currentSolution = levelRepository.getCurrentSolution(currentLevel)
            firebaseAnalytics.logEvent(
                FirebaseCustom.Events.REWARD_GIVEN,
                bundleOf(
                    Pair(FirebaseCustom.Params.REWARD_AD_TYPE, rewardType.name),
                    Pair(FirebaseAnalytics.Param.LEVEL, currentLevel),
                    Pair(FirebaseCustom.Params.CURRENT_SOLUTION, currentSolution),
                    Pair(
                        FirebaseCustom.Params.LEVEL_SOLUTION,
                        _currentLevel.value.solutions.first().palindrome
                    ),
                    Pair(FirebaseAnalytics.Param.VALUE, 1),
                )
            )
        }
    }


    private suspend fun subscribeToRealTimePlayerLevelUpdates() {
        if (_accountStatus.value == AccountStatus.SIGNED_IN_WITH_FIREBASE) {
            val maxPlayerLevelId = levelRepository.getMaxPlayerLevelId() ?: 1080
            val maxLevelId = levelRepository.getMaxLevelId()
            val query = fsDb.collection("user_submitted_levels").where(
                Filter.and(
                    Filter.equalTo(C.PLAYER_LEVELS_TAG, true),
                    Filter.or(
                        // player levels 1000+
                        Filter.and(
                            Filter.greaterThan("levelId", 1080),
                            Filter.greaterThan("levelId", maxPlayerLevelId),
                            Filter.lessThan("levelId", 10000)
                        ),
                        // language levels 10000+
                        Filter.and(
                            Filter.greaterThan("levelId", 10000),
                            Filter.greaterThan("levelId", maxLevelId),
                        )
                    )
                )
            )
            liveStoreRegistration = query.addSnapshotListener(EventListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@EventListener
                }
                val playerLevels = mutableListOf<PlayerLevel>()
                snapshots?.let { snap ->
                    for (dc in snap.documentChanges) {
                        when (dc.type) {
                            DocumentChange.Type.ADDED -> {
                                try {
                                    dc.document.data.let { levelValues ->
                                        val levelId = levelValues["levelId"] as Long
                                        if (levelId > 1080) {
                                            val solutionsToCategories =
                                                levelValues["solutionToCategory"] as Map<String, Map<String, String>>
                                            if (solutionsToCategories.isEmpty()) {
                                                throw Exception("Solutions were empty")
                                            }
                                            val supportedHints = C.languages.keys.map { "hint_$it" }.plus("hint")
                                            if (!solutionsToCategories.keys.all {
                                                    solutionsToCategories[it]?.keys?.containsAll(supportedHints) == true
                                                }) {
                                                throw Exception("Solutions did not include hints for all languages.")
                                            }
                                            val countryCode =
                                                levelValues.getOrElse("countryCode") { "gb" }
                                            val playerLevel = PlayerLevel(
                                                levelId = levelValues["levelId"] as Long,
                                                player = levelValues["player"] as String,
                                                solutionToCategory = solutionsToCategories,
                                                countryCode = countryCode as String
                                            )
                                            playerLevels.add(playerLevel)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.w(
                                        TAG,
                                        "Something went wrong when adding the user level from Firebase.",
                                        e
                                    )
                                    return@EventListener
                                }
                            }

                            DocumentChange.Type.MODIFIED -> {
                            }

                            DocumentChange.Type.REMOVED -> {
                            }
                        }
                    }
                }
                viewModelScope.launch {
                    if (playerLevels.size != 0) {
                        // Get Pair where first element is a level when only 1 level added
                        // Second element is the level count when there are more than 1 level added.
                        val levelAndCount = insertPlayerLevels(playerLevels)
                        if (levelAndCount.second > 0) {
                            if (levelAndCount.second > 1) {
                                _newPlayerLevels.update {
                                    NewLevelNotification(
                                        -1,
                                        levelAndCount.second,
                                        "",
                                        ""
                                    )
                                }
                            } else {
                                val isLanguagePackComplete =
                                    if (levelAndCount.first.countryCode != "gb" && levelAndCount.first.countryCode != "") {
                                        levelRepository.isLanguagePackCompleted(levelAndCount.first.countryCode)
                                    } else true
                                _newPlayerLevels.update {
                                    NewLevelNotification(
                                        levelAndCount.first.levelId,
                                        1,
                                        levelAndCount.first.author,
                                        levelAndCount.first.countryCode,
                                        showPlayButton = isLanguagePackComplete
                                    )
                                }
                            }
                        }
                    }
                }
            })
        }
    }


    fun resetNewPlayersLevels() {
        _newPlayerLevels.value = NewLevelNotification(-1, -1, "", "")
    }


    private suspend fun insertPlayerLevels(playerLevels: List<PlayerLevel>): Pair<LevelData, Int> {
        return levelRepository.insertPlayerLevels(playerLevels)
    }

    suspend fun submitUserLevel(playersPalindrome: String) {
        val hintMap =  C.languages.map { "hint_${it.key}" to "[FILL_HERE]" }.plus("hint" to "[FILL_HERE]").toMap()
        // Create a new user with a first, middle, and last name
        val level = hashMapOf(
            C.PLAYER_LEVELS_TAG to false,
            "levelId" to -1337,
            "palindrome" to playersPalindrome,
            "player" to currentlySignedInPlayerName,
            "solutionToCategory" to hashMapOf(
                Pair(playersPalindrome, hintMap)
            ),
            "countryCode" to tempCountrySelected.first,
            "timestamp" to FieldValue.serverTimestamp(),
        )

        // Add a new document with a generated ID
        fsDb.collection("user_submitted_levels")
            .add(level)
            .addOnSuccessListener { documentReference ->
                viewModelScope.launch {
                    levelRepository.updateSubmissionTimestamp()
                    submissionChannel.send(SubmissionEvent.Succeeded)
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
                viewModelScope.launch {
                    submissionChannel.send(SubmissionEvent.Failed)
                }
            }
    }

    fun isAllowedToSubmitAndSignedInToFirebase(): Boolean {
        return _accountStatus.value == AccountStatus.SIGNED_IN_WITH_FIREBASE
    }

    private suspend fun addLanguagePacks() {
        if (levelRepository.getUniqueCountries().isEmpty()) {
            C.languages.entries.forEach { entry ->
                val cc = entry.key
                val jsonInstance = Json { isLenient = true }
                val json =
                    getApplication<Application>().applicationContext.assets.open("language_pack_$cc.json")
                        .bufferedReader().use {
                            it.readText()
                        }
                val languageLevels = jsonInstance.decodeFromString<List<LanguageLevel>>(json)
                levelRepository.insertLanguageLevelPack(languageLevels, cc)
            }
        }
    }

    private suspend fun addRegularLevels() {
        val jsonInstance = Json { isLenient = true }
        val json =
            getApplication<Application>().applicationContext.assets.open("level_pack_1_modified.json")
                .bufferedReader().use {
                    it.readText()
                }
        val regularLevels = jsonInstance.decodeFromString<List<RegularLevel>>(json)
        levelRepository.insertRegularLevels(regularLevels)
    }

    private suspend fun addPlayerLevelsUntil1080() {
        val jsonInstance = Json { isLenient = true }
        val json =
            getApplication<Application>().applicationContext.assets.open("player_levels_until_1080.json")
                .bufferedReader().use {
                    it.readText()
                }
        val playerLevels = jsonInstance.decodeFromString<List<RegularLevel>>(json)
        levelRepository.insertRegularLevels(playerLevels)
    }

    private fun addIntervals() {
        val sad = getApplication<Application>().getString(R.string.sad_mode_interval)
        val scores = getApplication<Application>().getString(R.string.player_centered_scores)
        val hint = getApplication<Application>().getString(R.string.hint_suggestion)
        val last = getApplication<Application>().getString(R.string.last_submission)
        val five = getApplication<Application>().getString(R.string.achievement_flawless_5)
        val fifteen = getApplication<Application>().getString(R.string.achievement_flawless_15)

        val intervalSadMode = Interval(
            name = sad,
            currentCount = 0,
            limitCount = 10,
            timestamp = -1
        )

        val scoresInterval = Interval(
            name = scores,
            currentCount = 0,
            limitCount = 6,
            timestamp = -1
        )

        val hintInterval = Interval(
            name = hint,
            currentCount = 0,
            limitCount = 3,
            timestamp = -1
        )

        val lastInterval = Interval(
            name = last,
            currentCount = 0,
            limitCount = 7,
            timestamp = -1
        )

        val fiveInterval = Interval(
            name = five,
            currentCount = 0,
            limitCount = 5,
            timestamp = -1
        )

        val fifteenInterval = Interval(
            name = fifteen,
            currentCount = 0,
            limitCount = 15,
            timestamp = -1
        )
        viewModelScope.launch {
            listOf(
                intervalSadMode,
                scoresInterval,
                hintInterval,
                fiveInterval,
                fifteenInterval,
                lastInterval
            ).forEach {
                levelRepository.addInterval(it)
            }
        }
    }

    private fun checkLanguagePackValidity() {
        C.languages.entries.forEach { entry ->
            val cc = entry.key
            val jsonInstance = Json { isLenient = true }
            val json =
                getApplication<Application>().applicationContext.assets.open("language_pack_$cc.json")
                    .bufferedReader().use {
                        it.readText()
                    }
            val languageLevels = jsonInstance.decodeFromString<List<LanguageLevel>>(json)
            val missingLanguagesMap = mutableMapOf<Int, List<String>>()

            languageLevels.forEachIndexed { index, level ->
                val missingLangs = arrayListOf<String>()
                level.solutionToCategory.forEach { solution ->
                    val diff =
                        C.languages.keys.minus(solution.value.keys.map { it.substringAfter("_") }
                            .toSet())
                    missingLangs.addAll(diff)
                    if (!solution.value.keys.contains("hint")) missingLangs.add("gb")
                }
                if (missingLangs.isNotEmpty()) {
                    missingLanguagesMap[index] = missingLangs
                }
            }

            Log.d(TAG, "Missing languages for $cc: $missingLanguagesMap")
            Log.d(TAG, "Levels count $cc: ${languageLevels.count()}")
            Log.d(TAG, "--------------------------------------------")
        }
    }

}