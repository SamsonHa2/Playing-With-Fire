package com.example.playingwithfire.ui.game

import androidx.lifecycle.ViewModel
import com.example.playingwithfire.domain.GameEngine
import com.example.playingwithfire.model.Direction
import com.example.playingwithfire.model.GameState
import com.example.playingwithfire.model.PlayerState
import com.example.playingwithfire.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(): ViewModel () {

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent. receiveAsFlow()
    val game: GameEngine = GameEngine()
    private var _gameState = MutableStateFlow(game.getGameState())
    val gameState: StateFlow<GameState> = _gameState

    private val eventQueue = mutableListOf<GameEvent>()
    private val queueLock = Any()

    fun updateGame(delta: Double) {
        // Step 1: Copy and clear events atomically
        val eventsToProcess = synchronized(queueLock) {
            val copy = eventQueue.toList()
            eventQueue.clear()
            copy
        }

        // Step 2: Process them
        for (event in eventsToProcess) {
            handleEvent(event)
        }

        // Step 3: Advance game state
        game.update(delta)

        // Step 4: Refresh StateFlows
        _gameState.value = game.getGameState()
    }


    private fun handleEvent(event: GameEvent) {
        val player = game.getPlayers().firstOrNull() ?: return
        when (event) {
            GameEvent.OnBombClick -> game.placeBomb(player)
            GameEvent.OnDirectionRelease -> player.state = PlayerState.IDLE
            GameEvent.OnDownClick -> game.setPlayerRunning(player, Direction.DOWN)
            GameEvent.OnLeftClick -> game.setPlayerRunning(player, Direction.LEFT)
            GameEvent.OnRightClick -> game.setPlayerRunning(player, Direction.RIGHT)
            GameEvent.OnUpClick -> game.setPlayerRunning(player, Direction.UP)
        }
    }

    fun onEvent(event: GameEvent) {
        synchronized(queueLock) {
            eventQueue.add(event)
        }
    }
}