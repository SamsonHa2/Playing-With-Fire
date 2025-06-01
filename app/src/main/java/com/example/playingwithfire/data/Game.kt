package com.example.playingwithfire.data

import android.util.Log
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.min

enum class Moves {
    MOVE_UP, MOVE_DOWN, MOVE_LEFT, MOVE_RIGHT, PLACE_BOMB, DO_NOTHING, NO_CHANGE
}

object PathfindingCosts {
    // '#' = unbreakable, '*' = existing blast, 'b' = bomb, 'e' = enemy, '+' = breakable, 'x' = predict blast, '.' = empty, 'p' = power up
    val avoidDanger =   mapOf('#' to -1, '*' to  3, 'b' to  3, 'e' to 10, '+' to -1, 'x' to  1, '.' to 1, 'p' to 1)
    val powerUp =       mapOf('#' to -1, '*' to -1, 'b' to -1, 'e' to  1, '+' to  3, 'x' to 30, '.' to 1, 'p' to 1)
    val breakableWall = mapOf('#' to -1, '*' to -1, 'b' to -1, 'e' to  3, '+' to  3, 'x' to -1, '.' to 1, 'p' to 1)
    val enemy =         mapOf('#' to -1, '*' to -1, 'b' to -1, 'e' to  1, '+' to  3, 'x' to -1, '.' to 1, 'p' to 1)
    val canEscape =     mapOf('#' to -1, '*' to 10, 'b' to 10, 'e' to -1, '+' to -1, 'x' to 10, '.' to 1, 'p' to 1)
}

class Game {
    val grid: GameGrid = generateGameGrid(
        width = 15,
        height = 13,
        breakableWallCount = 50,
        unbreakableWallCount = 30
    )

    private val players = mutableMapOf<String, Player>()
    private val bombs = mutableListOf<Bomb>()
    private val explosions = mutableListOf<Explosion>()
    private val powerUps = mutableListOf<PowerUp>()

    init {
        spawnPlayer("Player 1", "Player 1", Position(1.5f,1.5f))
        spawnPlayer("Bot 1", "Bot 1", Position(13.5f,11.5f))
    }

    private fun spawnPlayer(id: String, name: String, position: Position) {
        if (players.containsKey(id)) return
        if (grid[position.x.toInt(), position.y.toInt()].type != TileType.Empty) return

        players[id] = Player(
            id = id,
            position = position,
            name = name,
        )
    }

    private fun updatePlayer(id: String, updated: Player) {
        if (players.containsKey(id)) {
            players[id] = updated
        }
    }

    fun update(delta: Double) {
        updateBombs(delta)
        updateExplosions(delta)
        updatePlayers(delta)
    }

    private fun updatePlayers(delta: Double) {
        for ((id, player) in players) {
            if (id == "Bot 1"){
                when (calculateNextMove(player)){
                    Moves.MOVE_UP -> player.direction = Direction.UP
                    Moves.MOVE_DOWN -> player.direction = Direction.DOWN
                    Moves.MOVE_LEFT -> player.direction = Direction.LEFT
                    Moves.MOVE_RIGHT -> player.direction = Direction.RIGHT
                    Moves.PLACE_BOMB -> placeBomb(player)
                    Moves.DO_NOTHING -> player.direction = Direction.NONE
                    Moves.NO_CHANGE -> { /* do nothing */ }
                }
            }
            val movedPlayer = player.copy().apply { move(delta) }
            val playerRadius = player.size / 2
            val direction = player.direction

            val targetX = when (direction) {
                Direction.LEFT  -> (movedPlayer.position.x - playerRadius).toInt()
                Direction.RIGHT -> (movedPlayer.position.x + playerRadius).toInt()
                else            -> movedPlayer.position.x.toInt()
            }

            val targetY = when (direction) {
                Direction.UP    -> (movedPlayer.position.y - playerRadius).toInt()
                Direction.DOWN  -> (movedPlayer.position.y + playerRadius).toInt()
                else            -> movedPlayer.position.y.toInt()
            }

            val diaTargetX = getOccupiedTile(movedPlayer.position.x, playerRadius)
            val diaTargetY = getOccupiedTile(movedPlayer.position.y, playerRadius)

            val mainTile = grid[targetX, targetY]
            val diaTile = grid[diaTargetX, diaTargetY]

            if (willCollide(mainTile, diaTile)) {
                val tilePos = grid[player.position.x.toInt(), player.position.y.toInt()].position
                movedPlayer.position = resetBlockedPosition(tilePos, player.direction, player.position, player.size / 2)
            }

            handleExplosionCollision(movedPlayer, mainTile.position, diaTile.position)

            val finalPlayer = if (id == "Player 1"){
                collectPowerUp(movedPlayer, mainTile.position, diaTile.position).copy(direction = Direction.NONE)
            }  else {
                collectPowerUp(movedPlayer, mainTile.position, diaTile.position).copy()
            }
            updatePlayer(id, finalPlayer)
        }
    }

