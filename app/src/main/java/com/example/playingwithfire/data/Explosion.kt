package com.example.playingwithfire.data

data class Explosion(
    val affectedPositions: List<Position>,
    var remainingTime: Double = 3.0 // seconds
)
