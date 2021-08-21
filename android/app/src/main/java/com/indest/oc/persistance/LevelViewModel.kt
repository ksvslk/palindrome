package com.indest.oc.persistance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class LevelViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    val nr:Int = savedStateHandle["nr"] ?:
        throw IllegalArgumentException("missing level nr")
}