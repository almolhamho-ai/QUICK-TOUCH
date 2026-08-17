package com.example.quickgestures

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.services.ShakeDetectorService
import com.example.quickgestures.services.edge.EdgeGestureService
import com.example.quickgestures.services.floating.FloatingBallService
import com.example.quickgestures.ui.screens.*

class MainActivity : ComponentActivity() {

    private lateinit var prefs: AppPreferences

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* لا حاجة لأي إجراء إضافي، مجرد طلب الصلاحية */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPreferences(this)
        requestRuntimePermissionsIfNeeded()

        setContent {
            MaterialTheme {
                MainAppStructure(prefs = prefs, onRequestOverlayPermission = { requestOverlayPermission() })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startBackgroundServicesIfReady()
    }

    private fun requestRuntimePermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
    }

    /** الخدمات الأساسية بتشتغل دايماً بالخلفية؛ الكرة العائمة وإيماءات الحافة بيحتاجوا صلاحية الظهور فوق التطبيقات */
    private fun startBackgroundServicesIfReady() {
        startService(Intent(this, ShakeDetectorService::class.java))

        if (Settings.canDrawOverlays(this)) {
            if (prefs.quickBallEnabled && prefs.quickBallWorksOutsideApp) {
                startService(Intent(this, FloatingBallService::class.java))
            }
            if (prefs.edgeGestureEnabled) {
                startService(Intent(this, EdgeGestureService::class.java))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppStructure(prefs: AppPreferences, onRequestOverlayPermission: () -> Unit) {
    val navController = rememberNavController()
    var refreshKey by remember { mutableIntStateOf(0) }
    val onStateChanged = { refreshKey++ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("لمسة سريعة") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "settings",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("settings") {
                GestureSettingsScreen(onNavigate = { route -> navController.navigate(route) })
            }
            composable("quick_ball") {
                onRequestOverlayPermission()
                QuickBallConfigScreen(prefs, onStateChanged)
            }
            composable("edge_gestures") { EdgeGestureScreen(prefs, onStateChanged) }
            composable("routines") { RoutinesScreen(prefs, onStateChanged) }
            composable("recording") { RecordingSettingsScreen(prefs, onStateChanged) }
            composable("app_lock") { AppLockScreen(prefs, onStateChanged) }
            composable("calibration") { AutoCalibrationScreen(prefs, onStateChanged) }
            composable("profiles") { ProfilesScreen(prefs, onStateChanged) }
        }
    }
}
