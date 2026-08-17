package com.example.quickgestures

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.quickgestures.ui.components.QuickBallWidget
import com.example.quickgestures.ui.screens.AutoCalibrationScreen
import com.example.quickgestures.ui.screens.GestureSettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainAppStructure(versionName = BuildConfig.VERSION_NAME)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppStructure(versionName: String) {
    val navController = rememberNavController()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Gestures v$versionName") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        navController.navigate("settings")
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "الإعدادات") },
                    label = { Text("الإيماءات") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        navController.navigate("calibration")
                    },
                    icon = { Icon(Icons.Default.Build, contentDescription = "المعايرة") },
                    label = { Text("المعايرة") }
                )
            }
        },
        floatingActionButton = {
            // كرة الاختصارات السريعة MIUI Quick Ball
            QuickBallWidget()
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "settings",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("settings") {
                GestureSettingsScreen()
            }
            composable("calibration") {
                AutoCalibrationScreen(onCalibrationComplete = { sensitivity ->
                    // حفظ قيمة الحساسية الجديدة تلقائياً
                })
            }
        }
    }
}
