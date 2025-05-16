package com.example.playingwithfire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.playingwithfire.ui.game.GameScreen
import com.example.playingwithfire.ui.start.StartScreen
import com.example.playingwithfire.ui.theme.PlayingWithFireTheme
import com.example.playingwithfire.util.Routes
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlayingWithFireTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Routes.START
                ) {
                    composable(Routes.START) {
                        StartScreen(
                            onNavigate = {
                                navController.navigate(it.route)
                            }
                        )
                    }
                    composable(Routes.GAME) {
                        GameScreen(
                            onNavigate = {
                                navController.navigate(it.route)
                            }
                        )
                    }
                }
            }
        }
    }
}