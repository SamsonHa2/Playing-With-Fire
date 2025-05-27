package com.example.playingwithfire.ui.game

import androidx.lifecycle.ViewModel
import com.example.playingwithfire.data.Bomb
import com.example.playingwithfire.data.Direction
import com.example.playingwithfire.data.Explosion
import com.example.playingwithfire.data.Game
import com.example.playingwithfire.data.GameGrid
import com.example.playingwithfire.data.Player
import com.example.playingwithfire.data.PowerUp
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
    val game: Game = Game()
    private val _grid = MutableStateFlow(game.grid)
    val grid: StateFlow<GameGrid> = _grid
    private val _players = MutableStateFlow(game.getPlayers())
    val players: StateFlow<List<Player>> = _players
    private val _bombs = MutableStateFlow(game.getBombs())
    val bombs: StateFlow<List<Bomb>> = _bombs
    private val _explosions = MutableStateFlow(game.getExplosions())
    val explosions: StateFlow<List<Explosion>> = _explosions
    private val _powerUps = MutableStateFlow(game.getPowerUps())
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

        // Step 4: Refresh StateFlows
        _players.value = game.getPlayers()
        _bombs.value = game.getBombs()
        _explosions.value = game.getExplosions()
        _powerUps.value = game.getPowerUps()
        _grid.value = game.grid
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