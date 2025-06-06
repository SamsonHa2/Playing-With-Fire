package com.example.playingwithfire.ui.game

import androidx.lifecycle.ViewModel
import com.example.playingwithfire.model.Bomb
import com.example.playingwithfire.model.Direction
import com.example.playingwithfire.model.Explosion
import com.example.playingwithfire.domain.GameEngine
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
    private var gameState = game.getGameState()
    private val _grid = MutableStateFlow(gameState.grid)
    val grid: StateFlow<Grid> = _grid
    private val _players = MutableStateFlow(gameState.players)
    val players: StateFlow<List<Player>> = _players
    private val _bombs = MutableStateFlow(gameState.bombs)
    val bombs: StateFlow<List<Bomb>> = _bombs
    private val _explosions = MutableStateFlow(gameState.explosions)
    val explosions: StateFlow<List<Explosion>> = _explosions
    private val _powerUps = MutableStateFlow(gameState.powerUps)
    val powerUps: StateFlow<List<PowerUp>> = _powerUps

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
        gameState = game.getGameState()

        // Step 4: Refresh StateFlows
        _players.value = gameState.players
        _bombs.value = gameState.bombs
        _explosions.value = gameState.explosions
        _powerUps.value = gameState.powerUps
        _grid.value = gameState.grid
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