package com.indest.hah

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.indest.hah.ads.AdsLiveData
import com.indest.hah.ads.RewardType
import com.indest.hah.other.ConnectionLiveData
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

    init {
        viewModelScope.launch {
            val skus = arrayOf(
                application.getString(R.string.full_no_ads),
                application.getString(R.string.instant_clues),
                application.getString(R.string.no_intermediate_ads),
                application.getString(R.string.instant_letters)
            )
            _billingDataSource = BillingDataSource.getInstance(
                application,
                viewModelScope,
                skus
            )

            // Collect and react to purchase button updates from BillingDataSource
            launch {
                combineAndUpdateMinSkuDetails(application.getString(R.string.instant_letters))
            }
            launch {
                combineAndUpdateMinSkuDetails(application.getString(R.string.instant_clues))
            }
            launch {
                combineAndUpdateMinSkuDetails(application.getString(R.string.no_intermediate_ads))
            }
            launch {
                combineAndUpdateMinSkuDetails(application.getString(R.string.full_no_ads))
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
                    }
                    _rewardState.value = it.second
                    updateUiState()
                }
            }
        }
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
                        internetState
                    )
                )
            }
        } else {
            viewModelScope.launch {
                _buttonState.emit(ButtonInfo(ButtonState.NO_AD, minSkuDetailsMap, internetState))
            }
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
            updateUiState()
        }
    }

    data class ButtonInfo(
        val state: ButtonState,
        val skus: MutableMap<String, MinSkuDetails>,
        val internet: Boolean
    )
}