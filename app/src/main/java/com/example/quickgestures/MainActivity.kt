package com.example.quickgestures

import android.Manifest
import android.app.Activity
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.data.GestureActionCatalog
import com.example.quickgestures.services.AccessibilityShortcutService
import com.example.quickgestures.ui.components.QuickBallOverlayView
import com.example.quickgestures.ui.screens.*
import com.example.quickgestures.utils.ActionExecutor
import com.example.quickgestures.utils.AppLockManager
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var prefs: AppPreferences
    private lateinit var lockManager: AppLockManager

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* النتيجة تُقرأ لاحقاً عبر ContextCompat.checkSelfPermission عند الحاجة */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = AppPreferences(applicationContext)
        lockManager = AppLockManager(applicationContext)

        requestRuntimePermissions()

        setContent {
            val systemDark = isSystemInDarkTheme()
            var themeMode by remember { mutableStateOf(prefs.themeMode) }
            val useDark = when (themeMode) {
                AppPreferences.ThemeMode.LIGHT -> false
                AppPreferences.ThemeMode.DARK -> true
                AppPreferences.ThemeMode.SYSTEM -> systemDark
            }

            val colorScheme = if (useDark) darkColorScheme() else lightColorScheme()

            MaterialTheme(colorScheme = colorScheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val navController = rememberNavController()
                        val backStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = backStackEntry?.destination?.route

                        QuickTouchNavHost(navController, prefs, lockManager, applicationContext)

                        // خروج بضغطتين رجوع من الشاشة الرئيسية مع تحذير بالضغطة الأولى
                        if (currentRoute == "home") {
                            HomeExitBackHandler()
                        }

                        // إعادة قراءة إعداد المظهر كل ما نرجع من شاشة الإعدادات
                        LaunchedEffect(currentRoute) {
                            themeMode = prefs.themeMode
                        }

                        // الكرة العائمة "جوا التطبيق بس"
                        var ballEnabled by remember { mutableStateOf(prefs.quickBallEnabled) }
                        var ballMode by remember { mutableStateOf(prefs.quickBallMode) }
                        LaunchedEffect(currentRoute) {
                            ballEnabled = prefs.quickBallEnabled
                            ballMode = prefs.quickBallMode
                        }

                        if (ballEnabled && ballMode == AppPreferences.QuickBallMode.IN_APP_ONLY) {
                            val actionExecutor = remember { ActionExecutor(applicationContext) }
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                                QuickBallOverlayView(
                                    config = prefs.quickBallRadialConfig,
                                    actionsCatalog = GestureActionCatalog::byId,
                                    isEdgeOnLeft = false,
                                    onActionTapped = { action -> actionExecutor.execute(action) },
                                    onLongPressMove = { _, _ -> }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestRuntimePermissions() {
        val neededPermissions = mutableListOf<String>()
        neededPermissions += Manifest.permission.RECORD_AUDIO
        neededPermissions += Manifest.permission.CAMERA
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            neededPermissions += Manifest.permission.POST_NOTIFICATIONS
        }

        val toRequest = neededPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(toRequest.toTypedArray())
        }

        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                android.content.Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
    }
}

/** ضغطة رجوع أولى بالشاشة الرئيسية = تحذير Toast، وضغطة ثانية خلال ثانيتين = خروج فعلي */
@Composable
private fun HomeExitBackHandler() {
    val context = LocalContext.current
    var armed by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (armed) {
            (context as? Activity)?.finish()
        } else {
            armed = true
            Toast.makeText(context, "اضغط رجوع مرة ثانية للخروج", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(armed) {
        if (armed) {
            delay(2000)
            armed = false
        }
    }
}

private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val expectedComponent = "${context.packageName}/${AccessibilityShortcutService::class.java.name}"
    val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        ?: return false
    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabledServices)
    while (splitter.hasNext()) {
        if (splitter.next().equals(expectedComponent, ignoreCase = true)) return true
    }
    return false
}

private fun queryInstalledUserApps(context: android.content.Context): List<InstalledAppInfo> {
    val pm = context.packageManager
    return pm.getInstalledApplications(PackageManager.GET_META_DATA)
        .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
        .filterNot { it.packageName == context.packageName }
        .map { InstalledAppInfo(it.packageName, pm.getApplicationLabel(it).toString()) }
        .sortedBy { it.label.lowercase() }
}

@Composable
private fun QuickTouchNavHost(
    navController: NavHostController,
    prefs: AppPreferences,
    lockManager: AppLockManager,
    appContext: android.content.Context
) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("quick_ball") { QuickBallConfigScreen(prefs) }
        composable("edge_gestures") { EdgeGestureScreen(prefs) }
        composable("back_tap") { BackTapScreen(prefs) }
        composable("quick_tiles") { QuickTilesScreen(prefs) }
        composable("routines") {
            var routines by remember { mutableStateOf(listOf<com.example.quickgestures.data.Routine>()) }
            RoutinesScreen(
                routines = routines,
                onSave = { saved ->
                    routines = if (routines.any { it.id == saved.id }) {
                        routines.map { if (it.id == saved.id) saved else it }
                    } else {
                        routines + saved
                    }
                },
                onDelete = { toDelete -> routines = routines.filterNot { it.id == toDelete.id } }
            )
        }
        composable("recording") { RecordingSettingsScreen() }
        composable("calibration") { AutoCalibrationScreen(prefs) }
        composable("profiles") { ProfilesScreen(onExport = { "{}" }, onImport = {}) }
        composable("app_lock") {
            val installedApps = remember { queryInstalledUserApps(appContext) }
            val accessibilityEnabled = remember { isAccessibilityServiceEnabled(appContext) }
            AppLockScreen(lockManager, installedApps = installedApps, isAccessibilityEnabled = accessibilityEnabled)
        }
        composable("network_speed") { NetworkSpeedScreen(prefs) }
        composable("settings") { SettingsScreen(prefs) }
    }
}

@Composable
private fun HomeScreen(navController: NavHostController) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Quick Touch", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        val items = listOf(
            "quick_ball" to "الكرة العائمة",
            "edge_gestures" to "إيماءات الحافة",
            "back_tap" to "النقر على الظهر",
            "quick_tiles" to "بلاطات مركز التحكم",
            "routines" to "الروتينات",
            "recording" to "التسجيل الصوتي",
            "calibration" to "الهزة والحساسية",
            "profiles" to "البروفايلات",
            "app_lock" to "قفل التطبيقات",
            "network_speed" to "مراقب سرعة الإنترنت",
            "settings" to "الإعدادات"
        )

        items.forEach { (route, label) ->
            OutlinedButton(
                onClick = { navController.navigate(route) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) { Text(label) }
        }
    }
}
