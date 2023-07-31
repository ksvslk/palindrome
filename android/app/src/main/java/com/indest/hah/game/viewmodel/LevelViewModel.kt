package com.indest.hah.game.viewmodel

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.annotation.NonNull
import androidx.core.os.bundleOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.android.play.core.assetpacks.db
import com.google.android.play.core.tasks.OnCompleteListener
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.*
import com.indest.hah.R
import com.indest.hah.ads.RewardType
import com.indest.hah.general.AccountStatus
import com.indest.hah.other.C
import com.indest.hah.other.getLevelScore
import com.indest.hah.persistance.dto.LanguageLevel
import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.persistance.dto.NewLevelNotification
import com.indest.hah.persistance.dto.PlayerLevel
import com.indest.hah.persistance.dto.SaveGame
import com.indest.hah.persistance.level.LevelProgress
import com.indest.hah.persistance.level.LevelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject


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
        MutableStateFlow(NewLevelNotification(-1,-1,"",""))
    val newPlayerLevels = _newPlayerLevels.asStateFlow()

    var currentlySignedInPlayerName = "Unknown"
    var tempSubmissionPalindrome = ""
    var tempCountrySelected = "gb"

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(
            TAG,
            "Error syncing cloud with local. Start collecting level from  local database without up to date information.",
            throwable
        )
        startCollectingLevelUpdate()
    }

    fun updateColumnCount(newCols:Int)
    {
        viewModelScope.launch {
            levelRepository.updateColumnCount(newCols, currentLevel.value.levelInfo.levelId)
        }
    }

    private fun startCollectingLevelUpdate() {
        viewModelScope.launch {
            levelRepository.latestLevelFlow().collect {
                val changeInColumnsCount =
                    it.levelInfo.columnCount != _currentLevel.value.levelInfo.columnCount
                val changeInLevelId = it.levelInfo.levelId != _currentLevel.value.levelInfo.levelId
                _currentLevel.value = it
                if (isCurrentLevelLegit() && (changeInLevelId)) {
                    val progress = getProgressAndText()
                    gameEventChannel.send(GameEvent.DisplayLevel(_currentLevel.value, progress))
                }
            }
        }
        viewModelScope.launch {
            if (levelRepository.getMaxLevelId() < 10000) {
                addLanguagePack()
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
        } else if (levelId > 10000) {
            val language = _currentLevel.value.levelInfo.countryCode
            val levels = levelRepository.getLevelsForCountry(language)
            val levelsCount = levels.count()
            val levelShownInUi = levels.indexOfFirst { levelData -> levelData.levelId == levelId } + 1
            val authorIfExists = if (author.isNotEmpty()) " - $author" else ""
            val text =
                if (solutionsCount > 1) "$levelShownInUi*$authorIfExists" else "$levelShownInUi$authorIfExists"
            val progress = (levelShownInUi.toFloat() / levelsCount.toFloat() * 100.0).toInt()
            result = Pair(progress, text)
        } else {
            val levelsCount = levelRepository.getMaxGameLevelId()
            val text = if (solutionsCount > 1) "$levelId*" else "$levelId"
            val progress = (levelId / levelsCount.toFloat() * 100.0).toInt()
            result = Pair(progress, text)
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
        viewModelScope.launch {
            levelRepository.init()
        }

    }


    suspend fun isShowScores(): Boolean {
        val showScoresString =
            getApplication<Application>().getString(R.string.player_centered_scores)
        val limitReached = levelRepository.isLimitAttained(showScoresString)
        if (limitReached) resetIntervalCount(showScoresString)
        return limitReached
    }


    suspend fun isLastLevel(): Pair<Boolean, Boolean> {
        val lastGameLevel =
            levelRepository.isFinalLevel(_currentLevel.value.levelInfo.levelId, 1, 100)
        val lastPlayerLevel =
            levelRepository.isFinalLevel(_currentLevel.value.levelInfo.levelId, 1001, Int.MAX_VALUE)
        return Pair(lastGameLevel == 1, lastPlayerLevel == 1)
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

    fun resetLetterInfo(levelNr:Int)
    {
        viewModelScope.launch {
            Log.d(TAG, "resetLetterInfo")
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


    fun executeLevelCompleteActions(changeLevelTo: Int = _currentLevel.value.levelInfo.levelId + 1) {
        val levelNr = _currentLevel.value.levelInfo.levelId
        val hintInterval = getApplication<Application>().getString(R.string.hint_suggestion)
        viewModelScope.launch {
            levelRepository.executeLevelCompleteTransaction(levelNr, hintInterval, changeLevelTo)
            levelRepository.increaseCount(getApplication<Application>().getString(R.string.sad_mode_interval))
            levelRepository.increaseCount(getApplication<Application>().getString(R.string.player_centered_scores))
            if (changeLevelTo - _currentLevel.value.levelInfo.levelId == 1) saveGameImmediately()
            logLevelStart()
        }
    }


    fun executeLevelCompleteActionsFinal() {
        val levelNr = _currentLevel.value.levelInfo.levelId
        val hintInterval = getApplication<Application>().getString(R.string.hint_suggestion)
        viewModelScope.launch {
            levelRepository.executeLevelCompleteTransactionFinal(levelNr, hintInterval)
            saveGameImmediately()
        }
    }


    fun allPlayerLevelsFlow(): Flow<List<Pair<Boolean, LevelData>>> {
        return flow {
            emit(levelRepository.getAllLevels().filter { it.second.levelId in 1000..9999 })
        }
    }


    fun allGameLevelsFlow(): Flow<List<Pair<Boolean, LevelData>>> {
        return flow {
            emit(levelRepository.getAllLevels().filter { it.second.levelId <= 100 })
        }
    }


    fun allPalindromesFlow(): Flow<MutableSet<String>> {
        return flow {
            emit(levelRepository.getAllPalindromes())
        }
    }

    fun allUniqueCountries(): Flow<List<String>> {
        return flow {
            emit(levelRepository.getUniqueCountries())
        }
    }

    fun allUniqueCountriesWithLevelCount(): Flow<List<Pair<String,Int>>> {
        return flow {
            emit(levelRepository.getUniqueCountriesWithLevelCount())
        }
    }

    fun getLevelsForCountry(cc: String): Flow<List<LevelData>> {
        return flow {
            emit(levelRepository.getLevelsForCountry(cc))
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
            val isLastLevel: Pair<Boolean, Boolean>
        ) : GameEvent()

        object ShowLoading : GameEvent()
    }


    sealed class VariousEvent {
        data class UnlockAchievement(val achievement: String) : VariousEvent()
        data class SubmitScore(val score: Long, val playerLevels: Boolean, val languageLevels:Boolean, val countryCode:String) : VariousEvent()
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
        if (_currentLevel.value.levelInfo.levelId >= 21 &&
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
            10010 -> unlockAchievement(context.getString(R.string.achievement_chinese_linguist))
            10026 -> unlockAchievement(context.getString(R.string.achievement_german_linguist))
            10046 -> unlockAchievement(context.getString(R.string.achievement_spanish_linguist))
            10066 -> unlockAchievement(context.getString(R.string.achievement_estonian_linguist))
            10086 -> unlockAchievement(context.getString(R.string.achievement_finnish_linguist))
            10106 -> unlockAchievement(context.getString(R.string.achievement_french_linguist))
            10126 -> unlockAchievement(context.getString(R.string.achievement_hindi_linguist))
            10137 -> unlockAchievement(context.getString(R.string.achievement_indonesian_linguist))
            10155 -> unlockAchievement(context.getString(R.string.achievement_italian_linguist))
            10175 -> unlockAchievement(context.getString(R.string.achievement_japanese_linguist))
            10195 -> unlockAchievement(context.getString(R.string.achievement_korean_linguist))
            10215 -> unlockAchievement(context.getString(R.string.achievement_dutch_linguist))
            10235 -> unlockAchievement(context.getString(R.string.achievement_polish_linguist))
            10255 -> unlockAchievement(context.getString(R.string.achievement_portuguese_linguist))
            10275 -> unlockAchievement(context.getString(R.string.achievement_romanian_linguist))
            10295 -> unlockAchievement(context.getString(R.string.achievement_russian_linguist))
            10315 -> unlockAchievement(context.getString(R.string.achievement_swedish_linguist))
            10335 -> unlockAchievement(context.getString(R.string.achievement_turkish_linguist))
            10355 -> unlockAchievement(context.getString(R.string.achievement_ukrainian_linguist))
        }
    }


    private fun isSolutionCorrect(guess: String): Boolean {
        Log.d(TAG, "Guess: $guess")
        val databaseSolutions = _currentLevel.value.solutions.map { it.palindrome.replace(" ", "") }
        Log.d(TAG, "Database solutions: $databaseSolutions")
        return _currentLevel.value.solutions.any {
            it.palindrome.replace(Regex("[\\s-]+"), "") == guess
        }
    }


    private fun updateSolutionState(index: Int, guess: String) {
        viewModelScope.launch {
            val solution = _currentLevel.value.solutions.first().palindrome.replace(Regex("[\\s-]+"), "")
            var solutionData = SolutionData()

            Log.d(TAG, "Solution: $solution")
            Log.d(TAG, "Guess: $guess")

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
            val maxScore = _currentLevel.value.levelInfo.levelId.getLevelScore(_currentLevel.value.levelInfo.puzzle)
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

    suspend fun simulateNewLevelFetch() {
        val playerLevels = mutableListOf<PlayerLevel>()
        val docRef: DocumentReference = fsDb
            .collection("user_submitted_levels")
            .document("Bc4MHj3i0hMNRsgVAMdc")
        docRef.get().addOnSuccessListener {
            Log.d(TAG,"it received!")
            try {
                Log.d(TAG, it.data.toString())
                it.data.let { levelValues ->
                    val solutionsToCategories =
                        levelValues?.get("solutionToCategory") as Map<String, String>
                    if (solutionsToCategories.isEmpty()) throw Exception("Solutions were empty")
                    val countryCode =
                        levelValues.getOrElse("countryCode") { "gb" }
                    val level = PlayerLevel(
                        levelId = levelValues["levelId"] as Long,
                        player = levelValues["player"] as String,
                        solutionToCategory = solutionsToCategories,
                        countryCode = countryCode as String
                    )
                    Log.d(TAG, "level: $level")
                    playerLevels.add(level)
                }
            } catch (e: Exception) {
                Log.w(
                    TAG,
                    "Something went wrong when parsing the user level from FB.",
                    e
                )
            }

            Log.d(TAG, "levels to insert: ${playerLevels.toString()}")

            viewModelScope.launch {
                if (playerLevels.size != 0) {
                    // Get Pair where first element is a level when only 1 level added
                    // Second element is the level count when there are more than 1 level added.
                    val levelAndCount = insertPlayerLevels(playerLevels)
                    if (levelAndCount.second > 0) {
                        if (levelAndCount.second > 1)
                        {
                            _newPlayerLevels.update {
                                NewLevelNotification(
                                    -1,
                                    levelAndCount.second,
                                    "",
                                    ""
                                )
                            }
                        }
                        else {
                            _newPlayerLevels.update {
                                NewLevelNotification(
                                    levelAndCount.first.levelId,
                                    1,
                                    levelAndCount.first.author,
                                    levelAndCount.first.countryCode
                                )
                            }
                        }
                    }
                }
            }
        }

    }

    private suspend fun subscribeToRealTimePlayerLevelUpdates() {
        if (_accountStatus.value == AccountStatus.SIGNED_IN_WITH_FIREBASE) {
            val maxLevelId = levelRepository.getMaxLevelId()
            val query = fsDb.collection("user_submitted_levels")
                .whereEqualTo("isFeatured", true)
                .whereGreaterThan("levelId", maxLevelId)
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
                                        val solutionsToCategories =
                                            levelValues["solutionToCategory"] as Map<String, String>
                                        if (solutionsToCategories.isEmpty()) throw Exception("Solutions were empty")
                                        val countryCode =
                                            levelValues.getOrElse("countryCode") { "gb" }
                                        playerLevels.add(
                                            PlayerLevel(
                                                levelId = levelValues["levelId"] as Long,
                                                player = levelValues["player"] as String,
                                                solutionToCategory = solutionsToCategories,
                                                countryCode = countryCode as String
                                            )
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.w(
                                        TAG,
                                        "Something went wrong when parsing the user level from FB.",
                                        e
                                    )
                                    return@EventListener
                                }
                            }

                            DocumentChange.Type.MODIFIED -> {}
                            DocumentChange.Type.REMOVED -> {}
                        }
                    }
                }
                viewModelScope.launch {
                    if (playerLevels.size != 0) {
                        // Get Pair where first element is a level when only 1 level added
                        // Second element is the level count when there are more than 1 level added.
                        val levelAndCount = insertPlayerLevels(playerLevels)
                        if (levelAndCount.second > 0) {
                            if (levelAndCount.second > 1)
                            {
                                _newPlayerLevels.value = NewLevelNotification(
                                    -1,
                                    levelAndCount.second,
                                    "",
                                    ""
                                )
                            }
                            else {
                                _newPlayerLevels.value = NewLevelNotification(
                                    levelAndCount.first.levelId,
                                    -1,
                                    levelAndCount.first.author,
                                    levelAndCount.first.countryCode
                                )
                            }
                        }
                    }
                }
            })
        }
    }


    fun resetNewPlayersLevels() {
        _newPlayerLevels.value = NewLevelNotification(-1,-1,"","")
    }


    private suspend fun insertPlayerLevels(playerLevels: List<PlayerLevel>): Pair<LevelData, Int> {
        return levelRepository.insertPlayerLevels(playerLevels)
    }

    suspend fun submitUserLevel(playersPalindrome: String) {
        // Create a new user with a first, middle, and last name
        val level = hashMapOf(
            "isFeatured" to false,
            "levelId" to -1337,
            "palindrome" to playersPalindrome,
            "player" to currentlySignedInPlayerName,
            "solutionToCategory" to hashMapOf(Pair(playersPalindrome, "[ADD_CATEGORY]")),
            "countryCode" to tempCountrySelected,
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

    private suspend fun addLanguagePack() {
        C.languages.entries.forEach { entry ->
            val cc = entry.key
            val jsonInstance = Json { isLenient = true }
            val json =
                getApplication<Application>().applicationContext.assets.open("language_pack_$cc.json")
                    .bufferedReader().use {
                        it.readText()
                    }
            val languageLevels = jsonInstance
                .decodeFromString<List<JsonElement>>(json)
                .map { jsonInstance.decodeFromJsonElement(it) as LanguageLevel }

            levelRepository.insertLanguageLevelPack(languageLevels, cc)
        }
    }
}