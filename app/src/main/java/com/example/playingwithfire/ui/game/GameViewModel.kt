package com.example.playingwithfire.ui.game

import androidx.lifecycle.ViewModel
import com.example.playingwithfire.model.Bomb
import com.example.playingwithfire.model.Direction
import com.example.playingwithfire.model.Explosion
import com.example.playingwithfire.domain.GameEngine
import com.example.playingwithfire.model.GameState
import com.example.playingwithfire.model.Grid
import com.example.playingwithfire.model.Player
import com.example.playingwithfire.model.PowerUp
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
        when (event){
            GameEvent.OnBombClick -> game.placeBomb(game.getPlayers()[0])
            GameEvent.OnDownClick -> game.getPlayers()[0].direction = Direction.DOWN
            GameEvent.OnLeftClick -> game.getPlayers()[0].direction = Direction.LEFT
            GameEvent.OnRightClick -> game.getPlayers()[0].direction = Direction.RIGHT
            GameEvent.OnUpClick -> game.getPlayers()[0].direction = Direction.UP
        }
    }

    fun onEvent(event: GameEvent) {
        synchronized(queueLock) {
            eventQueue.add(event)
        }
    }
}