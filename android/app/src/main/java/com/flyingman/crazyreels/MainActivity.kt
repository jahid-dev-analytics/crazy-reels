package com.flyingman.crazyreels

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.flyingman.crazyreels.ui.home.HomeScreen
import com.flyingman.crazyreels.ui.theme.CrazyReelsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrazyReelsTheme {
                HomeScreen()
            }
        }
    }
}
