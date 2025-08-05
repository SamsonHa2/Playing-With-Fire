package com.example.playingwithfire.domain.map

import com.example.playingwithfire.model.*

object MapGenerator {
    fun generateGrid(
        width: Int,
        height: Int,
        breakableWallCount: Int,
        unbreakableWallCount: Int
    ): Grid {
        val grid = Grid(width, height)

        // Fill border
        for (x in 0 until width) {
            grid[x, 0].type = TileType.UnbreakableWall
            grid[x, height - 1].type = TileType.UnbreakableWall
        }
        for (y in 0 until height) {
            grid[0, y].type = TileType.UnbreakableWall
            grid[width - 1, y].type = TileType.UnbreakableWall
        }

        // Reserve spawn zones
        val reserved = mutableSetOf<Position>()
        reserved += listOf(
            Position(1.5f, 1.5f), Position(1.5f, 2.5f), Position(1.5f, 3.5f), Position(2.5f, 1.5f), Position(3.5f, 1.5f),                    // Top-left
            Position(width - 1.5f, 1.5f), Position(width - 2.5f, 1.5f), Position(width - 3.5f, 1.5f), Position(width - 1.5f, 2.5f), Position(width - 2.5f, 2.5f), // Top-right
            Position(1.5f, height - 1.5f), Position(1.5f, height - 2.5f), Position(1.5f, height - 3.5f), Position(2.5f, height - 1.5f), Position(3.5f, height - 1.5f), // Bottom-left
            Position(width - 1.5f, height - 1.5f), Position(width - 2.5f, height - 1.5f), Position(width - 3.5f, height - 1.5f), Position(width - 1.5f, height - 2.5f), Position(width - 1.5f, height - 3.5f) // Bottom-right
        )

        val candidateTiles = grid.tiles
            .flatten()
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
}