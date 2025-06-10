package com.example.playingwithfire.domain.pathfinding

import com.example.playingwithfire.model.Direction
import com.example.playingwithfire.model.Moves
import com.example.playingwithfire.model.Position
import java.util.PriorityQueue

object Pathfinding {

    fun shortestPath(
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
                //println("Found shortest path to $location, path length $length, position: (${start.y.toInt()}, ${start.x.toInt()}), end char: $endChar, costs $costs, map: $map")
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

        //println("position: (${start.y.toInt()}, ${start.x.toInt()}) map: $map")
        return null
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
        //println("Shortest path move sequence: $path")
        return path
    }
}