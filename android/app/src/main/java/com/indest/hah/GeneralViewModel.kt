package com.indest.hah

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import com.indest.hah.persistance.dto.Sku
import com.indest.hah.util.ConnectionLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

@HiltViewModel
class GeneralViewModel @Inject constructor(application: Application) : ViewModel() {

    private val TAG = "GeneralViewModel"

    private val connectionLiveData: ConnectionLiveData =
        ConnectionLiveData(application.applicationContext)
    val internetState = connectionLiveData.asFlow()

    private var _internet = false
    private var _ad = false
    private var _purchases = mutableListOf<String>()

    private val _buttonState = MutableStateFlow(ButtonInfo(ButtonState.NO_AD, mutableListOf()))
    val buttonState = _buttonState.asStateFlow()

    fun setInternet(newValue:Boolean)
    {
        _internet = newValue
        updateButtonState()
    }

    fun setAd(newValue:Boolean)
    {
        _ad = newValue
        updateButtonState()
    }

    fun addPurchase(sku:String)
    {
        _purchases.add(sku)
        updateButtonState()
    }

    fun removePurchase(sku:String)
    {
        _purchases.remove(sku)
        updateButtonState()
    }

    private fun updateButtonState()
    {
        if (_internet && _ad) {
            _buttonState.value = ButtonInfo(ButtonState.AD, _purchases.toMutableList())
        }
        else {
            _buttonState.value = ButtonInfo(ButtonState.NO_AD,  _purchases.toMutableList())
        }
    }

    data class ButtonInfo(
        val state: ButtonState,
        val skus: MutableList<String>
    )

}