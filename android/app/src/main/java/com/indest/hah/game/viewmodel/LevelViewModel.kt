package com.indest.hah.game.viewmodel

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.*
import com.indest.hah.R
import com.indest.hah.ads.RewardType
import com.indest.hah.game.creation.BookMode
import com.indest.hah.game.creation.WindowSizeClass
import com.indest.hah.game.viewmodel.ContainerState.*
import com.indest.hah.general.AccountStatus
import com.indest.hah.general.CompetitionAnnouncement
import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.persistance.dto.SaveGame
import com.indest.hah.persistance.dto.PlayerLevel
import com.indest.hah.persistance.level.LevelProgress
import com.indest.hah.persistance.level.LevelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class LevelViewModel @Inject constructor(
    private val levelRepository: LevelRepository,
    application: Application
) :
    AndroidViewModel(application) {

    private val TAG = "LevelViewModel"

    private val _uiState = MutableStateFlow(UiState(level = Level(), solutionData = SolutionData()))
    val uiState = _uiState.asStateFlow()

    private var _windowSizeClassWidth = MutableStateFlow(WindowSizeClass.NOT_SET)
    private var _bookMode = MutableStateFlow(BookMode.NOT_SET)

    private val _competitionStatus = MutableStateFlow(CompetitionAnnouncement())

    private val _accountStatus = MutableStateFlow(AccountStatus.NOT_SET)
    private val _currentLevel = MutableStateFlow(Level())
    val currentLevel = _currentLevel.asStateFlow()

    private val variousEventChannel = Channel<VariousEvent>()
    val variousEventFlow = variousEventChannel.receiveAsFlow()

    private val submissionChannel = Channel<SubmissionEvent>()
    val submissionEventFlow = submissionChannel.receiveAsFlow()

    private val firebaseAnalytics = FirebaseAnalytics.getInstance(application.applicationContext)
    private val fsDb = FirebaseFirestore.getInstance()
    private var liveStoreRegistration: ListenerRegistration? = null

    private val _newPlayerLevels =
        MutableStateFlow(Triple(-1, -1, "")) // level nr, multiple levels, who submitted
    val newPlayerLevels = _newPlayerLevels.asStateFlow()

    var currentlySignedInPlayerName = "Unknown"

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(
            TAG,
            "Error syncing cloud with local. Start collecting level from  local database without up to date information.",
            throwable
        )
        viewModelScope.launch {
            levelRepository.latestLevelFlow()
                .collect { _currentLevel.value = it }
        }
    }

    override fun onCleared() {
        super.onCleared()
        liveStoreRegistration?.remove()
    }

    private val saveGameExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Could not write snapshot or save game: ${throwable.message}")
    }

    fun getCompetitionStatus():CompetitionAnnouncement
    {
        return _competitionStatus.value
    }

    fun setCompetitionStatus(competitionAnnouncement: CompetitionAnnouncement) {
        _competitionStatus.value = competitionAnnouncement
    }

    suspend fun changeFriendIndexesBasedOnColumnCount(newColumnCount: Int) {
        val columnDifference = newColumnCount - _uiState.value.level.levelInfo.columnCount
        Log.d(TAG, "newColumnCount: $newColumnCount")
        Log.d(TAG, "currentColumnCount: ${_uiState.value.level.levelInfo.columnCount}")
        if (columnDifference != 0) {
            Log.d(TAG, "WE HAVE A COLUMN DIFF: $columnDifference")
            levelRepository.changeFriendIndexesBasedOnColumnCount(
                _uiState.value.level.levelInfo.levelId,
                columnDifference,
                newColumnCount
            )
        }
    }

    fun setAccountStatus(accountStatus: AccountStatus) {
        _accountStatus.value = accountStatus
    }

    fun setWindowSizeClass(windowSizeClass: WindowSizeClass) {
        _windowSizeClassWidth.value = windowSizeClass
        _uiState.value = _uiState.value.copy(
            windowSizeClass = _windowSizeClassWidth.value
        )
    }

    fun setBookMode(bookMode: BookMode) {
        _bookMode.value = bookMode
        _uiState.value = _uiState.value.copy(
            bookMode = _bookMode.value
        )
    }

    init {
        viewModelScope.launch {
            levelRepository.init()
            uiUpdater()
        }
    }

    private suspend fun uiUpdater() {
        combine(
            _accountStatus,
            _currentLevel,
            _bookMode,
            _windowSizeClassWidth
        )
        { accountStatus, level, bookMode, windowSizeClassWidth ->
            {
                Pair(Pair(accountStatus, level), Pair(bookMode, windowSizeClassWidth))
            }
        }.distinctUntilChanged().collect {
            Log.d(TAG, it().toString())

            val level = it().first.second
            val accountStatus = it().first.first
            val bookMode = it().second.first
            val windowSizeClass = it().second.second
            val state = _uiState.value.state

            if (
                accountStatus != AccountStatus.NOT_SET &&
                level.levelInfo.levelId == 0 &&
                bookMode != BookMode.NOT_SET &&
                windowSizeClass != WindowSizeClass.NOT_SET &&
                state == LOADING
            ) {
                if (accountStatus == AccountStatus.SIGNED_IN) {
                    variousEventChannel.send(VariousEvent.LoadGame)
                }
                else {
                    viewModelScope.launch {
                        levelRepository.latestLevelFlow().collect { _currentLevel.value = it }
                    }
                }
            } else if (
                level.levelInfo.levelId != 0 &&
                accountStatus != AccountStatus.NOT_SET &&
                bookMode != BookMode.NOT_SET &&
                windowSizeClass != WindowSizeClass.NOT_SET
            ) {
                if (level.levelInfo.progress == LevelProgress.INTERMEDIATE && state != LEVEL_COMPLETE) {
                    _uiState.value = getNewUiState(
                        newState = INTERMEDIATE,
                        refreshLevelUi = true,
                        info = Pair(
                            level,
                            Triple(accountStatus, bookMode, windowSizeClass)
                        )
                    )
                } else if (state == LEVEL_COMPLETE) {
                    _uiState.value = getNewUiState(
                        newState = LEVEL_COMPLETE,
                        refreshLevelUi = true,
                        info = Pair(
                            level,
                            Triple(accountStatus, bookMode, windowSizeClass)
                        )
                    )
                } else if (state == PLAY || state == BUSY || state == MENU_ACTION) {
                    _uiState.value = getNewUiState(
                        newState = PLAY,
                        refreshLevelUi = false,
                        info = Pair(
                            level,
                            Triple(accountStatus, bookMode, windowSizeClass)
                        )
                    )
                } else if (state == PAUSED || state == LOADING) {
                    _uiState.value = getNewUiState(
                        newState = PLAY,
                        refreshLevelUi = true,
                        info = Pair(
                            level,
                            Triple(accountStatus, bookMode, windowSizeClass)
                        )
                    )
                }
                Log.d(TAG, _uiState.value.toString())
            }
        }
    }

    suspend fun isShowScores(): Boolean {
        val showScoresString =
            getApplication<Application>().getString(R.string.player_centered_scores)
        val limitReached = levelRepository.isLimitAttained(showScoresString)
        if (limitReached) resetIntervalCount(showScoresString)
        return limitReached
    }

    suspend fun isLastLevel(): Boolean {
        return levelRepository.isFinalLevel(_currentLevel.value.levelInfo.levelId)
    }

    private fun getNewUiState(
        newState: ContainerState,
        refreshLevelUi: Boolean = false,
        info: Pair<Level, Triple<AccountStatus, BookMode, WindowSizeClass>>
    ): UiState {
        return _uiState.value.copy(
            level = info.first,
            state = newState,
            accountStatus = info.second.first,
            bookMode = info.second.second,
            windowSizeClass = info.second.third,
            solutionData = SolutionData(),
            refreshLevelUi = refreshLevelUi
        )
    }

    fun resetSolutionState() {
        val newState = if (_uiState.value.state == BUSY) {
            PLAY
        } else {
            _uiState.value.state
        }
        _uiState.value = _uiState.value.copy(
            state = newState,
            solutionData = SolutionData(),
            refreshLevelUi = false
        )
    }

    fun setUiStateLoading() {
        _uiState.value = _uiState.value.copy(
            state = LOADING
        )
        viewModelScope.launch {
            variousEventChannel.send(VariousEvent.LoadGame)
        }
    }

    fun setUiStateBusy() {
        _uiState.value = _uiState.value.copy(
            state = BUSY,
            solutionData = SolutionData(),
            refreshLevelUi = false
        )
    }

    fun setUiStateMenuAction() {
        _uiState.value = _uiState.value.copy(
            state = MENU_ACTION,
            solutionData = SolutionData(),
            refreshLevelUi = false
        )
    }

    fun setUiStateWhenOnResume() {
        if (_uiState.value.state == PAUSED && _windowSizeClassWidth.value != WindowSizeClass.NOT_SET && _bookMode.value != BookMode.NOT_SET) {
            _uiState.value = _uiState.value.copy(
                state = PLAY,
                solutionData = SolutionData(),
                refreshLevelUi = false
            )
        } else if (_uiState.value.state == MENU_ACTION) {
            _uiState.value = _uiState.value.copy(
                state = PLAY,
                solutionData = SolutionData(),
                refreshLevelUi = false
            )
        }

    }

    fun setUiStateWhenOnPause() {
        if (_uiState.value.state == PLAY) {
            _uiState.value = _uiState.value.copy(
                state = PAUSED,
                solutionData = SolutionData(),
                refreshLevelUi = false
            )
        }
    }

    fun setUiStatePlayWhenBusy() {
        if (_uiState.value.state == BUSY) {
            _uiState.value = _uiState.value.copy(
                state = PLAY,
                solutionData = SolutionData()
            )
        }
    }

    fun loadLatestLevelFromCloudOrLocal(saveGame: SaveGame?) {
        viewModelScope.launch(exceptionHandler) {
            if (saveGame != null) {
                levelRepository.updateLocalGame(saveGame)
                launch {
                    levelRepository.latestLevelFlow().collect { _currentLevel.value = it }
                }
            } else {
                launch {
                    levelRepository.latestLevelFlow().collect { _currentLevel.value = it }
                }
            }
            launch {
                collectPlayerLevels()
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

    fun changeLevel(chosenLevel: Int) {
        viewModelScope.launch {
            _currentLevel.value.levelInfo.let { levelInfo ->
                val hintInterval = getApplication<Application>().getString(R.string.hint_suggestion)
                levelRepository.setCountTo(hintInterval, 0)
                levelRepository.nextLevelActions(
                    levelInfo.levelId,
                    chosenLevel
                )
            }
        }
    }

    fun resetIntervalCount(interval: String) {
        viewModelScope.launch {
            levelRepository.setCountTo(interval, 0)
        }
    }

    fun executeLevelCompleteActions() {
        _uiState.value = _uiState.value.copy(state = PAUSED)
        val levelNr = _currentLevel.value.levelInfo.levelId
        val hintInterval = getApplication<Application>().getString(R.string.hint_suggestion)
        viewModelScope.launch {
            levelRepository.executeLevelCompleteTransaction(levelNr, hintInterval)
            levelRepository.increaseCount(getApplication<Application>().getString(R.string.sad_mode_interval))
            levelRepository.increaseCount(getApplication<Application>().getString(R.string.player_centered_scores))
            saveGameImmediately()
            logLevelStart()
        }
    }

    fun executeLevelCompleteActionsFinal() {
        _uiState.value = _uiState.value.copy(state = PAUSED)
        val levelNr = _currentLevel.value.levelInfo.levelId
        val hintInterval = getApplication<Application>().getString(R.string.hint_suggestion)
        viewModelScope.launch {
            levelRepository.executeLevelCompleteTransactionFinal(levelNr, hintInterval)
            saveGameImmediately()
        }
    }

    fun allLevelsFlow(): Flow<List<Pair<Boolean, LevelData>>> {
        return flow {
            emit(levelRepository.getAllLevels())
        }
    }

    fun allPalindromesFlow(): Flow<MutableSet<String>> {
        return flow {
            emit(levelRepository.getAllPalindromes())
        }
    }

    fun saveGameImmediately() {
        viewModelScope.launch(saveGameExceptionHandler) {
            if (_accountStatus.value == AccountStatus.SIGNED_IN) {
                val saveGame = levelRepository.getLocalSaveGame()
                variousEventChannel.send(VariousEvent.SaveGame(saveGame))
            }
        }
    }

    private fun submitScore() {
        viewModelScope.launch {
            if (_accountStatus.value == AccountStatus.SIGNED_IN) {
                val totalScore = levelRepository.getTotalUpdatedScore(
                    _currentLevel.value.levelInfo.levelId
                )
                variousEventChannel.send(VariousEvent.SubmitScore(totalScore, _currentLevel.value.levelInfo.levelId > 1000))
            }
        }
    }

    private fun unlockAchievement(achievement: String) {
        viewModelScope.launch {
            if (_accountStatus.value == AccountStatus.SIGNED_IN) {
                variousEventChannel.send(VariousEvent.UnlockAchievement(achievement))
            }
        }
    }

    sealed class VariousEvent {
        data class UnlockAchievement(val achievement: String) : VariousEvent()
        data class SubmitScore(val score: Long, val playerLevels: Boolean) : VariousEvent()
        data class SaveGame(val localSaveGame: com.indest.hah.persistance.dto.SaveGame) :
            VariousEvent()
        object LoadGame : VariousEvent()
        object PalindromeSubmissionSucceeded : VariousEvent()
        object PalindromeSubmissionFailed : VariousEvent()
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
        }
    }

    private fun isSolutionCorrect(guess: String): Boolean {
        return _currentLevel.value.solutions.any {
            it.palindrome.replace(" ", "") == guess
        }
    }

    private fun updateSolutionState(index: Int, guess: String) {
        viewModelScope.launch {
            //val solution = _currentLevel.value.levelInfo.solution.replace(" ", "")
            val solution = _currentLevel.value.solutions.first().palindrome.replace(" ", "")
            if (guess.length == solution.length) {
                val playerGuess = guess.map { char ->
                    if ("^([?!.,’])".contains(char)) "" else char
                }.joinToString("")

                Log.d(TAG, "guess: $guess, solutions: ${_currentLevel.value.solutions}")

                val hintsInterval =
                    getApplication<Application>().getString(R.string.hint_suggestion)

                val maxScore = if (_currentLevel.value.levelInfo.levelId > 1000) {
                    _currentLevel.value.levelInfo.puzzle.length * 100
                } else {
                    _currentLevel.value.levelInfo.levelId * 100
                }

                when {
                    isSolutionCorrect(guess) -> {
                        submitScore()
                        levelRepository.updateLevelProgress(
                            _currentLevel.value.levelInfo.levelId,
                            LevelProgress.INTERMEDIATE
                        )
                        val solutionData = SolutionData(
                            solution = Solution.CORRECT,
                            index = index,
                            score = _currentLevel.value.levelInfo.score,
                            maxScore = maxScore,
                            showHintsBadge = false
                        )
                        _uiState.value = _uiState.value.copy(
                            state = LEVEL_COMPLETE,
                            solutionData = solutionData
                        )
                        resolveLevelCompleteAchievements(solutionData)
                        levelRepository.setCountTo(hintsInterval, 0)
                        logLevelEnd()
                    }
                    playerGuess.reversed() == playerGuess -> {
                        val showHintsBadge =
                            levelRepository.increaseCountAndGetResult(hintsInterval)
                        val solutionData = SolutionData(
                            solution = Solution.BOGUS_PALINDROME,
                            index = index,
                            score = _currentLevel.value.levelInfo.score,
                            maxScore = maxScore,
                            showHintsBadge = showHintsBadge
                        )
                        _uiState.value = _uiState.value.copy(
                            state = BUSY,
                            solutionData = solutionData
                        )
                        // Wrong Palindrome achievement
                        unlockAchievement(getApplication<Application>().getString(R.string.achievement_wrong_palindrome))
                    }
                    else -> {
                        val showHintsBadge =
                            levelRepository.increaseCountAndGetResult(hintsInterval)
                        val solutionData = SolutionData(
                            solution = Solution.WRONG,
                            index = index,
                            score = _currentLevel.value.levelInfo.score,
                            maxScore = maxScore,
                            showHintsBadge = showHintsBadge
                        )
                        _uiState.value = _uiState.value.copy(
                            state = BUSY,
                            solutionData = solutionData
                        )
                    }
                }
            } else {
                val solutionData = SolutionData(
                    solution = Solution.NONE,
                    index = -1,
                    score = -1,
                    maxScore = -1,
                    showHintsBadge = false
                )
                _uiState.value = _uiState.value.copy(
                    solutionData = solutionData
                )
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
            val scoreToMaxRatio =
                _currentLevel.value.levelInfo.score / (_currentLevel.value.levelInfo.levelId * 100f)
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

    fun logCompetitionRelatedEvent(event:String) {
        viewModelScope.launch(Dispatchers.Default) {
            firebaseAnalytics.logEvent(event, bundleOf())
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

    private suspend fun collectPlayerLevels() {
        liveStoreRegistration?.remove()
        val maxLevelId = getMaxLevelId()
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
                                    playerLevels.add(
                                        PlayerLevel(
                                            levelValues["levelId"] as Long,
                                            levelValues["player"] as String,
                                            solutionsToCategories
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
                        DocumentChange.Type.MODIFIED -> Log.d(
                            TAG,
                            "Modified level: " + dc.document.data
                        )
                        DocumentChange.Type.REMOVED -> Log.d(
                            TAG,
                            "Removed level: " + dc.document.data
                        )
                    }
                }
            }
            Log.d(TAG, "New featured levels: $playerLevels")
            viewModelScope.launch {
                if (playerLevels.size != 0) {
                    val levelAndCount = insertPlayerLevels(playerLevels)
                    if (levelAndCount.second > 0) {
                        _newPlayerLevels.value = if (levelAndCount.second > 1) Triple(
                            -1,
                            levelAndCount.second,
                            ""
                        ) else Triple(levelAndCount.first.levelId, 1, levelAndCount.first.author)
                    }
                }
            }
        })
    }

    fun resetNewPlayersLevels() {
        _newPlayerLevels.value = Triple(-1, -1, "")
    }

    private suspend fun insertPlayerLevels(playerLevels: List<PlayerLevel>): Pair<LevelData, Int> {
        return levelRepository.insertPlayerLevels(playerLevels)
    }

    suspend fun getMaxLevelId(): Int {
        return levelRepository.getMaxLevelId()
    }

    suspend fun submitUserLevel(playersPalindrome:String) {
        // Create a new user with a first, middle, and last name
        val level = hashMapOf(
            "is_featured" to false,
            "level" to playersPalindrome,
            "timestamp" to FieldValue.serverTimestamp(),
            "user_name" to currentlySignedInPlayerName
        )
        Log.d(TAG, "fsDb: ${fsDb.app.options.toString()}")
        Log.d(TAG, "submitUserLevel: ${level.toString()}")

        // Add a new document with a generated ID
        fsDb.collection("user_submitted_levels")
            .add(level)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
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

}