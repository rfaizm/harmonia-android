package com.rfaizm.harmoniamusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rfaizm.harmoniamusic.data.Settings
import com.rfaizm.harmoniamusic.ui.HarmoniaApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Settings.init(this) // before the first composition reads dark mode
        setContent { HarmoniaApp() }
    }
}
