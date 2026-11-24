package com.example

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.TeleprompterAppShell
import com.example.ui.theme.SmartTeleprompterTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.RemoteKeyBus

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as TeleprompterApplication).container

        setContent {
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModel.factory(appContainer)
            )
            val settings by mainViewModel.settings.collectAsStateWithLifecycle()

            SmartTeleprompterTheme(themeMode = settings.themeMode) {
                TeleprompterAppShell(
                    container = appContainer,
                    mainViewModel = mainViewModel
                )
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (RemoteKeyBus.onKeyDown(event.keyCode, event)) {
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
