package com.example.playingwithfire.model

data class Explosion(
    val position: Position,
    val direction: Direction,
    var type: ExplosionType,
    var remainingTime: Double = 1.5, // seconds
    var damagedPlayers: MutableSet<String> = mutableSetOf()
)
