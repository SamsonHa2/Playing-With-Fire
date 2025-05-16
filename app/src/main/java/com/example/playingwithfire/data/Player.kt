package com.example.playingwithfire.data

data class Player(
    val id: String,
    val name: String,
    var position: Position,
    val size: Float = 0.9f,
    val isAlive: Boolean = true,
    val bombCount: Int = 1,
    val fireRange: Int = 1,
    val speed: Float = 0.5f
){
    fun move(delta: Double, direction: Direction) {
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

        setPosition(newX, newY)
    }

    private fun setPosition(x: Float, y: Float){
        this.position = Position(x, y)
    }
}