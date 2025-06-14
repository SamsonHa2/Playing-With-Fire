package com.example.playingwithfire.domain

import android.util.Log
import com.example.playingwithfire.domain.map.MapGenerator
import com.example.playingwithfire.model.*
import kotlin.math.min

class GameEngine {
    private val grid: Grid = MapGenerator.generateGrid(15, 13, 50, 30)

    private val players = mutableMapOf<String, Player>()
    private val bombs = mutableListOf<Bomb>()
    private val explosions = mutableListOf<Explosion>()
    private val powerUps = mutableListOf<PowerUp>()

    init {
        spawnPlayer("Player 1", "Player 1", Position(1.5f,1.5f))
        spawnPlayer("Bot 1", "Bot 1", Position(13.5f,11.5f))
    }
    fun getGameState(): GameState {
        return GameState(
            grid = grid,
            players = players.values.toList(),
            bombs = bombs.toList(),
            explosions = explosions.toList(),
            powerUps = powerUps.toList()
        )
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
                when (player.behavior.decideMove(player, getGameState())){
                    Moves.MOVE_UP -> setPlayerRunning(player, Direction.UP)
                    Moves.MOVE_DOWN -> setPlayerRunning(player, Direction.DOWN)
                    Moves.MOVE_LEFT -> setPlayerRunning(player, Direction.LEFT)
                    Moves.MOVE_RIGHT -> setPlayerRunning(player, Direction.RIGHT)
                    Moves.PLACE_BOMB -> placeBomb(player)
                    Moves.DO_NOTHING -> player.state = PlayerState.IDLE
                    Moves.NO_CHANGE -> { /* do nothing */ }
                }
            }
            if (player.state == PlayerState.IDLE) continue
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

            val finalPlayer = collectPowerUp(movedPlayer, mainTile.position, diaTile.position).copy()

            updatePlayer(id, finalPlayer)
        }
    }

    fun setPlayerRunning(player: Player, direction: Direction) {
        player.direction = direction
        player.state = PlayerState.RUNNING
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

}