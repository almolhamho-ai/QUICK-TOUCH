package com.example.quickgestures.services

import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.IBinder
import java.io.File

class SecretRecorderService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRecording) {
            startSecretRecording()
        } else {
            stopSecretRecording()
        }
        return START_STICKY
    }

    private fun startSecretRecording() {
        val outputFile = File(getExternalFilesDir(null), "secret_record_${System.currentTimeMillis()}.mp3")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
        isRecording = true
    }

    private fun stopSecretRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecording = false
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
