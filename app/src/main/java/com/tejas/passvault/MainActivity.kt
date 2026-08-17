package com.tejas.passvault

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tejas.passvault.navigation.PassVaultNavGraph
import com.tejas.passvault.ui.theme.PassVaultTheme
import com.tejas.passvault.ui.theme.ThemeMode

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Block screenshots and the recent-apps thumbnail from ever showing saved passwords.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        setContent {
            val vm: VaultViewModel = viewModel()
            val themeMode by vm.themeMode.collectAsState()
            val useDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            PassVaultTheme(darkTheme = useDarkTheme) {
                PassVaultNavGraph(vm)
            }
        }
    }
}
