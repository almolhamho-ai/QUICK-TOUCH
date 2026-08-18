package com.example.quickgestures.services.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.TrafficStats
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.data.NetworkSpeedDisplayMode

/**
 * مؤشر سرعة الإنترنت: نافذة عائمة شفافة ترتسم فوق منطقة شريط الحالة نفسها (مش إشعار قابل للسحب)،
 * فبتبين وكأنها جزء من الشريط جنب الساعة والبطارية والواي فاي.
 *
 * ملاحظة تقنية: أندرويد ما بيسمح لتطبيق عادي (بدون Root) يحقن أيقونة فعلية داخل شريط الحالة
 * النظامي، فهاي الطريقة (نافذة عائمة بنفس ارتفاع الشريط) هي نفسها يلي تستخدمها كل تطبيقات
 * مراقبة السرعة المعروفة بالسوق بدون Root.
 *
 * أندرويد بيتطلب إشعار Foreground Service إلزامي حتى تضل الخدمة شغالة بالخلفية - خليناه
 * إشعار صامت وأقل أهمية ممكنة (LOW) بدل ما نعتمد عليه لعرض السرعة.
 */
class NetworkSpeedService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: AppPreferences
    private var overlayView: TextView? = null

    private val handler = Handler(Looper.getMainLooper())
    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastTimeStamp = 0L
    private val updateInterval = 1000L

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateSpeed()
            handler.postDelayed(this, updateInterval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = AppPreferences(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        lastRxBytes = TrafficStats.getTotalRxBytes()
        lastTxBytes = TrafficStats.getTotalTxBytes()
        lastTimeStamp = System.currentTimeMillis()
        addOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildSilentNotification())
        if (prefs.networkSpeedDisplayMode == NetworkSpeedDisplayMode.OFF) {
            stopSelf()
            return START_NOT_STICKY
        }
        handler.removeCallbacks(updateRunnable)
        handler.post(updateRunnable)
        return START_STICKY
    }

    private fun addOverlay() {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        val textView = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 10f
            setPadding(6, 0, 6, 0)
            text = "..."
        }

        val statusBarHeightPx = statusBarHeight()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            statusBarHeightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            // نخليها قبل أيقونات النظام (البطارية/الواي فاي/الساعة) بشوي مسافة من الحافة
            x = (60 * metrics.density).toInt()
            y = 0
        }

        overlayView = textView
        try {
            windowManager.addView(textView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun statusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId)
        else (24 * resources.displayMetrics.density).toInt()
    }

    private fun updateSpeed() {
        val mode = prefs.networkSpeedDisplayMode
        if (mode == NetworkSpeedDisplayMode.OFF) {
            stopSelf()
            return
        }

        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTx = TrafficStats.getTotalTxBytes()
        val now = System.currentTimeMillis()
        if (currentRx == TrafficStats.UNSUPPORTED.toLong()) return

        val elapsedSeconds = ((now - lastTimeStamp).coerceAtLeast(1)) / 1000.0
        val downloadSpeed = (currentRx - lastRxBytes) / elapsedSeconds
        val uploadSpeed = (currentTx - lastTxBytes) / elapsedSeconds

        lastRxBytes = currentRx
        lastTxBytes = currentTx
        lastTimeStamp = now

        val text = when (mode) {
            NetworkSpeedDisplayMode.DOWNLOAD_ONLY -> "↓${formatSpeed(downloadSpeed)}"
            NetworkSpeedDisplayMode.UPLOAD_ONLY -> "↑${formatSpeed(uploadSpeed)}"
            NetworkSpeedDisplayMode.BOTH -> "↓${formatSpeed(downloadSpeed)} ↑${formatSpeed(uploadSpeed)}"
            NetworkSpeedDisplayMode.OFF -> ""
        }

        overlayView?.text = text
    }

    private fun formatSpeed(bytesPerSecond: Double): String {
        val kb = bytesPerSecond / 1024.0
        return if (kb < 1024) "%.0fKB/s".format(kb) else "%.1fMB/s".format(kb / 1024.0)
    }

    /** إشعار صامت وإلزامي فقط عشان أندرويد بيمنع Foreground Service بدون إشعار - ما بيعرض أي أرقام */
    private fun buildSilentNotification(): android.app.Notification {
        val channelId = "network_speed_silent_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "مراقب سرعة الإنترنت",
                NotificationManager.IMPORTANCE_MIN
            ).apply { description = "يبقي مؤشر السرعة العائم شغال بالخلفية" }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("مراقب سرعة الإنترنت شغال")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        overlayView?.let { runCatching { windowManager.removeView(it) } }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 5566

        fun applyMode(context: Context, mode: NetworkSpeedDisplayMode) {
            val prefs = AppPreferences(context)
            prefs.networkSpeedDisplayMode = mode
            if (mode == NetworkSpeedDisplayMode.OFF) {
                context.stopService(Intent(context, NetworkSpeedService::class.java))
            } else {
                context.startForegroundService(Intent(context, NetworkSpeedService::class.java))
            }
        }
    }
}
