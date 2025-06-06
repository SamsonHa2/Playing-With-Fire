package com.example.playingwithfire.domain.behaviors

import com.example.playingwithfire.domain.pathfinding.Pathfinding.shortestPath
import com.example.playingwithfire.domain.pathfinding.PathfindingCosts
import com.example.playingwithfire.domain.pathfinding.calculateDangerZones
import com.example.playingwithfire.domain.pathfinding.canEscape
import com.example.playingwithfire.domain.pathfinding.canEscapeAfterBombPlaced
import com.example.playingwithfire.domain.pathfinding.mapEquals
import com.example.playingwithfire.domain.pathfinding.shift
import com.example.playingwithfire.model.*
import kotlin.math.abs

class ChasePlayerBehavior : EnemyBehavior {

    // Strategy core; given the current game state, return a move to make.
    override fun decideMove(player: Player, gameState: GameState): Moves {
        if (!isPlayerCentered(player)) return Moves.NO_CHANGE

        val dangerZones = calculateDangerZones(player, gameState)

        // If in danger zone, try to find a path to nearest safe location.
        if (isInDanger(player.position, dangerZones)) {
            val escapeMove = tryEscape(player, dangerZones)
            if (escapeMove != null) return escapeMove
        }

        // If placing a bomb will trap enemy, place bomb
        if (shouldTrapEnemy(player, gameState, dangerZones)) {
            println("Enemy is probably trapped by blast, placing bomb...")
            return Moves.PLACE_BOMB
        }

        // If next to a destructible wall, place bomb
        if (shouldDestroyNearbyWall(player, gameState, dangerZones)) {
            println("Placing bomb to destroy destructible wall...")
            return Moves.PLACE_BOMB
        }

        // Try to find a path to nearest accessible power up.
        val powerUpPath = shortestPath(dangerZones, player.position, 'p', PathfindingCosts.powerUp)
        if (powerUpPath != null) {
            println("Seeking power up...")
            return powerUpPath.first()
        }

        // Try to find a path to the nearest breakable wall.
        val breakableWallPath = shortestPath(dangerZones, player.position, '+', PathfindingCosts.breakableWall)
        if (breakableWallPath != null) {
            println("Homing in to destructible wall...")
            return breakableWallPath.first()
        }

        // Try to find a path to the nearest enemy.
        val toEnemyPath = shortestPath(dangerZones, player.position, 'e', PathfindingCosts.enemy)
        if (toEnemyPath != null) {
            println("Homing in to enemy...")
            return toEnemyPath.first()
        }

        // Don't know what else to do
        println("Nothing else to do.") // DEBUG
        return Moves.DO_NOTHING
    }

    private fun isPlayerCentered(player: Player): Boolean {
        val centeredX = abs(player.position.x % 1 - 0.5f) < 0.05f
        val centeredY = abs(player.position.y % 1 - 0.5f) < 0.05f
        return centeredX && centeredY
    }

    private fun isInDanger(position: Position, dangerZones: List<List<Char>>): Boolean {
        return mapEquals(dangerZones, position, listOf('x', '*', 'b'))
    }

    private fun tryEscape(player: Player, dangerZones: List<List<Char>>): Moves? {
        val path = shortestPath(
            map = dangerZones,
            start = player.position,
            endChar = '.',
            costs = PathfindingCosts.avoidDanger
        )

        if (path != null) {
            println("Trying to escape to a safe place...")
            return path.first()
        }

        if (player.bombCount > 0 && !mapEquals(dangerZones, player.position, listOf('b'))) {
            println("It's a trap!")
            return Moves.PLACE_BOMB
        }
        return null
    }

    private fun shouldTrapEnemy(player: Player, gameState: GameState, dangerZones: List<List<Char>>): Boolean {
        for (otherPlayer in gameState.players) {
            if (otherPlayer == player || player.bombCount <= 0) continue
            if (!canEscape(dangerZones, player.position)) continue
            val fakeBomb = Bomb(player.position, player.fireRange)
            if (!canEscapeAfterBombPlaced(otherPlayer, gameState, fakeBomb) && canEscapeAfterBombPlaced(player, gameState, fakeBomb)) {
                return true
            }
        }
        return false
    }

    private fun shouldDestroyNearbyWall(player: Player, gameState: GameState, dangerZones: List<List<Char>>): Boolean {
        if (player.bombCount <= 0) return false
        val directions = listOf(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)

        return directions.any { direction ->
            val target = shift(player.position, direction)
            val isBreakableWall = mapEquals(dangerZones, target, listOf('+'))
            val canEscape = canEscapeAfterBombPlaced(player, gameState, Bomb(player.position, player.fireRange))
            isBreakableWall && canEscape
        }
    }
}
