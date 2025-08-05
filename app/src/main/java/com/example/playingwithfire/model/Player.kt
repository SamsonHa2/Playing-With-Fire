package com.example.playingwithfire.model

import com.example.playingwithfire.domain.behaviors.ChasePlayerBehavior

data class Player(
    val id: String,
    val name: String,
    var position: Position,
    var wins: Int = 0,
    var bombs: MutableList<Bomb> = ArrayList(),
    var hp: Int = 100,
    val size: Float = 0.9f,
    var bombCount: Int = 1,
    var fireRange: Int = 1,
    var speed: Float = 2f,
    var direction: Direction = Direction.LEFT,
    var behavior: ChasePlayerBehavior = ChasePlayerBehavior(),
    var state: PlayerState = PlayerState.IDLE
){
    fun move(delta: Double) {
        val dx = when (direction) {
            Direction.LEFT -> -1f
            Direction.RIGHT -> 1f
            else -> 0f
        }

        val dy = when (direction) {
            Direction.UP -> -1f
            Direction.DOWN -> 1f
            else -> 0f
        }

        // Properly use delta with speed
        val distance = (speed * delta).toFloat()  // delta is in seconds

        val newX = position.x + dx * distance
        val newY = position.y + dy * distance

        this.position = Position(newX, newY)
    }
}