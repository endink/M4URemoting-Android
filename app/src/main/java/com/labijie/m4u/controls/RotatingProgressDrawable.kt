package com.labijie.m4u.controls

import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.Message


/**
 *
 *
 * @author cpacm
 *
 */
class RotatingProgressDrawable : Drawable {
    private var mPaint: Paint? = null
    private var progressPaint: Paint? = null
    private var drawable: Drawable
    private var mWidth = 0
    private var mRotation = 0f
    private var rectF: RectF? = null
    private var progress //进度条
            = 0f
    private var progressPercent //进度条宽度
            = 0
    private var progressColor //进度条颜色
            = 0

    // 旋转控制
    private var rotateHandler: RotateHandler? = null

    constructor(drawable: Drawable) {
        initDrawable()
        this.drawable = drawable
        circleBitmapFromDrawable(this.drawable)
    }

    constructor(res: Resources?, bitmap: Bitmap?) {
        initDrawable()
        drawable = BitmapDrawable(res, bitmap)
        circleBitmapFromDrawable(drawable)
    }

    private fun initDrawable() {
        progressPercent = 3
        progress = 0f
        progressColor = Color.RED
        rotateHandler = RotateHandler(Looper.getMainLooper())
        rectF = RectF()
        progressPaint = Paint()
        progressPaint!!.color = progressColor
        progressPaint!!.style = Paint.Style.STROKE
        progressPaint!!.isAntiAlias = true
    }

    var rotation: Float
        get() = mRotation
        set(rotation) {
            mRotation = rotation
            invalidateSelf()
        }

    override fun draw(canvas: Canvas) {
        val progressWidth = mWidth * progressPercent / 100f
        val halfWidth = progressWidth / 2
        // 画背景图
        canvas.save()
        canvas.rotate(mRotation, bounds.centerX().toFloat(), bounds.centerY().toFloat())
        val scale = 1 - progressWidth * 2.0f / mWidth
        canvas.scale(scale, scale, mWidth / 2.0f, mWidth / 2.0f)
        canvas.drawCircle(
            (mWidth / 2).toFloat(), (mWidth / 2).toFloat(), (mWidth / 2).toFloat(),
            mPaint!!
        )
        canvas.restore()
        // 画进度条
        rectF!![halfWidth, halfWidth, mWidth - halfWidth] = mWidth - halfWidth
        canvas.drawArc(rectF!!, -90f, progress, false, progressPaint!!)
    }

    /**
     * set progress
     * 设置进度
     *
     * @param progress 0-100
     */
    fun setProgress(progress: Float) {
        var progress = progress
        if (progress < 0 || progress > 100) return
        progress = progress * 360 / 100f
        this.progress = progress
        invalidateSelf()
    }

    /**
     * 设置进度条相对于图片的百分比，默认为3%
     *
     * @param percent 0-100
     */
    fun setProgressWidthPercent(percent: Int) {
        progressPercent = percent
        if (mWidth > 0) {
            val progressWidth = mWidth * percent / 100f
            progressPaint!!.strokeWidth = progressWidth
        }
        invalidateSelf()
    }

    /**
     * 设置进度条的颜色
     *
     * @param progressColor
     */
    fun setProgressColor(progressColor: Int) {
        this.progressColor = progressColor
        progressPaint!!.color = progressColor
        invalidateSelf()
    }

    /**
     * 是否开始旋转
     *
     * @param rotate
     */
    fun rotate(rotate: Boolean) {
        rotateHandler!!.removeMessages(0)
        if (rotate) {
            rotateHandler!!.sendEmptyMessage(0)
        }
    }

    /**
     * 圆形
     */
    private fun circleBitmap(mBitmap: Bitmap) {
        val bitmapShader = BitmapShader(
            mBitmap, Shader.TileMode.CLAMP,
            Shader.TileMode.CLAMP
        )
        mPaint = Paint()
        mPaint!!.isAntiAlias = true
        mPaint!!.shader = bitmapShader
        mWidth = Math.min(mBitmap.width, mBitmap.height)
        val progressWidth = mWidth * progressPercent / 100f
        progressPaint!!.strokeWidth = progressWidth
    }

    private fun circleBitmapFromDrawable(drawable: Drawable) {
        val mBitmap: Bitmap
        mBitmap = if (drawable is ColorDrawable) {
            Bitmap.createBitmap(
                COLORDRAWABLE_DIMENSION,
                COLORDRAWABLE_DIMENSION, BITMAP_CONFIG
            )
        } else {
            Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight, BITMAP_CONFIG
            )
        }
        val canvas = Canvas(mBitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        circleBitmap(mBitmap)
    }

    override fun getIntrinsicWidth(): Int {
        return mWidth
    }

    override fun getIntrinsicHeight(): Int {
        return mWidth
    }

    override fun setAlpha(alpha: Int) {
        mPaint!!.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mPaint!!.colorFilter = cf
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    private inner class RotateHandler internal constructor(looper: Looper?) : Handler(
        looper!!
    ) {
        override fun handleMessage(msg: Message) {
            if (msg.what == 0) {
                mRotation = mRotation + 1
                if (mRotation > 360) {
                    mRotation = 0f
                }
                rotation = mRotation
                rotateHandler!!.sendEmptyMessageDelayed(0, ROTATION_DEFAULT_SPEED.toLong())
            }
            super.handleMessage(msg)
        }
    }

    companion object {
        private const val COLORDRAWABLE_DIMENSION = 2
        private val BITMAP_CONFIG = Bitmap.Config.ARGB_4444
        private const val ROTATION_DEFAULT_SPEED = 25
    }
}
