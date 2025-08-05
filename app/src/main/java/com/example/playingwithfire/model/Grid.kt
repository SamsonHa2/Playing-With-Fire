package com.example.playingwithfire.model

class Grid(val width: Int, val height: Int) {
    val tiles: Array<Array<Tile>> = Array(height) { y ->
        Array(width) { x ->
            Tile(position = Position(x+0.5f, y+0.5f))
        }
    }

    operator fun get(x: Int, y: Int): Tile = tiles[y][x]
}