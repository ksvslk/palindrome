package com.indest.hah.game

import com.indest.hah.game.levelcreation.WindowSizeClass
import com.indest.hah.other.AccountStatus
import com.indest.hah.other.BookMode
import com.indest.hah.persistance.dto.Level

data class UiState(
    val state: ContainerState = ContainerState.LOADING,
    val level: Level,
    val accountStatus: AccountStatus = AccountStatus.NOT_SET,
    val bookMode: BookMode = BookMode.NOT_SET,
    val windowSizeClass: WindowSizeClass = WindowSizeClass.NOT_SET,
    val solutionData: SolutionData
)
