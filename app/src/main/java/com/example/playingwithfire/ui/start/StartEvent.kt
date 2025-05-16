package com.example.playingwithfire.ui.start

sealed class StartEvent {
    data object OnStartClick: StartEvent()
    data object OnOptionsClick: StartEvent()
}