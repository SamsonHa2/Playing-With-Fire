package com.example.playingwithfire.ui.start

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.playingwithfire.util.UiEvent
import kotlinx.coroutines.flow.collectLatest

@Composable
fun StartScreen(
    onNavigate: (UiEvent.Navigate) -> Unit,
    viewModel: StartViewModel = hiltViewModel()
    ) {

    LaunchedEffect(true) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is UiEvent.Navigate -> onNavigate(event)
                else -> Unit
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Playing With Fire",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = { viewModel.onEvent(StartEvent.OnStartClick) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Game")
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { viewModel.onEvent(StartEvent.OnOptionsClick) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Options")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StartScreenPreview() {
    StartScreen(
        onNavigate = {},
    )
}