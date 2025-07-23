package com.example.playingwithfire.ui.game

import android.annotation.SuppressLint
import android.content.res.Resources
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.playingwithfire.model.Player
import com.example.playingwithfire.model.Position
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun GamePlayerHud(modifier: Modifier = Modifier, players: List<Player>, roundNumber: Int) {
    BoxWithConstraints(modifier = modifier) {
        val fontScale = Resources.getSystem().configuration.fontScale
        val fontSize = ((maxHeight*0.1f).value / fontScale).sp
        val maxHeight = maxHeight
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.11f)
                        .clip(RoundedCornerShape(8.dp))
                        .weight(2.5f)
                        .background(Color.LightGray)
                )

                Column(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .fillMaxSize()
                        .weight(8f),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    WinCounterBar(modifier = Modifier.fillMaxHeight(0.055f), player = players[0])
                    HPBar(modifier = Modifier.fillMaxHeight(0.055f), listOf(Color.Blue, Color.Cyan), players[0])
                    Spacer(modifier = Modifier.fillMaxHeight(0.02f))
                    PowerUpBar(
                        modifier = Modifier.fillMaxHeight(0.0275f),
                    )
                }
                BoxWithConstraints(
                    modifier = Modifier.weight(1.5f).height(maxHeight*0.16f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimerDisplay(fontSize, maxHeight, roundNumber)
                }
                Column(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .fillMaxSize()
                        .weight(8f)
                        .graphicsLayer {
                            scaleX = -1f // Flip vertically
                        },
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    WinCounterBar(modifier = Modifier.fillMaxHeight(0.055f), player = players[1])
                    HPBar(modifier = Modifier.fillMaxHeight(0.055f), listOf(Color.Red, Color.Magenta), players[1])
                    Spacer(modifier = Modifier.fillMaxHeight(0.02f))
                    PowerUpBar(
                        modifier = Modifier.fillMaxHeight(0.0275f),
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.11f)
                        .clip(RoundedCornerShape(8.dp))
                        .weight(2.5f)
                        .background(Color.LightGray)
                )
            }
        }

    }
}

@Composable
fun WinCounterBar(modifier: Modifier, player: Player) {
    BoxWithConstraints(modifier = modifier) {
        val maxHeight = maxHeight
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = maxHeight*1.45f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // Define the colors for each win state
            val colors = when (player.wins) {
                0 -> listOf(Color.Black, Color.Black) // 0 wins, both black
                1 -> listOf(Color.Black, Color.Green) // 1 win, black then green
                2 -> listOf(Color.Green, Color.Green) // 2 wins, both green
                else -> listOf(Color.Black, Color.Black) // Default fallback
            }

            // Draw the shapes based on the `player.wins` value
            repeat(2) { index ->
                TaperedGradientBar(
                    modifier = Modifier.padding(end = maxHeight * 0.35f),
                    gradientColors = listOf(colors[index], colors[index]),
                    width = maxHeight * 0.8f,
                    height = maxHeight * 0.5f,
                    taperAmount = maxHeight * 0.35f,
                )
            }
        }
    }
}

@Composable
fun HPBar(modifier: Modifier, gradientColor: List<Color>, player: Player) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val maxHeight = maxHeight
        val endPadding = maxHeight * 0.7f

        var chipHp by remember { mutableFloatStateOf(player.hp.toFloat()) }

        // Animate chip bar after delay
        LaunchedEffect(player.hp) {
            delay(1000) // delay before chip starts
            animate(
                initialValue = chipHp,
                targetValue = player.hp.toFloat(),
                animationSpec = tween(durationMillis = 500)
            ) { value, _ ->
                chipHp = value
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = endPadding),
            contentAlignment = Alignment.CenterEnd
        ) {
            TaperedGradientBar(
                modifier = Modifier.padding(end = endPadding),
                gradientColors = listOf(Color.Black, Color.Black),
                width = maxWidth,
                height = maxHeight,
                taperAmount = endPadding,
            )
            TaperedGradientBar(
                modifier = Modifier.padding(end = endPadding),
                gradientColors = listOf(Color.Yellow, Color.Yellow),
                width = maxWidth * (chipHp / 100f),
                height = maxHeight,
                taperAmount = endPadding,
            )
            TaperedGradientBar(
                modifier = Modifier.padding(end = endPadding),
                gradientColors = gradientColor,
                width = maxWidth * (player.hp / 100f),
                height = maxHeight,
                taperAmount = endPadding,
            )
        }
    }
}

@Composable
fun PowerUpBar(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        val maxHeight = maxHeight
        val maxWidth = maxWidth
        val endPadding = maxHeight * 0.7f
        Row(){
            TaperedGradientBar(
                modifier = Modifier.padding(end = endPadding),
                gradientColors = listOf(Color.Green, Color.Yellow),
                width = maxWidth * 0.5f,
                height = maxHeight,
                taperAmount = endPadding,
            )
            Spacer(Modifier.padding(maxHeight*0.2f))
        }

    }
}

@Composable
fun TaperedGradientBar(
    modifier: Modifier = Modifier,
    gradientColors: List<Color>,
    width: Dp,
    height: Dp,
    taperAmount: Dp,
    outlineColor: Color = Color.Black,
    outlineWidth: Dp = 2.dp
) {
    val density = LocalDensity.current

    Canvas(
        modifier = modifier.size(width, height)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val taperPx = with(density) { taperAmount.toPx() }
        val outlineStrokeWidth = with(density) { outlineWidth.toPx() }

        // Define parallelogram points
        val topEdgeStart = Offset(0f, 0f)
        val topEdgeEnd = Offset(canvasWidth, 0f)
        val bottomEdgeStart = Offset(taperPx, canvasHeight)
        val bottomEdgeEnd = Offset(canvasWidth + taperPx, canvasHeight)

        // Create the path for the tapered shape
        val path = Path().apply {
            moveTo(topEdgeStart.x, topEdgeStart.y)
            lineTo(topEdgeEnd.x, topEdgeEnd.y)
            lineTo(bottomEdgeEnd.x, bottomEdgeEnd.y)
            lineTo(bottomEdgeStart.x, bottomEdgeStart.y)
            close()
        }

        // Draw the gradient fill
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = gradientColors,
                start = Offset(0f, 0f),
                end = Offset(canvasWidth, canvasHeight)
            )
        )

        // Draw the outline
        drawPath(
            path = path,
            brush = SolidColor(outlineColor),
            style = Stroke(width = outlineStrokeWidth)
        )
    }
}

@Composable
fun TimerDisplay(
    fontSize: TextUnit,
    maxHeight: Dp,
    roundNumber: Int,
    totalTimeSeconds: Int = 99,
) {
    var secondsLeft by remember(roundNumber) { mutableIntStateOf(totalTimeSeconds) }
    val isRunning = remember(roundNumber) { mutableStateOf(true) }

    LaunchedEffect(key1 = isRunning.value, key2 = roundNumber) {
        while (isRunning.value && secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        }
    }
    Text(
        modifier = Modifier.padding(top = maxHeight * 0.04f).fillMaxSize(),
        text = "$secondsLeft",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold
    )
}

@Preview(showBackground = true, widthDp = 732, heightDp = 412)
@Composable
fun GamePlayerHudPreview() {
    GamePlayerHud(Modifier.fillMaxSize(), players = listOf(Player("1", "s", Position(0f,0f), wins = 1, hp = 55), Player("2", "as", Position(0f,0f), wins = 2)), 1)
}