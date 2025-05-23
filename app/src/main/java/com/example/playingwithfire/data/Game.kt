package com.example.playingwithfire.data

import android.util.Log
import kotlin.math.min

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

            val finalPlayer = collectPowerUp(movedPlayer, mainTile.position, diaTile.position).copy(direction = Direction.NONE)
            updatePlayer(id, finalPlayer)
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
                    spawnPowerUp(tile.position)
                    break
                }
            }
        }

        getPlayers()[0].bombCount += 1
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

    fun addExplosion(explosion: Explosion) {
        explosions.add(explosion)
    }
}
