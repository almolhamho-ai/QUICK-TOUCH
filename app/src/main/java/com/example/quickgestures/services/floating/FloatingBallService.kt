package com.example.quickgestures.services.floating

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.quickgestures.R
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.data.GestureActionCatalog
import com.example.quickgestures.ui.components.QuickBallOverlayView
import com.example.quickgestures.utils.ActionExecutor

/**
 * تشغل الكرة العائمة على مستوى النظام (خيار "تعمل برا التطبيق كمان").
 *
 * ملاحظتين تقنيتين ضروريتين لأي Foreground Service بيرسم Compose View فوق نوافذ التطبيقات التانية:
 *  1) لازم تستدعي startForeground() فوراً بأول onCreate/onStartCommand، وإلا أندرويد 8+ بيوقف
 *     الخدمة (وأحياناً بيسبب تجمّد/اختفاء الواجهة اللي كنت شايفها).
 *  2) ComposeView المضاف يدوياً عبر WindowManager (مش جوا Activity) محتاج LifecycleOwner و
 *     SavedStateRegistryOwner و ViewModelStoreOwner يدويين، وإلا بيكرش فوراً لما يحاول يرسم.
 */
class FloatingBallService :
    Service(),
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private lateinit var prefs: AppPreferences
    private lateinit var actionExecutor: ActionExecutor

    private var ballX = 0
    private var ballY = 400

    override fun onCreate() {
        super.onCreate()

        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        createNotificationChannelIfNeeded()
        startForeground(NOTIFICATION_ID, buildNotification())

        prefs = AppPreferences(applicationContext)
        actionExecutor = ActionExecutor(applicationContext)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        addOverlay()

        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    private fun addOverlay() {
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = ballX
            y = ballY
        }

        val view = ComposeView(this).apply {
            // بدون هالأسطر الثلاثة، Compose بيكرش فوراً لأنو مش جوا Activity عادية
            setViewTreeLifecycleOwner(this@FloatingBallService)
            setViewTreeSavedStateRegistryOwner(this@FloatingBallService)
            setViewTreeViewModelStoreOwner(this@FloatingBallService)

            setContent {
                QuickBallOverlayView(
                    config = prefs.quickBallRadialConfig,
                    actionsCatalog = GestureActionCatalog::byId,
                    isEdgeOnLeft = ballX < 100,
                    onActionTapped = { action -> actionExecutor.execute(action) },
                    onLongPressMove = { dx, dy ->
                        ballX += dx.toInt()
                        ballY += dy.toInt()
                        params.x = ballX
                        params.y = ballY
                        runCatching { windowManager.updateViewLayout(this, params) }
                    }
                )
            }
        }

        composeView = view
        runCatching { windowManager.addView(view, params) }
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("الكرة العائمة تعمل")
            .setSmallIcon(android.R.drawable.presence_online)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Quick Touch - الكرة العائمة",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        composeView?.let { runCatching { windowManager.removeView(it) } }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "quick_touch_floating_ball_channel"
        private const val NOTIFICATION_ID = 4202
    }
}
