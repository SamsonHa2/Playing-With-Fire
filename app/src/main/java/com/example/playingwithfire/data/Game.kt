package com.example.playingwithfire.data

import android.util.Log

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
    init {
        spawnPlayer("101231965", "Samson Ha", Position(1.5f,1.5f))
    }

    fun spawnPlayer(id: String, name: String, position: Position) {
        if (players.containsKey(id)) return
        if (grid[position.x.toInt(), position.y.toInt()].type != TileType.Empty) return

        players[id] = Player(
            id = id,
            position = position,
            name = name,
        )
    }

    fun updatePlayer(id: String, updated: Player) {
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
        for (player in players){
            movePlayer(delta, player.value)
            player.value.direction = Direction.NONE
        }
    }

    private fun updateBombs(delta: Double) {
        val iterator = bombs.iterator()
        while (iterator.hasNext()) {
            val bomb = iterator.next()
            bomb.remainingTime -= delta
            if (bomb.remainingTime <= 0) {
                val explosion = explodeBomb(bomb)
                addExplosion(explosion)
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

    private fun movePlayer(delta: Double, player: Player) {
        val collision = checkCollision(delta, player)
        if (!collision){
            player.move(delta)
        }
    }

    private fun checkCollision(delta: Double, player: Player): Boolean {
        val updated = player.copy().apply { move(delta) }
        val playerRadius = player.size / 2
        val direction = player.direction

        val targetX = when (direction) {
            Direction.LEFT  -> (updated.position.x - playerRadius).toInt()
            Direction.RIGHT -> (updated.position.x + playerRadius).toInt()
            else            -> updated.position.x.toInt()
        }

        val targetY = when (direction) {
            Direction.UP    -> (updated.position.y - playerRadius).toInt()
            Direction.DOWN  -> (updated.position.y + playerRadius).toInt()
            else            -> updated.position.y.toInt()
        }

        val diaTargetX = getOccupiedTile(updated.position.x, playerRadius)
        val diaTargetY = getOccupiedTile(updated.position.y, playerRadius)

        val tile = grid[targetX, targetY]
        val tile2 = grid[diaTargetX, diaTargetY]

        if (tile.type == TileType.Empty && tile2.type == TileType.Empty) {
            Log.d("GameEvent", "Player moved $direction from ${player.position.x}, ${player.position.y} to ${updated.position.x}, ${updated.position.y} = no collision")
            updatePlayer(player.id, updated)
            return false
        } else {
            Log.d("GameEvent", "Player blocked $direction")
        }
        return true
    }


    private fun getOccupiedTile(value: Float, playerRadius: Float): Int {
        return when {
            value % 1 < playerRadius -> (value - playerRadius).toInt()
            value % 1 > 1 - playerRadius -> (value + playerRadius).toInt()
            else -> value.toInt()
        }
    }

    fun getPlayers(): List<Player> = players.values.toList()

    fun spawnBomb(position: Position, range: Int): Bomb? {

        val gridTile = grid[position.x.toInt(), position.y.toInt()]
        val bomb = Bomb(gridTile.position, range)
        if (gridTile.type == TileType.Empty) {
            bombs.add(bomb)
            return bomb
        }
        return null
    }

    fun explodeBomb(bomb: Bomb): Explosion {
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
                    break
                }
            }
        }

        return Explosion(
            affectedPositions = affected
        )
    }

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

    fun addExplosion(explosion: Explosion) {
        explosions.add(explosion)
    }
}
