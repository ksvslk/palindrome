package com.indest.hah.general

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.indest.hah.R
import com.indest.hah.ads.AdsLiveData
import com.indest.hah.ads.RewardType
import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.dto.MinSkuDetails
import com.indest.hah.persistance.level.LevelRepository
import com.indest.hah.views.ButtonState
import com.sample.android.trivialdrivesample.billing.BillingDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
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
            application.getSharedPreferences(
                application.getString(R.string.app_preferences),
                Context.MODE_PRIVATE
            )?.let { sp ->
                _sadMode.value =
                    sp.getBoolean(application.getString(R.string.sad_mode_pref), _sadMode.value)
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
                    Log.d(TAG, "Reward given: ${it.second}")
                    if (it.second == RewardType.CLUE) {
                        updateCategoryRewarded()
                    } else if (it.second == RewardType.SAD_MODE) {
                        deactivateSadMode()
                    }
                    _rewardState.value = it.second
                    updateUiState()
                }
            }
            launch {
                _accountStatus.collect {
                    Log.d(TAG, "Signed in state changed to: $it")
                }
            }
            launch {
                levelRepository.intervalAttainedFlow(application.getString(R.string.sad_mode_interval))
                    .distinctUntilChanged().collect {
                        Log.d(TAG, "Interval reached: $it")
                        if (it) {
                            levelRepository.setCountTo(
                                application.getString(R.string.sad_mode_interval),
                                0
                            )
                            updateSadMode(defaultValue = true)
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

    // Invoked after watching rewarded video
    private fun deactivateSadMode() {
        val app = getApplication<Application>()

        val newSadMode = false

        // Persist
        app.getSharedPreferences(
            app.getString(R.string.app_preferences),
            Context.MODE_PRIVATE
        )?.let { sp ->
            with(sp.edit()) {
                putBoolean(app.getString(R.string.sad_mode_pref), newSadMode)
                apply()
            }
        }

        _sadMode.value = newSadMode
    }

    // Invoked after interval of Sad Mode attained
    private fun updateSadMode(defaultValue:Boolean = _sadMode.value) {
        val app = getApplication<Application>()

        // First set the Sad Mode TRUE because it is called when interval reached
        var isSadMode = defaultValue

        // If it is purchased set it FALSE
        buttonState.replayCache.lastOrNull()?.let { b ->
            val allInclusiveInfo =
                b.skus.values.find { it.sku == app.getString(R.string.all_inclusive_pack) }
            val sadModeInfo = b.skus.values.find { it.sku == app.getString(R.string.sad_mode) }
            if (allInclusiveInfo?.isPurchased == true || sadModeInfo?.isPurchased == true) {
                isSadMode = false
            }
        }

        if (isSadMode != _sadMode.value)
        {
            // Persist new value
            app.getSharedPreferences(
                app.getString(R.string.app_preferences),
                Context.MODE_PRIVATE
            )?.let { sp ->
                with(sp.edit()) {
                    putBoolean(app.getString(R.string.sad_mode_pref), isSadMode)
                    apply()
                }
            }
            // Update UI
            _sadMode.value = isSadMode
        }
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

    data class ButtonInfo(
        val state: ButtonState,
        val skus: MutableMap<String, MinSkuDetails>,
        val internet: Boolean,
        val sadMode: Boolean
    )

}