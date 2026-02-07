package com.umbrella.presentation.main

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.umbrella.data.prefs.PreferencesRepository
import com.umbrella.presentation.navigation.Routes
import com.umbrella.presentation.navigation.UmbrellaNavGraph
import com.umbrella.presentation.theme.UmbrellaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.refresh()
        } else {
            Toast.makeText(this, "알림 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.refresh()
        } else {
            Toast.makeText(this, "위치 권한이 필요하거나 수동으로 지역을 설정하세요", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 온보딩 완료 여부 확인
        val onboardingCompleted = runBlocking {
            preferencesRepository.onboardingCompleted.first()
        }

        val startDestination = if (onboardingCompleted) Routes.MAIN else Routes.ONBOARDING

        setContent {
            UmbrellaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    val toastMessage by viewModel.toastMessage.collectAsState()
                    val navController = rememberNavController()

                    LaunchedEffect(toastMessage) {
                        toastMessage?.let {
                            Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                            viewModel.clearToast()
                        }
                    }

                    UmbrellaNavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        mainUiState = uiState,
                        onRefresh = { viewModel.refresh() },
                        onRequestNotificationPermission = { requestNotificationPermission() },
                        onRequestLocationPermission = { requestLocationPermission() },
                        onOpenExactAlarmSettings = { openExactAlarmSettings() },
                        onCompleteOnboarding = { completeOnboarding() }
                    )
                }
            }
        }

        // 앱 시작 시 자동 새로고침 (온보딩 완료된 경우만)
        if (onboardingCompleted) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.refresh()
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "설정을 열 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun completeOnboarding() {
        lifecycleScope.launch {
            preferencesRepository.completeOnboarding()
            viewModel.refresh()
        }
    }
}
