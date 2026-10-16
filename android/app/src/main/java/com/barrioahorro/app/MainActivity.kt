package com.barrioahorro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.barrioahorro.app.ui.navigation.AppNavigation
import com.barrioahorro.app.ui.theme.AndroidTheme
import com.barrioahorro.app.ui.theme.Cream
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.SystemBarStyle

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.rgb(251, 246, 230),
                android.graphics.Color.rgb(251, 246, 230),
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.rgb(251, 246, 230),
                android.graphics.Color.rgb(251, 246, 230),
            ),
        )
        setContent {
            AndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Cream,
                ) {
                    AppNavigation()
                }
            }
        }
    }
}