package com.samet.proapp

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

class ZoomableImageView@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var scaleFactor = 1f
    private val minScale = 1f
    private val maxScale = 5f
    private var lastFocusX = 0f
    private var lastFocusY = 0f
    private var initialDistance = 0f
    private var mode = Mode.NONE

    private enum class Mode {
        NONE, DRAG, ZOOM
    }

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(minScale, maxScale)

                scaleX = scaleFactor
                scaleY = scaleFactor

                return true
            }
        })

    init {
        scaleType = ScaleType.FIT_CENTER
        setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)

            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    mode = Mode.DRAG
                    lastFocusX = event.x
                    lastFocusY = event.y
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    mode = Mode.ZOOM
                    initialDistance = spacing(event)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mode == Mode.DRAG && scaleFactor > 1f) {
                        val dx = event.x - lastFocusX
                        val dy = event.y - lastFocusY
                        translationX += dx
                        translationY += dy
                        lastFocusX = event.x
                        lastFocusY = event.y
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    mode = Mode.NONE
                }
            }
            true
        }
    }

    private fun spacing(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return Math.sqrt((x * x + y * y).toDouble()).toFloat()
    }
}