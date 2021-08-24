package com.indest.oc.persistance

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LevelViewModel @Inject constructor(
        private val levelRepository: LevelRepository
    ): ViewModel() {

    val level = MutableLiveData<Level>()

    init {
        viewModelScope.launch {
            level.value = levelRepository.getLastLevel()
        }
    }

    fun updateShuffledPuzzle(shuffled: String)
    {
        level.value!!.shuffledPuzzle = shuffled
        viewModelScope.launch {
            levelRepository.saveLevel(level.value!!)
        }
    }
}