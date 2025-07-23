package com.example.playingwithfire.model

data class GameState(
    val grid: Grid,
    val players: List<Player>,
    val bombs: List<Bomb>,
    val explosions: List<Explosion>,
    val powerUps: List<PowerUp>,
    val round: Int
)