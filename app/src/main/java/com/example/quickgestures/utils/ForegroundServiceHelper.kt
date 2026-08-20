package com.example.quickgestures.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.quickgestures.R

/**
 * أي Service خلفي بالتطبيق (غير التسجيل الصوتي، يلي إلو إشعار خاص واضح ومختلف عمداً)
 * لازم يستدعي startForeground() بأول onCreate خلال أقل من 5 ثواني، وإلا أندرويد 8+ بيوقفه
 * أو بيكرش التطبيق. هاي دالة موحدة تبني إشعار بسيط منخفض الأولوية لهيك خدمات.
 */
fun Service.buildAndStartSilentForegroundNotification(
    notificationId: Int,
    channelId: String,
    channelName: String,
    contentText: String
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_MIN)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(this, channelId)
        .setContentTitle(getString(R.string.app_name))
        .setContentText(contentText)
        .setSmallIcon(android.R.drawable.presence_online)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .build()

    startForeground(notificationId, notification)
}
