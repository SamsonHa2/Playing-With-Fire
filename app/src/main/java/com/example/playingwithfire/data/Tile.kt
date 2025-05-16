package com.example.playingwithfire.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Tile(
    val position: Position,
    type: TileType = TileType.Empty
) {
    var type by mutableStateOf(type)
}