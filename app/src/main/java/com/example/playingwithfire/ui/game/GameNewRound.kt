package com.example.playingwithfire.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun GameNewRound(roundNumber: Int){
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.wrapContentSize(),
            verticalAlignment = Alignment.Bottom
        ) {
            val roundAlpha = remember { Animatable(0f) }

            LaunchedEffect(roundNumber) {
                delay(250)
                roundAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 250)
                )
                delay(1500)
                roundAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 250)
                )
            }

            Text(
                text = "ROUND",
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                modifier = Modifier
                    .alignBy(FirstBaseline)
                    .alpha(roundAlpha.value)
            )

            Spacer(modifier = Modifier.width(12.dp))

            val animatedOffsetX = remember { Animatable(-4000f) }

            LaunchedEffect(roundNumber) {
                animatedOffsetX.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 500,
                        easing = FastOutSlowInEasing
                    )
                )
                delay(1500)
                animatedOffsetX.animateTo(
                    targetValue = 4000f,
                    animationSpec = tween(durationMillis = 250)
                )
            }

            Text(
                text = "$roundNumber",
                fontSize = 128.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                modifier = Modifier
                    .alignBy(FirstBaseline)
                    .graphicsLayer { translationX = animatedOffsetX.value }
            )
        }

        val roundAlpha = remember { Animatable(0f) }

        val fontSizeAnim = remember { Animatable(256f) } // animate as Float (sp value)

        LaunchedEffect(roundNumber) {
            delay(2000)
            roundAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 250)
            )
            fontSizeAnim.animateTo(
                targetValue = 128f, // target size in sp
                animationSpec = tween(durationMillis = 50)
            )
            delay(1500)
            roundAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 250)
            )

        }

        Text(
            text = "FIGHT",
            fontSize = fontSizeAnim.value.sp, // convert Float to sp
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black,
            modifier = Modifier
                .alpha(roundAlpha.value)
        )
    }
}