    fun placeBomb(player: Player) {
        if (player.bombCount > 0) {
            player.bombCount -= 1
            val bomb = spawnBomb(player.position, player.fireRange)
            player.bombs += bomb
        }
    }

    private fun updateBombs(delta: Double) {
        val iterator = bombs.iterator()
        while (iterator.hasNext()) {
            val bomb = iterator.next()
            bomb.remainingTime -= delta
            if (bomb.remainingTime <= 0) {
                val explosion = explodeBomb(bomb)
                explosions.add(explosion)
                iterator.remove()
            }
        }
    }

    private fun updateExplosions(delta: Double) {
        val iterator = explosions.iterator()
        while (iterator.hasNext()) {
            val explosion = iterator.next()
            explosion.remainingTime -= delta
            if (explosion.remainingTime <= 0) {
                iterator.remove()
            }
        }
    }

    private fun willCollide(mainTile: Tile, diaTile: Tile): Boolean {
        return mainTile.type != TileType.Empty || diaTile.type != TileType.Empty
    }

    private fun handleExplosionCollision(player: Player, mainTilePos: Position, diaTilePos: Position): Player{
        val explosionPositions = explosions.flatMap { it.affectedPositions }
        if (explosionPositions.contains(mainTilePos) || explosionPositions.contains(diaTilePos)){
            Log.d("GameEvent", "Player stepped in explosion")
        }
        return player
    }

    private fun collectPowerUp(player: Player, mainTilePos: Position, diaTilePos: Position): Player{
        val powerUp = powerUps.find { it.position == mainTilePos || it.position == diaTilePos }
        if (powerUp != null) {
            Log.d("GameEvent", "Player collected power-up: ${powerUp.type}")
            when (powerUp.type){
                PowerUpType.FireRange -> player.fireRange += 1
                PowerUpType.ExtraBomb -> player.bombCount += 1
                PowerUpType.Speed -> player.speed = min( player.speed * 1.5f, 25.0f)
            }
            powerUps.remove(powerUp)
        }
        return player
    }

    private fun resetBlockedPosition(tilePosition: Position, direction: Direction, playerPosition: Position, playerRad: Float): Position {
        return when (direction){
            Direction.UP -> Position(playerPosition.x, tilePosition.y - 0.5f + playerRad + 0.01f)
            Direction.DOWN -> Position(playerPosition.x, tilePosition.y + 0.5f - playerRad - 0.01f)
            Direction.LEFT -> Position(tilePosition.x - 0.5f + playerRad + 0.01f, playerPosition.y)
            Direction.RIGHT -> Position(tilePosition.x + 0.5f - playerRad - 0.01f, playerPosition.y)
            Direction.NONE -> playerPosition
        }
    }

    private fun getOccupiedTile(value: Float, playerRadius: Float): Int {
        return when {
            value % 1 < playerRadius -> (value - playerRadius).toInt()
            value % 1 > 1 - playerRadius -> (value + playerRadius).toInt()
            else -> value.toInt()
        }
    }

    fun getPlayers(): List<Player> = players.values.toList()

    private fun spawnBomb(position: Position, range: Int): Bomb {
        val gridTile = grid[position.x.toInt(), position.y.toInt()]
        val bomb = Bomb(gridTile.position, range)
        bombs.add(bomb)
        return bomb
    }

