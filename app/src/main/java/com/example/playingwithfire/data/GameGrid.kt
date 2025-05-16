package com.example.playingwithfire.data

class GameGrid(val width: Int, val height: Int) {
    val tiles: Array<Array<Tile>> = Array(height) { y ->
        Array(width) { x ->
            Tile(position = Position(x+0.5f, y+0.5f))
        }
    }

    operator fun get(x: Int, y: Int): Tile = tiles[y][x]

    operator fun set(x: Int, y: Int, type: TileType) {
        tiles[y][x].type = type
    }

    fun allTiles(): List<Tile> = tiles.flatten()
}