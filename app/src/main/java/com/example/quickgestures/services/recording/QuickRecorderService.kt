package com.example.quickgestures.services.recording

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.quickgestures.MainActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * تسجيل صوت **شفاف بالكامل**: إشعار دائم لا يمكن إخفاؤه طول ما التسجيل شغال،
 * جودة عالية (AAC 128kbps / 44.1kHz)، بيشتغل حتى لو الشاشة مقفولة (Foreground Service).
 * الملفات بتنحفظ بمجلد خاص بالتطبيق ومحمي دايماً بقفل التطبيق (AppLockManager)
 * بغض النظر عن إعدادات قفل التطبيقات العامة.
 */
class QuickRecorderService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var currentFile: File? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRecording) start() else stop()
        return START_STICKY
    }

    private fun start() {
        val dir = File(filesDir, "recordings").apply { mkdirs() }
        val name = "REC_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.m4a"
        val outputFile = File(dir, name)
        currentFile = outputFile

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
        isRecording = true
        startForeground(NOTIFICATION_ID, buildOngoingNotification())
    }

    private fun stop() {
        runCatching {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        }
        mediaRecorder = null
        isRecording = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** إشعار دائم وواضح - هيدا شرط أساسي بأندرويد لأي Foreground Service من نوع مايكروفون، مش خيار */
    private fun buildOngoingNotification(): android.app.Notification {
        val channelId = "recording_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "تسجيل الصوت",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "يظهر هذا الإشعار طول فترة تسجيل الصوت" }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, QuickRecorderService::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("🔴 التسجيل شغال الآن")
            .setContentText("لمسة سريعة تسجّل صوت - اضغط لإيقافه")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true) // ما فيك تسحبه أو تخفيه طول ما التسجيل مستمر
            .setContentIntent(openAppIntent)
            .addAction(android.R.drawable.ic_media_pause, "إيقاف التسجيل", stopIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 4321

        fun toggle(context: Context) {
            context.startForegroundService(Intent(context, QuickRecorderService::class.java))
        }

        fun recordingsDir(context: Context): File = File(context.filesDir, "recordings")
    }
}
