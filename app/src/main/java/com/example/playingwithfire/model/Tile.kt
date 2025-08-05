package com.example.playingwithfire.model

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
class Tile(
    val position: Position,
    type: TileType = TileType.Empty
) {
    var type by mutableStateOf(type)
}