package com.example.playingwithfire.model

data class Explosion(
    val affectedPositions: List<Position>,
    var remainingTime: Double = 3.0 // seconds
)
