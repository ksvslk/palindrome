package com.indest.hah.game

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.games.Games
import com.indest.hah.R
import com.indest.hah.game.ContainerState.*
import com.indest.hah.game.levelcreation.WindowSizeClass
import com.indest.hah.other.AccountStatus
import com.indest.hah.other.BookMode
import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.persistance.level.LevelProgress
import com.indest.hah.persistance.level.LevelRepository
import com.indest.hah.savegame.SnapshotHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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

    //val windowSizeClassWidth = _windowSizeClassWidth.asStateFlow()
    private var _bookMode: MutableStateFlow<BookMode> = MutableStateFlow(BookMode.NOT_SET)

    //var isBookMode = _isBookMode.asStateFlow()
    private val _accountStatus = MutableStateFlow(AccountStatus.NOT_SET)
    //val accountStatus = _accountStatus.asStateFlow()

    private val _currentLevel = MutableStateFlow(Level())
    val currentLevel = _currentLevel.asStateFlow()

    @Inject
    lateinit var cloudSave: SnapshotHelper
    private var saveGameJob: Job? = null

    private val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        Log.e(
            TAG,
            "Error syncing cloud with local. Start collecting level from  local database without up to date information."
        )
        collectLevels()
    }

    fun setAccountStatus(accountStatus: AccountStatus) {
        _accountStatus.value = accountStatus
    }

    fun setWindowSizeClass(windowSizeClass: WindowSizeClass) {
        _windowSizeClassWidth.value = windowSizeClass
    }

    fun setBookMode(bookMode: BookMode) {
        _bookMode.value = bookMode
    }

    init {
        viewModelScope.launch {
            Log.d(TAG, "INIT")
            levelRepository.init()
            combine(
                _accountStatus,
                _windowSizeClassWidth,
                _bookMode,
                _currentLevel
            ) { accountStatus, windowSizeClass, bookMode, level ->
                {
                    Pair(level, Triple(accountStatus, bookMode, windowSizeClass))
                }
            }.collect {
                if (it().second.first != AccountStatus.NOT_SET &&
                    it().second.second != BookMode.NOT_SET &&
                    it().second.third != WindowSizeClass.NOT_SET &&
                    _uiState.value.state == LOADING &&
                    it().first.levelInfo.levelId == 0
                ) {
                    Log.d(TAG, "Activate latest level flow.")
                    launch {
                        levelRepository.latestLevelFlow()
                            .collect { _currentLevel.value = it }
                    }
                }
                else if(
                    it().first.levelInfo.levelId != 0 &&
                    it().second.first != AccountStatus.NOT_SET &&
                    it().second.second != BookMode.NOT_SET &&
                    it().second.third != WindowSizeClass.NOT_SET) {

                   if (it().first.levelInfo.progress == LevelProgress.INTERMEDIATE && _uiState.value.state != LEVEL_COMPLETE)
                    {
                        _uiState.value = _uiState.value.copy(
                            level = it().first,
                            state = INTERMEDIATE,
                            accountStatus = it().second.first,
                            bookMode = it().second.second,
                            windowSizeClass = it().second.third,
                            solutionData = SolutionData(),
                            refreshLevelUi = true
                        )
                    }
                    else if (_uiState.value.state == LEVEL_COMPLETE)
                    {
                        _uiState.value = _uiState.value.copy(
                            level = it().first,
                            state = LEVEL_COMPLETE,
                            accountStatus = it().second.first,
                            bookMode = it().second.second,
                            windowSizeClass = it().second.third,
                            solutionData = SolutionData(),
                            refreshLevelUi = true
                        )
                    }
                    else if (_uiState.value.state == PLAY) {
                        _uiState.value = _uiState.value.copy(
                            level = it().first,
                            state = PLAY,
                            accountStatus = it().second.first,
                            bookMode = it().second.second,
                            windowSizeClass = it().second.third,
                            solutionData = SolutionData(),
                            refreshLevelUi = false
                        )
                    }
                    else if (_uiState.value.state == PAUSED) {
                        _uiState.value = _uiState.value.copy(
                            level = it().first,
                            state = PLAY,
                            accountStatus = it().second.first,
                            bookMode = it().second.second,
                            windowSizeClass = it().second.third,
                            solutionData = SolutionData(),
                            refreshLevelUi = true
                        )
                    }
                    else if(_uiState.value.state == LOADING) {
                        _uiState.value = _uiState.value.copy(
                            level = it().first,
                            state = PLAY,
                            accountStatus = it().second.first,
                            bookMode = it().second.second,
                            windowSizeClass = it().second.third,
                            solutionData = SolutionData(),
                            refreshLevelUi = true
                        )
                    }
                   else if(_uiState.value.state == BUSY) {
                       _uiState.value = _uiState.value.copy(
                           level = it().first,
                           state = PLAY,
                           accountStatus = it().second.first,
                           bookMode = it().second.second,
                           windowSizeClass = it().second.third,
                           solutionData = SolutionData(),
                           refreshLevelUi = false
                       )
                   }
                }
            }
        }
    }

    fun resetSolutionState()
    {
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

    fun pauseUiContainerState()
    {
        if (_uiState.value.state == PLAY)
        {
            _uiState.value = _uiState.value.copy(
                state = PAUSED,
                solutionData = SolutionData(),
                refreshLevelUi = false
            )
        }
    }

    fun busyUiContainerState()
    {
            _uiState.value = _uiState.value.copy(
                state = BUSY,
                solutionData = SolutionData(),
                refreshLevelUi = false
            )
    }

    fun playUiContainerState()
    {
        if (_uiState.value.state == PAUSED)
        {
            _uiState.value = _uiState.value.copy(
                state = PLAY,
                solutionData = SolutionData()
            )
        }
    }

    fun loadLatestLevelFromCloudOrLocal() {
        viewModelScope.launch(exceptionHandler) {
            val saveGame = cloudSave.load()
            Log.d(TAG, "Get save game from cloud.")
            levelRepository.updateLocalGame(saveGame)
            Log.d(
                TAG,
                "Update local game to: ${saveGame.game.findLast { it.levelInfo.progress == LevelProgress.IN_PROGRESS }?.levelInfo}"
            )
            collectLevels()
            startSavingGame()
        }
    }

    fun stopSavingGame() {
        Log.d(TAG, "Stop saving game.")
        saveGameJob?.cancel()
    }

    private fun collectLevels() {
        viewModelScope.launch {
            levelRepository.latestLevelFlow()
                .collect { _currentLevel.value = it }
        }
        startSavingGame()
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
                val pointsToSubtract = levelInfo.levelId * 100 / 20
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
                startSavingGame()
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
                val scoreInterval =
                    getApplication<Application>().getString(R.string.player_centered_scores)
                val currentScoreIntervalCount = levelRepository.getCount(scoreInterval)

                // Loading each level gives +1 so to balance it out we will -1 each level
                // change made from "Level Select" screen as this is not part of the intended gameplay.
                if (currentScoreIntervalCount > 0) {
                    levelRepository.setCountTo(scoreInterval, currentScoreIntervalCount - 1)
                }

                levelRepository.setCountTo(hintInterval, 0)
                levelRepository.nextLevelActions(
                    levelInfo.levelId,
                    chosenLevel
                )
            }
        }
    }

    suspend fun isIntervalAttained(interval: String): Boolean {
        return levelRepository.increaseCount(interval)
    }

    fun executeLevelCompleteActions() {
        _uiState.value = _uiState.value.copy(state = PAUSED)
        val levelNr = _currentLevel.value.levelInfo.levelId
        val hintInterval = getApplication<Application>().getString(R.string.hint_suggestion)
        viewModelScope.launch {
            levelRepository.executeLevelCompleteTransaction(levelNr, hintInterval)
        }
    }

    fun allLevelsFlow(): Flow<List<LevelData>> {
        return flow {
            emit(levelRepository.getAllLevels())
        }
    }

    fun startSavingGame() {
        if (saveGameJob == null || saveGameJob?.isCompleted == true) {
            Log.d(TAG, "Start saving game. Job was null/completed.")

            saveGameJob = viewModelScope.launch {
                while (isActive) {
                    delay(30000)
                    Log.d(TAG, "Save game to cloud.")
                    val saveGame = levelRepository.getLocalSaveGame()
                    cloudSave.save(saveGame)
                }
            }
        } else if (saveGameJob?.isCancelled == true) {
            Log.d(TAG, "Start saving game (job was Cancelled")
            saveGameJob?.start()
        }
    }

    fun saveGameImmediately() {
        viewModelScope.launch {
            val saveGame = levelRepository.getLocalSaveGame()
            cloudSave.save(saveGame)
        }
    }

    private fun submitScore() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            GoogleSignIn.getLastSignedInAccount(context)?.let {
                val totalScore = levelRepository.getTotalUpdatedScore(
                    _currentLevel.value.levelInfo.levelId
                )
                Games.getLeaderboardsClient(context, it)
                    .submitScore(
                        context.getString(R.string.leaderboard_crme_de_la_crme),
                        totalScore
                    )
            }
        }
    }

    private fun unlockAchievement(achievement: String) {
        val context = getApplication<Application>()
        GoogleSignIn.getLastSignedInAccount(context)?.let { signedInAccount ->
            Games.getAchievementsClient(context, signedInAccount)
                .unlock(achievement)
        }
    }

    private fun updateAndConsiderFlawlessAchievements(
        solutionData: SolutionData
    ) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            if (solutionData.score == solutionData.maxScore) {
                if (levelRepository.increaseCount(context.getString(R.string.achievement_flawless_5))) {
                    unlockAchievement(context.getString(R.string.achievement_flawless_5))
                }
                if (levelRepository.increaseCount(context.getString(R.string.achievement_flawless_15))) {
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
        if (_currentLevel.value.levelInfo.solution == "LEVEL") {
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

    private fun updateSolutionState(index: Int, guess: String) {
        viewModelScope.launch {
            val solution = _currentLevel.value.levelInfo.solution.replace(" ", "")
            if (guess.length == solution.length) {
                val playerGuess = guess.map { char ->
                    if ("^([?!.,’])".contains(char)) "" else char
                }.joinToString("")
                Log.d(TAG, "guess: $guess, solution: $solution")
                val hintsInterval =
                    getApplication<Application>().getString(R.string.hint_suggestion)

                when {
                    guess == solution -> {
                        submitScore()
                        levelRepository.updateLevelProgress(
                            _currentLevel.value.levelInfo.levelId,
                            LevelProgress.INTERMEDIATE
                        )
                        val solutionData = SolutionData(
                            solution = Solution.CORRECT,
                            index = index,
                            score = _currentLevel.value.levelInfo.score,
                            maxScore = _currentLevel.value.levelInfo.levelId * 100,
                            showHintsBadge = false
                        )
                        _uiState.value = _uiState.value.copy(
                            state = LEVEL_COMPLETE,
                            solutionData = solutionData
                        )
                        resolveLevelCompleteAchievements(solutionData)
                        levelRepository.setCountTo(hintsInterval, 0)
                    }
                    playerGuess.reversed() == playerGuess -> {
                        val showHintsBadge = levelRepository.increaseCount(hintsInterval)
                        val solutionData = SolutionData(
                            solution = Solution.BOGUS_PALINDROME,
                            index = index,
                            score = _currentLevel.value.levelInfo.score,
                            maxScore = _currentLevel.value.levelInfo.levelId * 100,
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
                        val showHintsBadge = levelRepository.increaseCount(hintsInterval)
                        val solutionData = SolutionData(
                            solution = Solution.WRONG,
                            index = index,
                            score = _currentLevel.value.levelInfo.score,
                            maxScore = _currentLevel.value.levelInfo.levelId * 100,
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

}