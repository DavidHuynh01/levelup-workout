package com.davidhuynh.levelup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.davidhuynh.levelup.ui.navigation.AuthGate
import com.davidhuynh.levelup.ui.theme.LevelUpTheme

/**
 * The only Activity. Everything else is a composable destination, so there is no
 * hand-passing of the signed-in user through Intent extras the way the Foodie app had to.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as LevelUpApplication).container

        setContent {
            LevelUpTheme {
                AuthGate(container = container)
            }
        }
    }
}
