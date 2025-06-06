package com.example.playingwithfire.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.playingwithfire.model.Bomb
import com.example.playingwithfire.model.Explosion
import com.example.playingwithfire.model.Player
import com.example.playingwithfire.model.PowerUp
import com.example.playingwithfire.model.PowerUpType
import com.example.playingwithfire.model.TileType
import com.example.playingwithfire.util.UiEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

@Composable
fun GameScreen(
    onNavigate: (UiEvent.Navigate) -> Unit,
    viewModel: GameViewModel = hiltViewModel()
) {
    val grid by viewModel.grid.collectAsState()
    val players by viewModel.players.collectAsState()
    val bombs by viewModel.bombs.collectAsState()
    val explosions by viewModel.explosions.collectAsState()
    val powerUps by viewModel.powerUps.collectAsState()

    LaunchedEffect(true) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is UiEvent.Navigate -> onNavigate(event)
                else -> Unit
            }
        }
    }

    LaunchedEffect(Unit) {
        val fixedDelta = 1.0 / 60.0  // 60 updates per second
        val fixedDeltaNanos = (fixedDelta * 1_000_000_000L).toLong()

        var previousTime = System.nanoTime()
        var lag = 0L

        while (true) {
            val currentTime = System.nanoTime()
            val elapsed = currentTime - previousTime
            previousTime = currentTime
            lag += elapsed

            while (lag >= fixedDeltaNanos) {
                viewModel.updateGame(fixedDelta)
                lag -= fixedDeltaNanos
            }

            // Optional: add delay to avoid tight looping
            delay(1L)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .weight(0.5f),
            contentAlignment = Alignment.TopStart
        ) {
            val size = maxWidth / 6  // Adjust scale as needed (smaller = bigger)
            ArrowButtonGrid(
                viewModel = viewModel,
                size = size
            )
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.5f)
                .padding(16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            val tileSize = calculateCellSize(
                maxWidth = maxWidth,
                maxHeight = maxHeight,
                gridWidth = grid.width,
                gridHeight = grid.height
            )
            Column {
                for (y in 0 until grid.height) {
                    Row {
                        for (x in 0 until grid.width) {
                            when (grid[x, y].type) {
                                TileType.Empty -> EmptyBox(size = tileSize)
                                TileType.UnbreakableWall -> UnbreakableBox(size = tileSize)
                                TileType.BreakableWall -> BreakableBox(size = tileSize)
                            }
                        }
                    }
                }
            }
            for (bomb in bombs) {
                BombSprite(
                    bomb,
                    tileSize
                )
            }

            for (player in players){
                PlayerSprite(
                    player,
                    tileSize
                )
            }

            for (powerUp in powerUps){
                PowerUpSprite(
                    powerUp,
                    tileSize
                )
            }

            for (explosion in explosions){
                ExplosionSprite(
                    explosion,
                    tileSize
                )
            }
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .weight(0.5f),
            contentAlignment = Alignment.TopStart
        ) {
            val size = maxWidth / 6  // Adjust scale as needed (smaller = bigger)
            BombButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                onClick = { viewModel.onEvent(GameEvent.OnBombClick) },
                size = size
            )
        }
    }
}

@Composable
fun PowerUpSprite(powerUp: PowerUp, size: Dp) {
    val color = when (powerUp.type) {
        PowerUpType.Speed -> Color.White
        PowerUpType.FireRange -> Color.Magenta
        PowerUpType.ExtraBomb -> Color.Cyan
    }
    val density = LocalDensity.current.density

    // Calculate power up size and ensure it's even
    val sizePx = size.value * density
    val adjustedSizePx = (sizePx / 2).roundToInt() * 2
    val powerUpSize = (adjustedSizePx / density).dp

    Box(
        modifier = Modifier
            .size(size)
            .offset(
                x = size * powerUp.position.x - powerUpSize / 2,
                y = size * powerUp.position.y - powerUpSize / 2
            )
            .background(color),

    )
}

@Composable
fun EmptyBox(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(Color.LightGray)
    )
}

@Composable
fun UnbreakableBox(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .border(1.dp, Color.Black)
            .background(Color.DarkGray)
    )
}

