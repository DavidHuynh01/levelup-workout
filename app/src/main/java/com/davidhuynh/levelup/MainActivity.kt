package com.davidhuynh.levelup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.davidhuynh.levelup.ui.navigation.AuthGate
import com.davidhuynh.levelup.ui.theme.LevelUpTheme

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
