package com.example.quickgestures.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import com.example.quickgestures.data.GestureAction

/**
 * نصف دائرة عند حافة الشاشة: النص الظاهر منها فيه أيقونة الإجراء الحالي،
 * سحب لفوق/تحت عالكرة نفسها بيدوّر بين الإجراءات المتاحة (حتى لو كترت)،
 * ونقرة بتنفذ الإجراء الظاهر حالياً. النص التاني من الدائرة يضل خارج حافة الشاشة تماماً.
 */
class QuickBallOverlayView(
    context: Context,
    private var actions: List<GestureAction>,
    private val onActionSelected: (GestureAction) -> Unit,
    private val onDragMoved: (rawX: Float, rawY: Float) -> Unit
) : View(context) {

    private var currentIndex = 0
    private val ballPaint = Paint().apply { color = Color.argb(190, 20, 20, 20); isAntiAlias = true }
    private val iconPaint = Paint().apply { color = Color.WHITE; isAntiAlias = true; textSize = 34f; textAlign = Paint.Align.CENTER }

    private var downY = 0f
    private var downX = 0f
    private var isDragging = false

    fun updateActions(newActions: List<GestureAction>) {
        actions = newActions
        if (currentIndex >= actions.size) currentIndex = 0
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = width.coerceAtMost(height).toFloat() / 2f
        canvas.drawCircle(width / 2f, height / 2f, radius, ballPaint)
        if (actions.isNotEmpty()) {
            val label = actions[currentIndex].label.take(1)
            canvas.drawText(label, width / 2f, height / 2f + 12f, iconPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX; downY = event.rawY
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.rawY - downY
                val dx = event.rawX - downX
                if (kotlin.math.abs(dx) + kotlin.math.abs(dy) > 40) {
                    isDragging = true
                    // سحب عمودي قصير = تدوير بين الإجراءات، سحب طويل/بالاتجاهين = تحريك الكرة
                    if (kotlin.math.abs(dy) in 40.0..140.0 && kotlin.math.abs(dx) < 40) {
                        rotate(if (dy < 0) -1 else 1)
                        downY = event.rawY
                    } else {
                        onDragMoved(event.rawX, event.rawY)
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging && actions.isNotEmpty()) {
                    onActionSelected(actions[currentIndex])
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun rotate(step: Int) {
        if (actions.isEmpty()) return
        currentIndex = (currentIndex + step + actions.size) % actions.size
        invalidate()
    }
}
