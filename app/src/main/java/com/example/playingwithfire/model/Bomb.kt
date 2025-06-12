package com.example.playingwithfire.model

import java.util.UUID

data class Bomb(
    val position: Position,
    val range: Int,
    var remainingTime: Double = 3.0, // seconds
    val totalTime: Double = 3.0,
    val id: UUID = UUID.randomUUID(),
)
