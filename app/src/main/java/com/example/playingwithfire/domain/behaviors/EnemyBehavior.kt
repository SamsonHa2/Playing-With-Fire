package com.example.playingwithfire.domain.behaviors

import com.example.playingwithfire.model.*

interface EnemyBehavior {
    fun decideMove(player: Player, gameState: GameState): Moves
}