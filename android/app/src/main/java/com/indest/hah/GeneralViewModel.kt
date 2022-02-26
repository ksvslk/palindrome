package com.indest.hah

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.indest.hah.game.enums.Internet
import com.indest.hah.util.ConnectionLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GeneralViewModel @Inject constructor(application: Application) : ViewModel() {

    private val TAG = "GeneralViewModel"

    private val _internetState: ConnectionLiveData =
        ConnectionLiveData(application.applicationContext)
    val internetState = _internetState.asFlow()

    private var _internetTest = false
    private var _adTest = false
    private var _purchasesTest = mutableListOf<String>()

    private val _buttonState = MutableStateFlow(ButtonInfo(ButtonState.NO_AD, mutableListOf(), _internetTest))
    val buttonState = _buttonState.asStateFlow()

    init {
        viewModelScope.launch {
            internetState.distinctUntilChanged().collect {
                //updateButtonState()
            }
        }
    }

    fun setInternet(newValue:Boolean)
    {
        _internetTest = newValue
        updateButtonState()
    }

    fun setAd(newValue:Boolean)
    {
        _adTest = newValue
        updateButtonState()
    }

    fun addPurchase(sku:String)
    {
        _purchasesTest.add(sku)
        updateButtonState()
    }

    fun removePurchase(sku:String)
    {
        _purchasesTest.remove(sku)
        updateButtonState()
    }

    private fun updateButtonState()
    {
        if (_internetTest && _adTest) {
            _buttonState.value = ButtonInfo(ButtonState.AD, _purchasesTest.toMutableList(), _internetTest)
        }
        else {
            _buttonState.value = ButtonInfo(ButtonState.NO_AD,  _purchasesTest.toMutableList(), _internetTest)
        }
    }

    data class ButtonInfo(
        val state: ButtonState,
        val skus: MutableList<String>,
        val internet: Boolean
        )

}