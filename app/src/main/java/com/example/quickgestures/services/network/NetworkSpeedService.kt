package com.example.quickgestures.services.network

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.TrafficStats
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.data.NetworkSpeedDisplayMode
import com.example.quickgestures.utils.buildAndStartSilentForegroundNotification

/**
 * مؤشر عائم شفاف يرتسم فوق منطقة شريط الحالة (بدون Root ما فيه طريقة لحقن أيقونة فعلية
 * داخل شريط الحالة النظامي، فالحل هو Overlay بنفس المنطقة بصريًا).
 */
class NetworkSpeedService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: AppPreferences

    private var lastRxBytes = TrafficStats.getTotalRxBytes()
    private var lastTxBytes = TrafficStats.getTotalTxBytes()
    private var lastTimestamp = System.currentTimeMillis()

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateSpeed()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        buildAndStartSilentForegroundNotification(
            notificationId = 4203,
            channelId = "quick_touch_network_speed_channel",
            channelName = "Quick Touch - مراقب السرعة",
            contentText = "مراقبة سرعة الإنترنت تعمل"
        )
        prefs = AppPreferences(applicationContext)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupOverlay()
        handler.post(updateRunnable)
    }

    private fun setupOverlay() {
        val density = resources.displayMetrics.density

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (70 * density).toInt()
            y = (2 * density).toInt()
        }

        val textView = TextView(this).apply {
            textSize = 11f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.argb(140, 0, 0, 0))
            val pad = (4 * density).toInt()
            setPadding(pad * 2, pad, pad * 2, pad)
        }
        overlayView = textView
        runCatching { windowManager.addView(textView, params) }
    }

    private fun updateSpeed() {
        if (!prefs.networkSpeedEnabled) {
            overlayView?.text = ""
            return
        }

        val mode = prefs.networkSpeedDisplayMode
        val now = System.currentTimeMillis()
        val elapsedSec = ((now - lastTimestamp).coerceAtLeast(1)) / 1000.0
        val rxNow = TrafficStats.getTotalRxBytes()
        val txNow = TrafficStats.getTotalTxBytes()

        val downloadKbps = ((rxNow - lastRxBytes) / elapsedSec / 1024).toInt()
        val uploadKbps = ((txNow - lastTxBytes) / elapsedSec / 1024).toInt()

        lastRxBytes = rxNow
        lastTxBytes = txNow
        lastTimestamp = now

        overlayView?.text = when (mode) {
            NetworkSpeedDisplayMode.DOWNLOAD_ONLY -> "↓${downloadKbps}KB/s"
            NetworkSpeedDisplayMode.UPLOAD_ONLY -> "↑${uploadKbps}KB/s"
            NetworkSpeedDisplayMode.BOTH -> "↓$downloadKbps ↑${uploadKbps}KB/s"
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateRunnable)
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
