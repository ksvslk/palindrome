package com.indest.hah

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import com.indest.hah.util.ConnectionLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GeneralViewModel @Inject constructor(application: Application) : ViewModel() {

    private val TAG = "GeneralViewModel"

    private val connectionLiveData: ConnectionLiveData =
        ConnectionLiveData(application.applicationContext)
    val internetState = connectionLiveData.asFlow()

}