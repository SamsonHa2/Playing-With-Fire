package com.example.playingwithfire.ui.game

sealed class GameEvent {
    data object OnUpClick: GameEvent()
    data object OnDownClick: GameEvent()
    data object OnLeftClick: GameEvent()
    data object OnRightClick: GameEvent()
    data object OnBombClick: GameEvent()
}