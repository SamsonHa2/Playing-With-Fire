package com.example.playingwithfire.model

data class Bomb(
    val position: Position,
    val range: Int,
    var remainingTime: Double = 3.0 // seconds
)
