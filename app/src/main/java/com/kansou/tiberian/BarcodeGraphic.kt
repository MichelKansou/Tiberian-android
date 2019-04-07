package com.kansou.tiberian

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.android.gms.vision.barcode.Barcode
import com.kansou.tiberian.camera.GraphicOverlay


class BarcodeGraphic internal constructor(overlay: GraphicOverlay<*>) : GraphicOverlay.Graphic(overlay) {

    var id: Int = 0

    private val mRectPaint: Paint
    @Volatile
    var barcode: Barcode? = null
        private set

    init {

        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.size
        val selectedColor = COLOR_CHOICES[mCurrentColorIndex]

        mRectPaint = Paint()
        mRectPaint.setColor(selectedColor)
        mRectPaint.setStyle(Paint.Style.STROKE)
        mRectPaint.setStrokeWidth(4.0f)
    }

    /**
     * Updates the barcode instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    internal fun updateItem(barcode: Barcode) {
        if (barcode.format == Barcode.QR_CODE) {
            this.barcode = barcode
            postInvalidate()
        }
    }

    /**
     * Draws the barcode annotations for position, size, and raw value on the supplied canvas.
     */
    override fun draw(canvas: Canvas) {
        val barcode = this.barcode ?: return

        // Draws the bounding box around the barcode.
        val rect = RectF(barcode.boundingBox)
        rect.left = translateX(rect.left)
        rect.top = translateY(rect.top)
        rect.right = translateX(rect.right)
        rect.bottom = translateY(rect.bottom)
        canvas.drawRect(rect, mRectPaint)
    }

    companion object {

        private val COLOR_CHOICES = intArrayOf(Color.BLUE, Color.CYAN, Color.GREEN)

        private var mCurrentColorIndex = 0
    }
}