package com.example.playingwithfire.domain

import android.util.Log
import com.example.playingwithfire.domain.map.MapGenerator
import com.example.playingwithfire.model.*
import kotlin.math.min

class GameEngine {
    private var grid: Grid = MapGenerator.generateGrid(17, 12, 55, 33)

    private val players = mutableMapOf<String, Player>()
    private var bombs = mutableListOf<Bomb>()
    private var explosions = mutableListOf<Explosion>()
    private var powerUps = mutableListOf<PowerUp>()
    private var round = 1
    private var winner = "None"

    init {
        spawnPlayer("Player 1", "Player 1", Position(1.5f,1.5f))
        spawnPlayer("Bot 1", "Bot 1", Position(15.5f,10.5f))
    }
    fun getGameState(): GameState {
        return GameState(
            grid = grid,
            players = players.values.toList(),
            bombs = bombs.toList(),
            explosions = explosions.toList(),
            powerUps = powerUps.toList(),
            round = round,
            winner = winner
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
        checkRoundWin()
    }

    private fun checkPlayerWin(){
        for (player in players.values){
            if (player.wins >= 2) {
                winner = player.id
            }
        }
    }

    private fun checkRoundWin(){
        for (player in players.values){
            if (player.hp <= 0) {
                val opponentId = if (player.id == "Player 1") "Bot 1" else "Player 1"
                players[opponentId]?.apply {
                    wins += 1
                }
                checkPlayerWin()

                if (winner == "None"){
                    resetRound()
                    round += 1
                }
            }
        }
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
            if (player.state == PlayerState.IDLE) {
                val playerCopy = player.copy()
                val tile = grid[playerCopy.position.x.toInt(), playerCopy.position.y.toInt()]

                val newPlayer = handleExplosionCollision(playerCopy, tile.position, tile.position)
                updatePlayer(id, newPlayer)
                continue
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

            val finalPlayer = collectPowerUp(movedPlayer, mainTile.position, diaTile.position).copy()

            updatePlayer(id, finalPlayer)
        }
    }

    private fun resetRound(){
        for (player in players.values){
            if (player.id == "Player 1"){
                player.position = Position(1.5f,1.5f)
            } else {
                player.position = Position(15.5f,10.5f)
            }

            player.bombs = ArrayList()
            player.hp = 100
            player.bombCount = 1
            player.fireRange = 1
            player.speed = 2f
            player.direction = Direction.LEFT
            player.state = PlayerState.IDLE
        }
        grid = MapGenerator.generateGrid(17, 12, 55, 33)
        bombs = mutableListOf()
        explosions = mutableListOf()
        powerUps = mutableListOf()
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
                explodeBomb(bomb)
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
        for (explosion in explosions){
            if (player.id in explosion.damagedPlayers) continue
            if (explosion.position == mainTilePos || explosion.position == diaTilePos) {
                player.hp -= 32
                explosion.damagedPlayers.add(player.id)
                Log.d("GameEvent", "Player stepped in explosion \t $explosion")
                break
            }
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

    private fun explodeBomb(bomb: Bomb){
        val originX = bomb.position.x.toInt()
        val originY = bomb.position.y.toInt()

        // Always include bomb's own tile
        explosions.add(Explosion(Position(originX.toFloat() + 0.5f, originY.toFloat() + 0.5f), Direction.UP, ExplosionType.Center))

        for (dir in Direction.entries) {
            val (dx, dy) = when (dir) {
                Direction.UP -> 0 to -1
                Direction.DOWN -> 0 to 1
                Direction.LEFT -> -1 to 0
                Direction.RIGHT -> 1 to 0
            }
            for (i in 1..bomb.range) {
                val x = originX + dx * i
                val y = originY + dy * i

                // Bounds check
                if (x !in 0 until grid.width || y !in 0 until grid.height) break

                val tile = grid[x, y]

                val pos = Position(x + 0.5f, y + 0.5f)

                if (tile.type == TileType.UnbreakableWall) break

                if (tile.type == TileType.BreakableWall) {
                    tile.type = TileType.Empty
                    spawnPowerUp(tile.position)
                    explosions.add(Explosion(pos, dir, ExplosionType.Outer))
                    break
                }

                val existing = explosions.find { it.position == pos }
                if (existing != null) {
                    if (existing.type == ExplosionType.Outer &&
                        ((existing.direction.isVertical() && dir.isVertical()) ||
                                (existing.direction.isHorizontal() && dir.isHorizontal()))
                    ) {
                        existing.type = ExplosionType.Middle
                        explosions.add(Explosion(pos, dir, ExplosionType.Middle))
                    }
                }
                val type = if (i == bomb.range) ExplosionType.Outer else ExplosionType.Middle
                explosions.add(Explosion(pos, dir, type))
            }
        }

        for ((_,player) in players){
            if (!player.bombs.contains(bomb)) continue
            player.bombs.remove(bomb)
            player.bombCount += 1
        }
    }

    private fun Direction.isVertical() = this == Direction.UP || this == Direction.DOWN
    private fun Direction.isHorizontal() = this == Direction.LEFT || this == Direction.RIGHT

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