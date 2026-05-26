package com.pira.gnetp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.pira.gnetp.ui.home.HomeScreen
import com.pira.gnetp.ui.theme.GNetTheme

class MainActivity : ComponentActivity() {
    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        mainViewModel = MainViewModel(application)
        mainViewModel.init()

        setContent {
            GNetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val homeUiState by mainViewModel.homeUiState.collectAsState()
                    
                    HomeScreen(
                        uiState = homeUiState,
                        onStartBothProxies = { mainViewModel.startBothProxies() },
                        onStopBothProxies = { mainViewModel.stopBothProxies() },
                        onNavigateToSettings = {},
                        onNavigateToHotspot = {},
                        onNavigateToLogs = {},
                        onVpnPermissionRequest = {}
                    )
                }
            }
        }
    }

    // 🔥 دالة النافذة العائمة (PiP) في مكانها الصحيح خارج الـ onCreate
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build())
        }
    }
}
