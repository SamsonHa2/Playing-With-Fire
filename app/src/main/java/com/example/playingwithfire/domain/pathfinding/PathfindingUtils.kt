package com.example.playingwithfire.domain.pathfinding

import com.example.playingwithfire.domain.pathfinding.Pathfinding.shortestPath
import com.example.playingwithfire.model.Bomb
import com.example.playingwithfire.model.Direction
import com.example.playingwithfire.model.GameState
import com.example.playingwithfire.model.Player
import com.example.playingwithfire.model.Position
import com.example.playingwithfire.model.TileType

fun mapInRange(map: List<List<Char>>, location: Position): Boolean {
    val (x, y) = location
    return x >= 0 && x < map[0].size && y >= 0 && y < map.size
}

fun mapEquals(map: List<List<Char>>, location: Position, chars: List<Char>): Boolean {
    if (!mapInRange(map, location)) return false
    return chars.contains(map[location.y.toInt()][location.x.toInt()])
}

fun mapEquals(map: List<List<Char>>, location: Position, char: Char): Boolean {
    if (!mapInRange(map, location)) return false
    return char == map[location.y.toInt()][location.x.toInt()]
}

fun shift(location: Position, direction: Direction, offset: Int = 1): Position = when (direction) {
    Direction.UP -> Position(location.x, location.y - offset)
    Direction.DOWN -> Position(location.x, location.y + offset)
    Direction.LEFT -> Position(location.x - offset, location.y)
    Direction.RIGHT -> Position(location.x + offset, location.y)
    else -> throw IllegalArgumentException("Invalid direction")
}

fun canEscape(dangerZones: List<List<Char>>, escapeFromLocation: Position): Boolean {
    val pathToSafety = shortestPath(
        map = dangerZones,
        start = escapeFromLocation,
        endChar = '.',
        costs = PathfindingCosts.canEscape
    )

    return when {
        pathToSafety == null -> false
        pathToSafety.size > 3 -> false
        else -> true
    }
}


fun canEscapeAfterBombPlaced(player: Player, gameState: GameState, bomb: Bomb): Boolean {
    val bombPlacedGameState = GameState(
        gameState.grid,
        gameState.players,
        gameState.bombs + bomb,
        gameState.explosions,
        gameState.powerUps,
        gameState.round
    )
    val placedBombMap = calculateDangerZones(player, bombPlacedGameState)
    val ans = canEscape(placedBombMap, player.position)
    return ans
}

fun calculateDangerZones(curPlayer: Player, gameState: GameState): List<List<Char>> {
    val width = gameState.grid.width
    val height = gameState.grid.height

    // Start with grid tile types
    val charGrid = Array(height) { y ->
        Array(width) { x ->
            when (gameState.grid[x, y].type) {
                TileType.UnbreakableWall -> '#'
                TileType.BreakableWall -> '+'
                TileType.Empty -> '.'
            }
        }
    }

    // mark power ups
    for (powerUp in gameState.powerUps) {
        val x = powerUp.position.x.toInt()
        val y = powerUp.position.y.toInt()
        if (x !in 0 until width || y !in 0 until height) continue
        if (charGrid[y][x] == '.') {
            charGrid[y][x] = 'p'
        }
    }

    //Mark enemies
    for (player in gameState.players) {
        if (player == curPlayer) continue
        val x = player.position.x.toInt()
        val y = player.position.y.toInt()
        if (x !in 0 until width || y !in 0 until height) continue
        charGrid[y][x] = 'e'
    }

    // Mark bombs
    // Predict explosions for all bombs
    for (bomb in gameState.bombs) {
        val originX = bomb.position.x.toInt()
        val originY = bomb.position.y.toInt()

        // Always include bomb's own tile
        if (originX in 0 until width && originY in 0 until height) {
            charGrid[originY][originX] = 'b'
        }

        val directions = listOf(
            Pair(1, 0),   // Right
            Pair(-1, 0),  // Left
            Pair(0, -1),  // Up
            Pair(0, 1)    // Down
        )

        for ((dx, dy) in directions) {
            for (i in 1..bomb.range) {
                val x = originX + dx * i
                val y = originY + dy * i

                if (x !in 0 until width || y !in 0 until height) break

                val tile = gameState.grid[x, y]
                if (tile.type == TileType.UnbreakableWall) break

                if (charGrid[y][x] != '#' && charGrid[y][x] != '+') {
                    charGrid[y][x] = 'x'
                }

                if (tile.type == TileType.BreakableWall) break
            }
        }

        // Mark actual bomb location after explosion prediction
        charGrid[originY][originX] = 'b'
    }

    // Mark explosions
    for (explosion in gameState.explosions) {
        val x = explosion.position.x.toInt()
        val y = explosion.position.y.toInt()
        if (x !in 0 until width || y !in 0 until height) continue
        charGrid[y][x] = '*'
    }

    // Convert to List<List<Char>>
    return charGrid.map { it.toList() }
}