package com.example.playingwithfire.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.playingwithfire.R
import com.example.playingwithfire.model.Bomb
import com.example.playingwithfire.model.Direction
import com.example.playingwithfire.model.Explosion
import com.example.playingwithfire.model.ExplosionType
import com.example.playingwithfire.model.Player
import com.example.playingwithfire.model.PlayerState
import com.example.playingwithfire.model.PowerUp
import com.example.playingwithfire.model.PowerUpType
import com.example.playingwithfire.model.Tile
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
    val gameState by viewModel.gameState.collectAsState()

    val grid = gameState.grid
    val players = gameState.players
    val bombs = gameState.bombs
    val explosions = gameState.explosions
    val powerUps = gameState.powerUps

    val context = LocalContext.current
    val bombBitmap = remember { ImageBitmap.imageResource(context.resources, R.drawable.bomb) }
    val playerBitmap = remember { ImageBitmap.imageResource(context.resources, R.drawable.player) }
    val powerUpBitmap = remember { ImageBitmap.imageResource(context.resources, R.drawable.powerup) }
    val tileBitmap = remember { ImageBitmap.imageResource(context.resources, R.drawable.tile) }
    val explosionBitmap = remember { ImageBitmap.imageResource(context.resources, R.drawable.explosion) }
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
                            Tile(tileBitmap, grid[x, y], tileSize)
                        }
                    }
                }
            }
            for (bomb in bombs) {
                key(bomb.id) {
                    BombSprite(
                        bomb = bomb,
                        bitmap = bombBitmap,
                        tileSize = tileSize
                    )
                }
            }

            for (player in players){
                PlayerSprite(
                    player,
                    playerBitmap,
                    tileSize
                )
            }

            for (powerUp in powerUps){
                PowerUpSprite(
                    powerUp,
                    powerUpBitmap,
                    tileSize
                )
            }

            for (explosion in explosions){
                ExplosionSprite(
                    explosion,
                    explosionBitmap,
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
fun PowerUpSprite(powerUp: PowerUp, bitmap: ImageBitmap, tileSize: Dp) {
    val frameWidth = bitmap.width / 3
    val frameHeight = bitmap.height
    val density = LocalDensity.current.density

    // Calculate power up size and ensure it's even
    val sizePx = tileSize.value * density
    val adjustedSizePx = (sizePx / 2).roundToInt() * 2
    val powerUpSize = (adjustedSizePx / density).dp

    Box(
        modifier = Modifier
            .size(tileSize)
            .offset(
                x = tileSize * powerUp.position.x - powerUpSize / 2,
                y = tileSize * powerUp.position.y - powerUpSize / 2
            )
            .background(Color.LightGray),

    ){
        Canvas(modifier = Modifier.size(tileSize)) {
            val col = when (powerUp.type){
                PowerUpType.ExtraBomb -> 0
                PowerUpType.Speed -> 1
                PowerUpType.FireRange -> 2
            }

            drawImage(
                image = bitmap,
                srcOffset = IntOffset(col * frameWidth, 0),
                srcSize = IntSize(frameWidth, frameHeight),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.None
            )
        }
    }
}

@Composable
fun Tile(bitmap: ImageBitmap, tile: Tile, tileSize: Dp) {
    val frameWidth = bitmap.width / 3
    val frameHeight = bitmap.height
    val col = when (tile.type){
        TileType.Empty -> 0
        TileType.BreakableWall -> 1
        TileType.UnbreakableWall -> 2
    }
    Box(
        modifier = Modifier
            .size(tileSize)
            .background(Color.LightGray)
    ){
        Canvas(modifier = Modifier.size(tileSize)) {
            drawImage(
                image = bitmap,
                srcOffset = IntOffset(0, 0),
                srcSize = IntSize(frameWidth, frameHeight),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.None
            )
            drawImage(
                image = bitmap,
                srcOffset = IntOffset(col * frameWidth, 0),
                srcSize = IntSize(frameWidth, frameHeight),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.None
            )
        }
    }
}

@Composable
fun ExplosionSprite(explosion: Explosion, bitmap: ImageBitmap, tileSize: Dp){
    val frameWidth = bitmap.width / 4
    val frameHeight = bitmap.height / 2
    val (row, col) = when (explosion.type) {
        ExplosionType.Center -> 1 to 0
        ExplosionType.Middle -> when (explosion.direction) {
            Direction.UP, Direction.DOWN -> 1 to 2
            Direction.LEFT, Direction.RIGHT -> 1 to 1
        }
        ExplosionType.Outer -> when (explosion.direction) {
            Direction.UP -> 0 to 2
            Direction.DOWN -> 0 to 3
            Direction.LEFT -> 0 to 0
            Direction.RIGHT -> 0 to 1
        }
    }

    Box(
        modifier = Modifier
            .size(tileSize)
            .offset(
                x = tileSize * explosion.position.x - tileSize / 2,
                y = tileSize * explosion.position.y - tileSize / 2
            )
    ){
        Canvas(modifier = Modifier.size(tileSize)) {
            drawImage(
                image = bitmap,
                srcOffset = IntOffset(col * frameWidth, frameHeight * row),
                srcSize = IntSize(frameWidth, frameHeight),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.None
            )
        }
    }
}

@Composable
fun PlayerSprite(player: Player, bitmap: ImageBitmap, tileSize: Dp){
    val frameWidth = bitmap.width / 3
    val frameHeight = bitmap.height / 4
    Box(
        modifier = Modifier
            .size(tileSize)
            .offset(
                x = tileSize * player.position.x - tileSize / 2,
                y = tileSize * player.position.y - tileSize / 2
            )
    ) {
        // Mutable state to manually track animation progress
        var animationProgress by remember { mutableFloatStateOf(0f) }

        // Start animation loop only when running
        if (player.state == PlayerState.RUNNING) {
            val infiniteTransition = rememberInfiniteTransition()
            animationProgress = infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(300, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            ).value
        } else {
            animationProgress = 0f
        }

        Canvas(modifier = Modifier.size(tileSize)) {
            val row = when (player.direction) {
                Direction.DOWN -> 0
                Direction.UP -> 1
                Direction.RIGHT -> 2
                Direction.LEFT -> 3
            }

            val col = when {
                player.state == PlayerState.IDLE -> 0 // Idle: always show first frame
                animationProgress < 0.5f -> 1
                else -> 2
            }

            drawImage(
                image = bitmap,
                srcOffset = IntOffset(col * frameWidth, row * frameHeight),
                srcSize = IntSize(frameWidth, frameHeight),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.None
            )
        }
    }
}

@Composable
fun BombSprite(bomb: Bomb, bitmap: ImageBitmap, tileSize: Dp){
    val animatable = remember(bomb.id) { Animatable(0f) }

    // Store initial duration so it doesn't change mid-animation
    val totalDurationMillis by remember(bomb.id) {
        derivedStateOf {
            ((bomb.totalTime + 1.25) * 1000).toInt()
        }
    }

    LaunchedEffect(bomb.id) {
        // Reset animation progress
        animatable.snapTo(0f)

        // Animate from 0f to 1f over remaining time in milliseconds
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = totalDurationMillis)
        )
    }

    Box(
        modifier = Modifier
            .size(tileSize)
            .offset(
                x = tileSize * bomb.position.x - tileSize / 2,
                y = tileSize * bomb.position.y - tileSize / 2
            )
        ,
    ) {
        Canvas(modifier = Modifier.size(tileSize)) {
            val frameCount = 20
            val canvasWidth = size.width.toInt()
            val canvasHeight = size.height.toInt()
            val frameWidth = bitmap.width / frameCount
            val frameHeight = bitmap.height

            val progress = animatable.value
            val col = ((progress.coerceIn(0f, 1f - 0.0001f)) * frameCount).toInt()

            drawImage(
                image = bitmap,
                srcOffset = IntOffset(col * frameWidth, 0),
                srcSize = IntSize(frameWidth, frameHeight),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(canvasWidth, canvasHeight),
                filterQuality = FilterQuality.None
            )
        }
    }
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
            onPress = { viewModel.onEvent(GameEvent.OnUpClick) },
            onRelease = { viewModel.onEvent(GameEvent.OnDirectionRelease) },
            size = size
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            ArrowButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                onPress = { viewModel.onEvent(GameEvent.OnLeftClick) },
                onRelease = { viewModel.onEvent(GameEvent.OnDirectionRelease) },
                size = size
            )
            Spacer(modifier = Modifier.size(size))
            ArrowButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                onPress = { viewModel.onEvent(GameEvent.OnRightClick) },
                onRelease = { viewModel.onEvent(GameEvent.OnDirectionRelease) },
                size = size
            )
        }

        ArrowButton(
            icon = Icons.Default.KeyboardArrowDown,
            onPress = { viewModel.onEvent(GameEvent.OnDownClick) },
            onRelease = { viewModel.onEvent(GameEvent.OnDirectionRelease) },
            size = size
        )
    }
}


@Composable
fun ArrowButton(
    icon: ImageVector,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    size: Dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Blue)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPress()
                        awaitRelease()
                        onRelease()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = icon.name,
            tint = Color.White,
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