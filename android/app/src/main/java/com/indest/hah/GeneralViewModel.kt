package com.indest.hah

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.indest.hah.ads.Ads
import com.indest.hah.other.ConnectionLiveData
import com.indest.hah.persistance.dto.MinSkuDetails
import com.indest.hah.views.ButtonState
import com.sample.android.trivialdrivesample.billing.BillingDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GeneralViewModel @Inject constructor(
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

    private var _adTest = false

    @Inject lateinit var ads: Ads

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
            viewModelScope.launch {
                combineAndUpdateMinSkuDetails(application.getString(R.string.instant_letters))
            }
            viewModelScope.launch {
                combineAndUpdateMinSkuDetails(application.getString(R.string.instant_clues))
            }
            viewModelScope.launch {
                combineAndUpdateMinSkuDetails(application.getString(R.string.no_intermediate_ads))
            }
            viewModelScope.launch {
                combineAndUpdateMinSkuDetails(application.getString(R.string.full_no_ads))
            }
            viewModelScope.launch {
                internet.collect {
                    updateButtonState()
                }
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
            updateButtonState()
        }
    }

    fun launchBillingFlow(activity: Activity, sku: String) {
        _billingDataSource.launchBillingFlow(activity, sku)
    }

    fun setAd(newValue: Boolean) {
        viewModelScope.launch {
            _adTest = newValue
            updateButtonState()
        }
    }

    private fun updateButtonState() {
        val internetState = _internet.value == true
        if (_internet.value == true && _adTest) {
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

    data class ButtonInfo(
        val state: ButtonState,
        val skus: MutableMap<String, MinSkuDetails>,
        val internet: Boolean
    )

}