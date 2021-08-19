package com.indest.oc.game

enum class GameState : IGameState {
    INITIALIZING,
    PLAYING,
    PAUSED,
    CHECKING,
    COMPLETED,
}