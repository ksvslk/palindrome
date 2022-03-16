package com.indest.hah.game.viewmodel

import com.indest.hah.game.creation.WindowSizeClass
import com.indest.hah.general.AccountStatus
import com.indest.hah.persistance.dto.Level

data class UiState(
    val state: ContainerState = ContainerState.LOADING,
    val level: Level,
    val accountStatus: AccountStatus = AccountStatus.NOT_SET,
    val bookMode: BookMode = BookMode.NOT_SET,
    val windowSizeClass: WindowSizeClass = WindowSizeClass.NOT_SET,
    val solutionData: SolutionData,
    val refreshLevelUi: Boolean = false
)
