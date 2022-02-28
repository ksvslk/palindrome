package com.indest.hah

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.indest.hah.persistance.billing.BillingRepository
import com.indest.hah.persistance.dto.MinSkuDetails
import com.indest.hah.util.ConnectionLiveData
import com.sample.android.trivialdrivesample.billing.BillingDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.min

@HiltViewModel
class GeneralViewModel @Inject constructor(
    application: Application,
    private val billingRepository: BillingRepository,
) : AndroidViewModel(application) {

    private val TAG = "GeneralViewModel"

    private lateinit var _billingDataSource: BillingDataSource
    private val minSkuDetailsMap: MutableMap<String, MinSkuDetails> = mutableMapOf()

    private val _internetState: ConnectionLiveData =
        ConnectionLiveData(application.applicationContext)
    val internetState = _internetState.asFlow()

    private var _internetTest = false
    private var _adTest = false

    private val _buttonState =
        MutableStateFlow(ButtonInfo(ButtonState.NO_AD, mutableMapOf(), _internetTest))
    val buttonState = _buttonState.asStateFlow()

    init {
        viewModelScope.launch {
            val skus = billingRepository.getAllSkuNames().toTypedArray()
            _billingDataSource = BillingDataSource.getInstance(
                application,
                viewModelScope,
                skus
            )
            viewModelScope.launch {
                combineMinSkuDetails(application.getString(R.string.instant_letters)).collectLatest {
                    minSkuDetailsMap[it.sku] = it
                    updateButtonState()
                }
            }
            viewModelScope.launch {
                combineMinSkuDetails(application.getString(R.string.instant_clues)).collectLatest {
                    minSkuDetailsMap[it.sku] = it
                    updateButtonState()
                }
            }
            viewModelScope.launch {
                combineMinSkuDetails(application.getString(R.string.no_intermediate_ads)).collectLatest {
                    minSkuDetailsMap[it.sku] = it
                    updateButtonState()
                }
            }
            viewModelScope.launch {
                combineMinSkuDetails(application.getString(R.string.full_no_ads)).collectLatest {
                    minSkuDetailsMap[it.sku] = it
                    updateButtonState()
                }
            }
        }
    }

    private fun combineMinSkuDetails(sku: String): Flow<MinSkuDetails> {
        return combine(
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
        }
    }

    fun makePurchase(activity: Activity, sku: String) {
        _billingDataSource.launchBillingFlow(activity, sku)
    }

    fun setInternet(newValue: Boolean) {
        viewModelScope.launch {
            _internetTest = newValue
            updateButtonState()
        }
    }

    fun setAd(newValue: Boolean) {
        viewModelScope.launch {
            _adTest = newValue
            updateButtonState()
        }
    }

    fun addPurchase(sku: String) {
        viewModelScope.launch {
            billingRepository.setSkuPurchased(sku)
        }
    }

    fun removePurchase(sku: String) {
        viewModelScope.launch {
            billingRepository.setSkuPurchased(sku, value = false)
        }
    }

    private fun updateButtonState() {
        Log.d(TAG, minSkuDetailsMap.toString())
        if (_internetTest && _adTest) {
            _buttonState.value =
                ButtonInfo(
                    ButtonState.AD,
                    minSkuDetailsMap,
                    _internetTest
                )
        } else {
            _buttonState.value =
                ButtonInfo(ButtonState.NO_AD, minSkuDetailsMap, _internetTest)
        }
    }

    data class ButtonInfo(
        val state: ButtonState,
        val skus: MutableMap<String, MinSkuDetails>,
        val internet: Boolean
    )

}