    private fun explodeBomb(bomb: Bomb): Explosion {
        val originX = bomb.position.x.toInt()
        val originY = bomb.position.y.toInt()
        val affected = mutableListOf<Position>()

        // Always include bomb's own tile
        affected.add(Position(originX.toFloat() + 0.5f, originY.toFloat() + 0.5f))

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

                // Bounds check
                if (x !in 0 until grid.width || y !in 0 until grid.height) break

                val tile = grid[x, y]

                val pos = Position(x.toFloat() + 0.5f, y.toFloat() + 0.5f)
                affected.add(pos)

                if (tile.type == TileType.UnbreakableWall) {
                    affected.remove(pos)
                    break
                }

                if (tile.type == TileType.BreakableWall) {
                    tile.type = TileType.Empty
                    spawnPowerUp(tile.position)
                    break
                }
            }
        }

        for ((_,player) in players){
            if (!player.bombs.contains(bomb)) continue
            player.bombs.remove(bomb)
            player.bombCount += 1
        }
        return Explosion(
            affectedPositions = affected
        )
    }

    private fun spawnPowerUp(position: Position){
        val powerUpTypes = PowerUpType.entries.toTypedArray()
        if ((1..3).random() == 1) { //33% chance to spawn a power up
            powerUps.add(PowerUp(
                powerUpTypes[(0..2).random()], //random power up type
                position
            ))
        }
    }

    fun getPowerUps(): List<PowerUp> = powerUps.toList()

    fun getExplosions(): List<Explosion> = explosions.toList()

    fun getBombs(): List<Bomb> = bombs.toList()

    fun generateGameGrid(
        width: Int,
        height: Int,
        breakableWallCount: Int,
        unbreakableWallCount: Int
    ): GameGrid {
        val grid = GameGrid(width, height)

        // Fill border
        for (x in 0 until width) {
            grid[x, 0].type = TileType.UnbreakableWall
            grid[x, height - 1].type = TileType.UnbreakableWall
        }
        for (y in 0 until height) {
            grid[0, y].type = TileType.UnbreakableWall
            grid[width - 1, y].type = TileType.UnbreakableWall
        }

        // Reserve spawn zones (same logic as before)
        val reserved = mutableSetOf<Position>()
        reserved += listOf(
            Position(1.5f, 1.5f), Position(1.5f, 2.5f), Position(1.5f, 3.5f), Position(2.5f, 1.5f), Position(3.5f, 1.5f),                    // Top-left
            Position(width - 1.5f, 1.5f), Position(width - 2.5f, 1.5f), Position(width - 3.5f, 1.5f), Position(width - 1.5f, 2.5f), Position(width - 2.5f, 2.5f), // Top-right
            Position(1.5f, height - 1.5f), Position(1.5f, height - 2.5f), Position(1.5f, height - 3.5f), Position(2.5f, height - 1.5f), Position(3.5f, height - 1.5f), // Bottom-left
            Position(width - 1.5f, height - 1.5f), Position(width - 2.5f, height - 1.5f), Position(width - 3.5f, height - 1.5f), Position(width - 1.5f, height - 2.5f), Position(width - 1.5f, height - 3.5f) // Bottom-right
        )

        val candidateTiles = grid.allTiles()
            .filter { it.position !in reserved && it.type == TileType.Empty }
            .shuffled()

        candidateTiles.take(unbreakableWallCount).forEach {
            it.type = TileType.UnbreakableWall
        }

        candidateTiles.drop(unbreakableWallCount).take(breakableWallCount).forEach {
            it.type = TileType.BreakableWall
        }

        return grid
    }

    private fun calculateDangerZones(curPlayer: Player): List<List<Char>> {
        val width = grid.width
        val height = grid.height

        // Start with grid tile types
        val charGrid = Array(height) { y ->
            Array(width) { x ->
                when (grid[x, y].type) {
                    TileType.UnbreakableWall -> '#'
                    TileType.BreakableWall -> '+'
                    TileType.Empty -> '.'
                }
            }
        }

        // mark power ups
        for (powerUp in powerUps) {
            val x = powerUp.position.x.toInt()
            val y = powerUp.position.y.toInt()
            if (x !in 0 until width || y !in 0 until height) continue
            if (charGrid[y][x] == '.') {
                charGrid[y][x] = 'p'
            }
        }

        //Mark enemies
        for ((_, player) in players) {
            if (player == curPlayer) continue
            val x = player.position.x.toInt()
            val y = player.position.y.toInt()
            if (x !in 0 until width || y !in 0 until height) continue
            charGrid[y][x] = 'e'
        }

        // Mark bombs
        // Predict explosions for all bombs
        for (bomb in bombs) {
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

                    val tile = grid[x, y]
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
        for (explosion in explosions) {
            for (pos in explosion.affectedPositions) {
                val x = pos.x.toInt()
                val y = pos.y.toInt()
                if (x !in 0 until width || y !in 0 until height) continue
                charGrid[y][x] = '*'
            }
        }

        // Convert to List<List<Char>>
        return charGrid.map { it.toList() }
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

    private fun shouldTrapEnemy(player: Player, dangerZones: List<List<Char>>): Boolean {
        for ((id, otherPlayer) in players) {
            if (id == player.id || player.bombCount <= 0) continue
            if (!canEscape(dangerZones, player.position)) continue
            val fakeBomb = Bomb(player.position, player.fireRange)
            if (!canEscapeAfterBombPlaced(otherPlayer, fakeBomb) && canEscapeAfterBombPlaced(player, fakeBomb)) {
                return true
            }
        }
        return false
    }

    private fun shouldDestroyNearbyWall(player: Player, dangerZones: List<List<Char>>): Boolean {
        if (player.bombCount <= 0) return false
        val directions = listOf(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)

        return directions.any { direction ->
            val target = shift(player.position, direction)
            val isBreakableWall = mapEquals(dangerZones, target, listOf('+'))
            val canEscape = canEscapeAfterBombPlaced(player, Bomb(player.position, player.fireRange))
            isBreakableWall && canEscape
        }
    }

    // Strategy core; given the current game state, return a move to make.
    private fun calculateNextMove(player: Player): Moves {

        if (!isPlayerCentered(player)) return Moves.NO_CHANGE

        val dangerZones = calculateDangerZones(player)

        // If in danger zone, try to find a path to nearest safe location.
        if (isInDanger(player.position, dangerZones)) {
            val escapeMove = tryEscape(player, dangerZones)
            if (escapeMove != null) return escapeMove
        }

        // If placing a bomb will trap enemy, place bomb
        if (shouldTrapEnemy(player, dangerZones)) {
            println("Enemy is probably trapped by blast, placing bomb...")
            return Moves.PLACE_BOMB
        }

        // If next to a destructible wall, place bomb
        if (shouldDestroyNearbyWall(player, dangerZones)) {
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

    private fun mapInRange(map: List<List<Char>>, location: Position): Boolean {
        val (x, y) = location
        return x >= 0 && x < map[0].size && y >= 0 && y < map.size
    }

    private fun mapEquals(map: List<List<Char>>, location: Position, chars: List<Char>): Boolean {
        if (!mapInRange(map, location)) return false
        return chars.contains(map[location.y.toInt()][location.x.toInt()])
    }

    private fun mapEquals(map: List<List<Char>>, location: Position, char: Char): Boolean {
        if (!mapInRange(map, location)) return false
        return char == map[location.y.toInt()][location.x.toInt()]
    }

    private fun shift(location: Position, direction: Direction, offset: Int = 1): Position = when (direction) {
        Direction.UP -> Position(location.x, location.y - offset)
        Direction.DOWN -> Position(location.x, location.y + offset)
        Direction.LEFT -> Position(location.x - offset, location.y)
        Direction.RIGHT -> Position(location.x + offset, location.y)
        else -> throw IllegalArgumentException("Invalid direction")
    }

    private fun shortestPathBacktrack(
        map: List<List<Char>>,
        distances: List<List<Int>>,
        endLocation: Position,
        costs: Map<Char, Int>
    ): List<Moves> {
        var current = endLocation
        val path = mutableListOf<Moves>()
        val directions = listOf(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)

        while (distances[current.y.toInt()][current.x.toInt()] > 0) {
            for (direction in directions) {
                val next = shift(current, direction)
                if (!mapInRange(map, next)) continue

                val tileCost = costs[map[current.y.toInt()][current.x.toInt()]] ?: -1
                val expected = distances[next.y.toInt()][next.x.toInt()] + tileCost

                if (tileCost <= 0 || expected != distances[current.y.toInt()][current.x.toInt()]) continue

                current = next
                path += when (direction) {
                    Direction.UP -> Moves.MOVE_DOWN
                    Direction.DOWN -> Moves.MOVE_UP
                    Direction.LEFT -> Moves.MOVE_RIGHT
                    Direction.RIGHT -> Moves.MOVE_LEFT
                    else -> throw IllegalArgumentException("Invalid direction")
                }
                break
            }
        }

        path.reverse()
        println("Shortest path move sequence: $path")
        return path
    }

    private fun shortestPath(
        map: List<List<Char>>,
        start: Position,
        endChar: Char,
        costs: Map<Char, Int>
    ): List<Moves>? {
        val distances = List(map.size) { MutableList(map[0].size) { Int.MAX_VALUE } }
        distances[start.y.toInt()][start.x.toInt()] = 0

        val pq = PriorityQueue(compareBy<Pair<Int, Position>> { it.first })
        pq.add(0 to start)

        val directions = listOf(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)

        while (pq.isNotEmpty()) {
            val (length, location) = pq.poll()!!

            if (mapEquals(map, location, endChar)) {
                println("Found shortest path to $location, path length $length, position: (${start.y.toInt()}, ${start.x.toInt()}), end char: $endChar, costs $costs, map: $map")
                return shortestPathBacktrack(map, distances, location, costs)
            }

            for (direction in directions) {
                val next = shift(location, direction)
                if (!mapInRange(map, next)) continue

                val tileCost = costs[map[next.y.toInt()][next.x.toInt()]] ?: -1
                if (tileCost < 0) continue

                val newDistance = length + tileCost
                if (newDistance >= distances[next.y.toInt()][next.x.toInt()]) continue

                distances[next.y.toInt()][next.x.toInt()] = newDistance
                pq.add(newDistance to next)
            }
        }

        println("position: (${start.y.toInt()}, ${start.x.toInt()}) map: $map")
        return null
    }

    private fun canEscape(dangerZones: List<List<Char>>, escapeFromLocation: Position): Boolean {
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

    private fun canEscapeAfterBombPlaced(player: Player, bomb: Bomb): Boolean {
        bombs.add(bomb)
        val placedBombMap = calculateDangerZones(player)
        val ans = canEscape(placedBombMap, player.position)
        bombs.removeAt(bombs.size - 1)
        return ans
    }
}
