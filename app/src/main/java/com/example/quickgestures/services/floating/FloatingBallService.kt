package com.example.quickgestures.services.floating

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.data.GestureActionRef
import com.example.quickgestures.ui.components.QuickBallOverlayView
import com.example.quickgestures.utils.ActionExecutor

/**
 * الخدمة يلي بتشغّل الكرة العائمة على مستوى النظام (فوق كل التطبيقات).
 * تشتغل بس إذا المستخدم فعّل "تعمل برا التطبيق" من الإعدادات.
 */
class FloatingBallService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: AppPreferences
    private var ballView: QuickBallOverlayView? = null
    private lateinit var layoutParams: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()
        prefs = AppPreferences(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        addBall()
    }

    private fun addBall() {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val sizePx = (56 * metrics.density).toInt()

        layoutParams = WindowManager.LayoutParams(
            sizePx, sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = metrics.widthPixels - sizePx
            y = metrics.heightPixels / 3
        }

        val view = QuickBallOverlayView(
            context = this,
            actions = prefs.quickBallActions,
            onActionSelected = { action -> ActionExecutor.execute(this, GestureActionRef(action)) },
            onDragMoved = { rawX, rawY ->
                layoutParams.x = (rawX - (layoutParams.width / 2)).toInt().coerceIn(0, metrics.widthPixels - layoutParams.width)
                layoutParams.y = (rawY - (layoutParams.height / 2)).toInt().coerceIn(0, metrics.heightPixels - layoutParams.height)
                runCatching { windowManager.updateViewLayout(ballView, layoutParams) }
            }
        )
        ballView = view
        try {
            windowManager.addView(view, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ballView?.updateActions(prefs.quickBallActions)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        ballView?.let { runCatching { windowManager.removeView(it) } }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
