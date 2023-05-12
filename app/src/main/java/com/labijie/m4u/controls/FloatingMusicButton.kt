package com.labijie.m4u.controls

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.lang.reflect.InvocationTargetException


/**
 *
 *
 * 装载旋转进度按钮位图的按钮，继承自[FloatingActionButton]
 *
 *
 *
 *
 * @author cpacm
 *
 */
class FloatingMusicButton : FloatingActionButton {
    private var coverDrawable: RotatingProgressDrawable? = null
    private var percent = 0
    private var color = 0
    private var backgroundHint: ColorStateList? = null
    private var progress = 0f
    private var isRotation = false

    constructor(context: Context?) : super(context!!) {
        setMaxImageSize()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
        setMaxImageSize()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!, attrs, defStyleAttr
    ) {
        setMaxImageSize()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
    }

    /**
     * 利用反射重新定义fab图片的大小，默认充满整个fab
     */
    fun setMaxImageSize() {
        try {
            val clazz: Class<*> = javaClass.superclass as Class<*>
            val sizeMethod = clazz.getDeclaredMethod("getSizeDimension")
            sizeMethod.isAccessible = true
            val size = sizeMethod.invoke(this) as Int
            //set fab maxsize
            val field = clazz.getDeclaredField("maxImageSize")
            field.isAccessible = true
            field.setInt(this, size)
            //get fab impl
            val field2 = clazz.getDeclaredField("impl")
            field2.isAccessible = true
            val o = field2[this]
            //set fabimpl maxsize
            val maxMethod = o.javaClass.superclass.getDeclaredMethod(
                "setMaxImageSize",
                Int::class.javaPrimitiveType
            )
            maxMethod.isAccessible = true
            maxMethod.invoke(o, size)
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        }
        //postInvalidate();
    }

    /**
     * 对fmb进行配置
     *
     * @param percent        进度条宽度百分比
     * @param color          进度条颜色
     * @param backgroundHint fmb背景颜色
     */
    fun config(percent: Int, color: Int, backgroundHint: ColorStateList?) {
        this.percent = percent
        this.color = color
        this.backgroundHint = backgroundHint
        config()
    }

    fun config() {
        if (coverDrawable != null) {
            coverDrawable!!.setProgressWidthPercent(percent)
            coverDrawable!!.setProgressColor(color)
            if (backgroundHint != null) {
                backgroundTintList = backgroundHint
            }
            coverDrawable!!.setProgress(progress)
            coverDrawable!!.rotate(isRotation)
            //setMaxImageSize();
        }
    }

    /**
     * 设置进度
     *
     * @param progress
     */
    fun setProgress(progress: Float) {
        this.progress = progress
        if (coverDrawable != null) {
            coverDrawable!!.setProgress(progress)
        }
    }

    /**
     * 设置按钮背景
     *
     * @param drawable
     */
    fun setCoverDrawable(drawable: Drawable?) {
        coverDrawable = RotatingProgressDrawable(drawable!!)
        config()
        setImageDrawable(coverDrawable)
        postInvalidate()
    }

    fun setCover(bitmap: Bitmap?) {
        coverDrawable = RotatingProgressDrawable(resources, bitmap)
        config()
        setImageDrawable(coverDrawable)
        postInvalidate()
    }

    fun rotate(rotate: Boolean) {
        coverDrawable!!.rotate(rotate)
        isRotation = rotate
    }

    override fun onSaveInstanceState(): Parcelable? {
        super.onSaveInstanceState()
        val bundle = Bundle()
        bundle.putBoolean("rotation", isRotation)
        bundle.putFloat("progress", progress)
        if (coverDrawable != null) {
            bundle.putFloat("rotation_angle", coverDrawable!!.rotation)
        }
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        super.onRestoreInstanceState(state)
        val bundle = state as Bundle
        isRotation = bundle.getBoolean("rotation")
        progress = bundle.getFloat("progress")
        requestLayout()
    }
}