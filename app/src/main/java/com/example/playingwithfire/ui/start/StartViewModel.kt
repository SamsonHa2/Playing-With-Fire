package com.example.playingwithfire.ui.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.playingwithfire.util.Routes
import com.example.playingwithfire.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartViewModel @Inject constructor(): ViewModel() {
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent. receiveAsFlow()

    fun onEvent(event: StartEvent) {
        when(event) {
            is StartEvent.OnStartClick -> {
                sendUiEvent(UiEvent.Navigate(Routes.GAME))
            }
            is StartEvent.OnOptionsClick -> {
                //sendUiEvent(UiEvent.Navigate(Routes.OPTIONS))
            }
        }
    }

    private fun sendUiEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }
}