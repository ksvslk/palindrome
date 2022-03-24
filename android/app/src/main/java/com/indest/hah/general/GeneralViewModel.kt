package com.indest.hah.general

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.indest.hah.BuildConfig
import com.indest.hah.R
import com.indest.hah.ads.AdsLiveData
import com.indest.hah.ads.RewardType
import com.indest.hah.game.viewmodel.FirebaseCustom
import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.dto.MinSkuDetails
import com.indest.hah.persistance.level.LevelRepository
import com.indest.hah.views.ButtonState
import com.sample.android.trivialdrivesample.billing.BillingDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GeneralViewModel @Inject constructor(
    private val levelRepository: LevelRepository,
    application: Application
) : AndroidViewModel(application) {

    private val TAG = "GeneralViewModel"

    private lateinit var _billingDataSource: BillingDataSource
    private val minSkuDetailsMap: MutableMap<String, MinSkuDetails> = mutableMapOf()

    private val _internet: ConnectionLiveData =
        ConnectionLiveData(application.applicationContext)
    val internet = _internet.asFlow()

    private val _buttonState = MutableSharedFlow<ButtonInfo>(1)
    val buttonState = _buttonState.asSharedFlow()

    private val _rewardState = MutableStateFlow(RewardType.NONE)
    val rewardState = _rewardState.asStateFlow()

    private val _adsLiveData: AdsLiveData = AdsLiveData(application.applicationContext)

    private val _currentLevel = MutableStateFlow(Level())
    val currentLevel = _currentLevel.asStateFlow()

    private val _accountStatus = MutableStateFlow(AccountStatus.SIGNED_OUT)
    val accountStatus = _accountStatus.asStateFlow()

    private val _sadMode = MutableStateFlow(false)

    init {
        _sadMode.value = getSadModeValue()
        viewModelScope.launch {
            val skus = arrayOf(
                application.getString(R.string.all_inclusive_pack),
                application.getString(R.string.instant_clues),
                application.getString(R.string.sad_mode),
                application.getString(R.string.instant_letters)
            )
            _billingDataSource = BillingDataSource.getInstance(
                application,
                viewModelScope,
                skus
            )

            launch {
                _sadMode.collect {
                    Log.d(TAG, "SAD MODE: ${_sadMode.value}")
                }
            }
            // Collect and react to purchase button updates from BillingDataSource
            launch {
                combineAndUpdateMinSkuDetails(application.getString(R.string.instant_letters))
            }
            launch {
                combineAndUpdateMinSkuDetails(application.getString(R.string.instant_clues))
            }
            launch {
                combineAndUpdateMinSkuDetails(application.getString(R.string.sad_mode))
            }
            launch {
                combineAndUpdateMinSkuDetails(application.getString(R.string.all_inclusive_pack))
            }

            // Collect and react to internet state updates from ConnectionLiveData
            launch {
                internet.collect {
                    updateUiState()
                }
            }

            launch {
                levelRepository.latestLevelFlow()
                    .collect {
                        _currentLevel.value = it
                    }
            }
            launch {
                _adsLiveData.asFlow().distinctUntilChanged().collect {
                    if (it.second == RewardType.CLUE) {
                        updateCategoryRewarded()
                    } else if (it.second == RewardType.SAD_MODE) {
                        persistSadMode(false)
                    }
                    _rewardState.value = it.second
                    updateUiState()
                }
            }
            launch {
                accountStatus.collect {
                    Log.d(TAG, "Account status: $it")
                }
            }

            launch {
                levelRepository.intervalAttainedFlow(application.getString(R.string.sad_mode_interval))
                    .distinctUntilChanged().collect {
                        Log.d(TAG, "Interval reached: $it")
                        levelRepository.setCountTo(
                            application.getString(R.string.sad_mode_interval),
                            0
                        )
                        if (it && !isSadModePurchased()) {
                            persistSadMode(true)
                            updateUiState()
                        }
                    }
            }
        }
    }

    fun setAccountStatus(accountStatus: AccountStatus) {
        _accountStatus.value = accountStatus
    }

    fun updateRewardState(rewardType: RewardType) {
        _rewardState.value = rewardType
    }

    fun showRewardedVideo(activity: Activity, rewardType: RewardType) {
        _adsLiveData.showRewardedVideo(activity, rewardType)
    }

    fun launchBillingFlow(activity: Activity, sku: String) {
        _billingDataSource.launchBillingFlow(activity, sku)
    }

    fun updateCategoryRewarded() {
        viewModelScope.launch {
            levelRepository.updateCategoryRewarded(true, currentLevel.value.levelInfo.levelId)
        }
    }

    private fun updateUiState() {
        val internetState = _internet.value == true
        if (_internet.value == true && _adsLiveData.value?.first == true) {
            viewModelScope.launch {
                _buttonState.emit(
                    ButtonInfo(
                        ButtonState.AD,
                        minSkuDetailsMap,
                        internetState,
                        _sadMode.value
                    )
                )
            }
        } else {
            viewModelScope.launch {
                _buttonState.emit(
                    ButtonInfo(
                        ButtonState.NO_AD,
                        minSkuDetailsMap,
                        internetState,
                        _sadMode.value
                    )
                )
            }
        }
    }

    private fun persistSadMode(newValue: Boolean) {
        val app = getApplication<Application>()
        app.getSharedPreferences(
            app.getString(R.string.app_preferences),
            Context.MODE_PRIVATE
        )?.let { sp ->
            with(sp.edit()) {
                putBoolean(app.getString(R.string.sad_mode_pref), newValue)
                apply()
            }
        }
        _sadMode.value = newValue
    }

    private fun getSadModeValue(): Boolean {
        val app = getApplication<Application>()
        var sadModeValue = false
        app.getSharedPreferences(
            app.getString(R.string.app_preferences),
            Context.MODE_PRIVATE
        )?.let { sp ->
            sadModeValue = sp.getBoolean(app.getString(R.string.sad_mode_pref), _sadMode.value)
        }
        return sadModeValue
    }

    private suspend fun combineAndUpdateMinSkuDetails(sku: String) {
        combine(
            _billingDataSource.getSkuTitle(sku),
            _billingDataSource.getSkuPrice(sku),
            _billingDataSource.canPurchase(sku),
            _billingDataSource.isPurchased(sku)
        ) { title, price, canPurchase, isPurchased ->
            MinSkuDetails(
                sku = sku,
                title = title.substringBefore(" ("),
                price = price,
                canPurchase = canPurchase,
                isPurchased = isPurchased
            )
        }.distinctUntilChanged().collect {
            minSkuDetailsMap[it.sku] = it
            updateSadMode()
            updateUiState()
        }
    }

    private fun updateSadMode() {
        if (isSadModePurchased()) {
            persistSadMode(false)
        }
    }

    private fun isSadModePurchased(): Boolean {
        val app = getApplication<Application>()
        val sadModeSku = app.getString(R.string.sad_mode)
        val allInclusiveSku = app.getString(R.string.all_inclusive_pack)
        return (minSkuDetailsMap[sadModeSku]?.isPurchased == true || minSkuDetailsMap[allInclusiveSku]?.isPurchased == true)
    }

    fun getShareIntent(screen: String): Intent {
        viewModelScope.launch(Dispatchers.Default) {
            FirebaseAnalytics.getInstance(getApplication<Application>().applicationContext).apply {
                this.logEvent(
                    FirebaseCustom.Events.SHARE_GAME_CLICKED,
                    bundleOf(Pair(FirebaseCustom.Params.ORIGIN_SCREEN, screen))
                )
            }
        }
        val puzzle = "\uD83C\uDD30\uD83C\uDD30\uD83C\uDD31\uD83C\uDD31"
        return Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "Rearrange chars to solve puzzle: $puzzle. Hint: famous band and most importantly - palindrome! Play full game: https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
            )
            type = "text/plain"
        }, "Share Game")
    }

    data class ButtonInfo(
        val state: ButtonState,
        val skus: MutableMap<String, MinSkuDetails>,
        val internet: Boolean,
        val sadMode: Boolean
    )

}