@Composable
fun BreakableBox(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .border(1.dp, Color.Black)
            .background(Color.Yellow)
    )
}
@Composable
fun ExplosionSprite(explosion: Explosion, tileSize: Dp){
    // Use LocalDensity to ensure the size is calculated correctly and stays even
    val density = LocalDensity.current.density

    // Calculate explosion size and ensure it's even
    val explosionSizePx = tileSize.value * density
    val adjustedExplosionSizePx = (explosionSizePx / 2).roundToInt() * 2
    val explosionSize = (adjustedExplosionSizePx / density).dp
    for (position in explosion.affectedPositions) {
        Box(
            modifier = Modifier
                .size(explosionSize)
                .offset(
                    x = tileSize * position.x - explosionSize / 2,
                    y = tileSize * position.y - explosionSize / 2
                )
                .background(Color.Blue),
        )
    }
}

@Composable
fun PlayerSprite(player: Player, tileSize: Dp){
    // Use LocalDensity to ensure the size is calculated correctly and stays even
    val density = LocalDensity.current.density

    // Calculate player size and ensure it's even
    val playerSizePx = (tileSize * player.size).value * density
    val adjustedPlayerSizePx = (playerSizePx / 2).roundToInt() * 2
    val playerSize = (adjustedPlayerSizePx / density).dp
    Box(
        modifier = Modifier
            .size(playerSize)
            .offset(
                x = tileSize * player.position.x - playerSize / 2,
                y = tileSize * player.position.y - playerSize / 2
            )
            .clip(CircleShape)
            .background(Color.Red)
            ,
    )
}

@Composable
fun BombSprite(bomb: Bomb, tileSize: Dp){
    // Use LocalDensity to ensure the size is calculated correctly and stays even
    val density = LocalDensity.current.density

    // Calculate player size and ensure it's even
    val bombSizePx = tileSize.value * density
    val adjustedBombSizePx = (bombSizePx / 2).roundToInt() * 2
    val bombSize = (adjustedBombSizePx / density).dp
    Box(
        modifier = Modifier
            .size(bombSize)
            .offset(
                x = tileSize * bomb.position.x - bombSize / 2,
                y = tileSize * bomb.position.y - bombSize / 2
            )
            .clip(CircleShape)
            .background(Color.Green)
        ,
    )
}

@Composable
fun BombButton(
    icon: ImageVector,
    onClick: () -> Unit, //
    size: Dp,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size * 3)
                .clip(CircleShape)
                .background(Color.Blue)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = icon.name,
                tint = Color.White
            )
        }
    }
}

@Composable
fun ArrowButtonGrid(
    viewModel: GameViewModel,
    size: Dp
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ArrowButton(
            icon = Icons.Default.KeyboardArrowUp,
            onRepeat = { viewModel.onEvent(GameEvent.OnUpClick) },
            size = size
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            ArrowButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                onRepeat = { viewModel.onEvent(GameEvent.OnLeftClick) },
                size = size
            )
            Spacer(modifier = Modifier.size(size))
            ArrowButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                onRepeat = { viewModel.onEvent(GameEvent.OnRightClick) },
                size = size
            )
        }

        ArrowButton(
            icon = Icons.Default.KeyboardArrowDown,
            onRepeat = { viewModel.onEvent(GameEvent.OnDownClick) },
            size = size
        )
    }
}


@Composable
fun ArrowButton(
    icon: ImageVector,
    onRepeat: () -> Unit,
    size: Dp,
    intervalMs: Long = 1L
) {
    var isPressed by remember { mutableStateOf(false) }

    // Launch a repeating coroutine when pressed
    LaunchedEffect(isPressed) {
        while (isPressed) {
            onRepeat()
            delay(intervalMs)
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Blue)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = icon.name,
            tint = Color.White
        )
    }
}


@Composable
fun calculateCellSize(
    maxWidth: Dp,
    maxHeight: Dp,
    gridWidth: Int,
    gridHeight: Int
): Dp {
    val maxWidthPerCell = maxWidth / gridWidth
    val maxHeightPerCell = maxHeight / gridHeight
    val rawSize = minOf(maxWidthPerCell, maxHeightPerCell)

    // Round to nearest even number in pixels, then convert to Dp
    val px = with(LocalDensity.current) { rawSize.toPx() }
    val evenPx = (px / 2).roundToInt() * 2
    return with(LocalDensity.current) { evenPx.toDp() }
}

@Preview(showBackground = true, device = Devices.AUTOMOTIVE_1024p, widthDp = 720, heightDp = 360)
@Composable
fun GameScreenPreview() {
    GameScreen(
        onNavigate = {},
    )
}