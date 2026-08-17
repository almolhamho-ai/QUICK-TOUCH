package com.example.quickgestures.services.edge

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.data.EdgeShape
import com.example.quickgestures.services.routine.RoutineEngine
import com.example.quickgestures.utils.ActionExecutor
import kotlin.math.abs

/**
 * خدمة عائمة شفافة تحط شرايط رفيعة على حواف الشاشة (يمين/يسار)، وبترصد نمط السحبة
 * وتصنّفها لشكل بسيط: خط مستقيم / زاوية L / نص دائرة، وبعدين تنفذ الإجراء المربوط فيه.
 */
class EdgeGestureService : Service() {

    private lateinit var windowManager: WindowManager
    private var edgeView: View? = null
    private lateinit var prefs: AppPreferences

    private val touchPoints = mutableListOf<Pair<Float, Float>>()

    override fun onCreate() {
        super.onCreate()
        prefs = AppPreferences(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        addEdgeStrip()
    }

    private fun addEdgeStrip() {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        val stripWidthPx = (18 * metrics.density).toInt()
        val overlayType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        val params = WindowManager.LayoutParams(
            stripWidthPx,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
        }

        val view = object : View(this) {
            override fun onTouchEvent(event: MotionEvent): Boolean {
                handleTouch(event)
                return true
            }
        }
        view.setOnTouchListener { _, event -> handleTouch(event); true }

        edgeView = view
        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleTouch(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchPoints.clear()
                touchPoints.add(event.x to event.y)
            }
            MotionEvent.ACTION_MOVE -> touchPoints.add(event.x to event.y)
            MotionEvent.ACTION_UP -> {
                touchPoints.add(event.x to event.y)
                classifyAndTrigger()
                touchPoints.clear()
            }
        }
    }

    /** تصنيف مبسّط: نحسب عدد مرات تغيّر اتجاه الحركة (X مقابل Y) لتمييز خط عن زاوية عن نص دائرة */
    private fun classifyAndTrigger() {
        if (!prefs.edgeGestureEnabled || touchPoints.size < 4) return

        var horizontalMoves = 0
        var verticalMoves = 0
        var directionChanges = 0
        var lastDx = 0f
        var lastDy = 0f

        for (i in 1 until touchPoints.size) {
            val (x0, y0) = touchPoints[i - 1]
            val (x1, y1) = touchPoints[i]
            val dx = x1 - x0
            val dy = y1 - y0
            if (abs(dx) > abs(dy)) horizontalMoves++ else verticalMoves++
            if (lastDy != 0f && (dy > 0) != (lastDy > 0)) directionChanges++
            lastDx = dx; lastDy = dy
        }

        val shape = when {
            directionChanges >= 2 -> EdgeShape.HALF_CIRCLE
            horizontalMoves > 0 && verticalMoves > 0 -> EdgeShape.CORNER_L
            else -> EdgeShape.LINE
        }

        prefs.getEdgeMapping(shape)?.let { action ->
            ActionExecutor.execute(this, com.example.quickgestures.data.GestureActionRef(action))
        }
        RoutineEngine.onEdgeGestureTriggered(this, shape)
    }

    override fun onDestroy() {
        super.onDestroy()
        edgeView?.let { runCatching { windowManager.removeView(it) } }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
