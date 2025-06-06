package com.example.playingwithfire.domain.pathfinding

object PathfindingCosts {
    // '#' = unbreakable, '*' = existing blast, 'b' = bomb, 'e' = enemy, '+' = breakable, 'x' = predict blast, '.' = empty, 'p' = power up
    val avoidDanger =   mapOf('#' to -1, '*' to  3, 'b' to  3, 'e' to 10, '+' to -1, 'x' to  1, '.' to 1, 'p' to 1)
    val powerUp =       mapOf('#' to -1, '*' to -1, 'b' to -1, 'e' to  1, '+' to  3, 'x' to 30, '.' to 1, 'p' to 1)
    val breakableWall = mapOf('#' to -1, '*' to -1, 'b' to -1, 'e' to  3, '+' to  3, 'x' to -1, '.' to 1, 'p' to 1)
    val enemy =         mapOf('#' to -1, '*' to -1, 'b' to -1, 'e' to  1, '+' to  3, 'x' to -1, '.' to 1, 'p' to 1)
    val canEscape =     mapOf('#' to -1, '*' to 10, 'b' to 10, 'e' to -1, '+' to -1, 'x' to 10, '.' to 1, 'p' to 1)
}