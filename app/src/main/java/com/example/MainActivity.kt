package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.LoginScreen
import com.example.ui.LoginViewModel
import com.example.ui.MonitoringScreen
import com.example.ui.MonitoringViewModel
import com.example.ui.theme.MyApplicationTheme

/** MainActivity — host de la app. El edge-to-edge se maneja por pantalla
 *  (MonitorScreen usa Scaffold que consume insets; Login aplica systemBars
 *  padding propio), así que acá NO se duplican insets (bug corregido). */
class MainActivity : ComponentActivity() {

  // ViewModels are retained across configuration changes (rotation tolerance)
  private val loginViewModel: LoginViewModel by viewModels()
  private val monitoringViewModel: MonitoringViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      // El tema sigue al sistema: modo claro y oscuro industriales (A1 UI)
      MyApplicationTheme {
        // Runtime permission request for Notifications (Android 13+)
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
          // If granted or denied, handle gracefully without crashing
        }

        LaunchedEffect(Unit) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
              this@MainActivity,
              Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
              notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
          }
        }

        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          AppNavigation(
            loginViewModel = loginViewModel,
            monitoringViewModel = monitoringViewModel
          )
        }
      }
    }
  }
}

@Composable
fun AppNavigation(
  loginViewModel: LoginViewModel,
  monitoringViewModel: MonitoringViewModel
) {
  val loginState by loginViewModel.uiState.collectAsState()

  AnimatedContent(
    targetState = loginState.isAuthenticated,
    transitionSpec = { fadeIn() togetherWith fadeOut() },
    label = "screen_transition"
  ) { isAuthenticated ->
    if (isAuthenticated) {
      BackHandler {
        // Prevent accidental logout on back press
      }
      MonitoringScreen(
        viewModel = monitoringViewModel,
        activeUser = loginState.activeUser,
        onLogout = {
          loginViewModel.logout()
          monitoringViewModel.disconnect()
        }
      )
    } else {
      LoginScreen(
        viewModel = loginViewModel,
        onLoginSuccess = {
          // Handled via state
        }
      )
    }
  }
}
