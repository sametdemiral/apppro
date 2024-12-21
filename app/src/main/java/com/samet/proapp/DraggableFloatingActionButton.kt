package com.samet.proapp

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DraggableFloatingActionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FloatingActionButton(context, attrs, defStyleAttr) {

    private var lastX: Int = 0
    private var lastY: Int = 0
    private var isDragging = false
    private var downX: Float = 0f
    private var downY: Float = 0f

    init {
        // FAB'ın tıklanabilir ve odaklanabilir olduğundan emin olalım
        isClickable = true
        isFocusable = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val parent = parent as ViewGroup
        val parentWidth = parent.width
        val parentHeight = parent.height

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Başlangıç pozisyonunu kaydet
                downX = event.x
                downY = event.y
                lastX = x.toInt()
                lastY = y.toInt()
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val moveX = event.x
                val moveY = event.y

                // Hareket mesafesini hesapla
                val distanceX = moveX - downX
                val distanceY = moveY - downY

                // Eğer yeterince hareket ettiyse sürüklemeyi başlat
                if (!isDragging && (Math.abs(distanceX) > 10 || Math.abs(distanceY) > 10)) {
                    isDragging = true
                }

                if (isDragging) {
                    // Yeni pozisyonu hesapla
                    var newX = (lastX + distanceX).toInt()
                    var newY = (lastY + distanceY).toInt()

                    // Ekran sınırlarını kontrol et
                    newX = newX.coerceIn(0, parentWidth - width)
                    newY = newY.coerceIn(0, parentHeight - height)

                    // FAB'ı yeni pozisyona taşı
                    x = newX.toFloat()
                    y = newY.toFloat()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    // Eğer sürükleme olmadıysa normal tıklama olarak değerlendir
                    performClick()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}