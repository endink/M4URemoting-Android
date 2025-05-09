package com.labijie.m4u.controls

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.TargetApi
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout.DefaultBehavior
import androidx.core.view.ViewCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.labijie.m4u.R


/**
 *
 *
 *
 * 可以通过调用 [.addButton] 和 [.removeButton] 来动态增减按钮数量。
 *
 * @author cpacm
 *
 */
@DefaultBehavior(FloatingMusicMenu.Behavior::class)
class FloatingMusicMenu : ViewGroup {
    private var floatingMusicButton: FloatingMusicButton? = null
    private var showAnimation: AnimatorSet? = null
    private var hideAnimation: AnimatorSet? = null
    private var progressWidthPercent = 0
    private var progressColor = 0
    private var progress = 0f
    private var buttonInterval = 0f
    private var backgroundTint: ColorStateList? = null
    private var cover: Drawable? = null
    var isExpanded = false
        private set
    private var isHided = false
    private var floatingDirection = 0

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        initMenu(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initMenu(context, attrs)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        initMenu(context, attrs)
    }

    private fun initMenu(context: Context, attrs: AttributeSet?) {
        val attr = context.obtainStyledAttributes(attrs, R.styleable.FloatingMusicMenu, 0, 0)
        progressWidthPercent =
            attr.getInteger(R.styleable.FloatingMusicMenu_fmm_progress_percent, 3)
        progressColor = attr.getColor(
            R.styleable.FloatingMusicMenu_fmm_progress_color, resources.getColor(
                android.R.color.holo_blue_dark
            )
        )
        progress = attr.getFloat(R.styleable.FloatingMusicMenu_fmm_progress, 0f)
        buttonInterval = attr.getDimension(R.styleable.FloatingMusicMenu_fmm_button_interval, 4f)
        buttonInterval = dp2px(buttonInterval)
        /*       if (Build.VERSION.SDK_INT < 21) {
            // 版本兼容
            buttonInterval = -BitmapUtils.dp2px(16);
        }*/cover = attr.getDrawable(R.styleable.FloatingMusicMenu_fmm_cover)
        backgroundTint = attr.getColorStateList(R.styleable.FloatingMusicMenu_fmm_backgroundTint)
        floatingDirection = attr.getInteger(R.styleable.FloatingMusicMenu_fmm_floating_direction, 0)
        attr.recycle()
        createRootButton(context)
        addScrollAnimation()
    }

    private fun addScrollAnimation() {
        showAnimation = AnimatorSet().setDuration(ANIMATION_DURATION.toLong())
        showAnimation?.play(ObjectAnimator.ofFloat(this, ALPHA, 0f, 1f))
        showAnimation?.setInterpolator(alphaExpandInterpolator)
        showAnimation?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                visibility = VISIBLE
            }

            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                visibility = VISIBLE
            }
        })
        hideAnimation = AnimatorSet().setDuration(ANIMATION_DURATION.toLong())
        hideAnimation?.play(ObjectAnimator.ofFloat(this, ALPHA, 1f, 0f))
        hideAnimation?.interpolator = alphaExpandInterpolator
        hideAnimation?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                visibility = INVISIBLE
            }
        })
    }

    private fun createRootButton(context: Context) {
        floatingMusicButton = FloatingMusicButton(context)
        floatingMusicButton!!.setOnClickListener { toggle() }
        floatingMusicButton!!.config(progressWidthPercent, progressColor, backgroundTint)
        floatingMusicButton!!.setProgress(progress)
        if (cover != null) {
            floatingMusicButton!!.setCoverDrawable(cover)
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        addView(floatingMusicButton, super.generateDefaultLayoutParams())
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        when (floatingDirection) {
            FLOATING_DIRECTION_UP, FLOATING_DIRECTION_DOWN -> onMeasureVerticalDirection()
            FLOATING_DIRECTION_LEFT, FLOATING_DIRECTION_RIGHT -> onMeasureHorizontalDirection()
        }
    }

    /**
     * 计算竖向排列时需要的大小
     */
    private fun onMeasureVerticalDirection() {
        var width = 0
        var height = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue
            width = Math.max(child.measuredWidth, width)
            height += child.measuredHeight
        }
        width += SHADOW_OFFSET * 2
        height += SHADOW_OFFSET * 2
        height += (buttonInterval * (childCount - 1)).toInt()
        height = adjustShootLength(height)
        setMeasuredDimension(width, height)
    }

    /**
     * 计算横向排列时需要的大小
     */
    private fun onMeasureHorizontalDirection() {
        var width = 0
        var height = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue
            height = Math.max(child.measuredHeight, height)
            width += child.measuredWidth
        }
        width += SHADOW_OFFSET * 2
        height += SHADOW_OFFSET * 2
        width += (buttonInterval * (childCount - 1)).toInt()
        width = adjustShootLength(width)
        setMeasuredDimension(width, height)
    }

    private fun adjustShootLength(length: Int): Int {
        return length * 12 / 10
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        when (floatingDirection) {
            FLOATING_DIRECTION_UP -> onUpDirectionLayout(l, t, r, b)
            FLOATING_DIRECTION_DOWN -> onDownDirectionLayout(l, t, r, b)
            FLOATING_DIRECTION_LEFT -> onLeftDirectionLayout(l, t, r, b)
            FLOATING_DIRECTION_RIGHT -> onRightDirectionLayout(l, t, r, b)
        }
    }

    /**
     * 摆放朝上展开方向的子控件位置
     */
    private fun onUpDirectionLayout(l: Int, t: Int, r: Int, b: Int) {
        val centerX = (r - l) / 2
        var offsetY = b - t - SHADOW_OFFSET
        for (i in childCount - 1 downTo 0) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue
            val width = child.measuredWidth
            val height = child.measuredHeight
            child.layout(centerX - width / 2, offsetY - height, centerX + width / 2, offsetY)

            //排除根按钮，添加动画
            if (i != childCount - 1) {
                val collapsedTranslation = (b - t - SHADOW_OFFSET - offsetY).toFloat()
                val expandedTranslation = 0f
                child.translationY = if (isExpanded) expandedTranslation else collapsedTranslation
                child.alpha = if (isExpanded) 1f else 0f
                val params = child.layoutParams as MenuLayoutParams
                params.collapseDirAnim.setFloatValues(expandedTranslation, collapsedTranslation)
                params.expandDirAnim.setFloatValues(collapsedTranslation, expandedTranslation)
                params.collapseDirAnim.setProperty(TRANSLATION_Y)
                params.expandDirAnim.setProperty(TRANSLATION_Y)
                params.setAnimationsTarget(child)
            }
            offsetY -= (height + buttonInterval).toInt()
        }
    }

    /**
     * 摆放朝下展开方向的子控件位置
     */
    private fun onDownDirectionLayout(l: Int, t: Int, r: Int, b: Int) {
        val centerX = (r - l) / 2
        var offsetY = SHADOW_OFFSET
        val rootView = getChildAt(childCount - 1)
        rootView.layout(
            centerX - rootView.measuredWidth / 2,
            offsetY,
            centerX + rootView.measuredWidth / 2,
            offsetY + rootView.measuredHeight
        )
        offsetY += (rootView.measuredHeight + buttonInterval).toInt()
        for (i in 0 until childCount - 1) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue
            val width = child.measuredWidth
            val height = child.measuredHeight
            child.layout(centerX - width / 2, offsetY, centerX + width / 2, offsetY + height)
            val collapsedTranslation = -offsetY.toFloat()
            val expandedTranslation = 0f
            child.translationY = if (isExpanded) expandedTranslation else collapsedTranslation
            child.alpha = if (isExpanded) 1f else 0f
            val params = child.layoutParams as MenuLayoutParams
            params.collapseDirAnim.setFloatValues(expandedTranslation, collapsedTranslation)
            params.expandDirAnim.setFloatValues(collapsedTranslation, expandedTranslation)
            params.collapseDirAnim.setProperty(TRANSLATION_Y)
            params.expandDirAnim.setProperty(TRANSLATION_Y)
            params.setAnimationsTarget(child)
            offsetY += (height + buttonInterval).toInt()
        }
    }

    /**
     * 摆放朝左展开方向的子控件位置
     */
    private fun onLeftDirectionLayout(l: Int, t: Int, r: Int, b: Int) {
        val centerY = (b - t) / 2
        var offsetX = r - l - SHADOW_OFFSET
        for (i in childCount - 1 downTo 0) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue
            val width = child.measuredWidth
            val height = child.measuredHeight
            child.layout(offsetX - width, centerY - height / 2, offsetX, centerY + height / 2)

            //排除根按钮，添加动画
            if (i != childCount - 1) {
                val collapsedTranslation = (r - l - SHADOW_OFFSET - offsetX).toFloat()
                val expandedTranslation = 0f
                child.translationX = if (isExpanded) expandedTranslation else collapsedTranslation
                child.alpha = if (isExpanded) 1f else 0f
                val params = child.layoutParams as MenuLayoutParams
                params.collapseDirAnim.setFloatValues(expandedTranslation, collapsedTranslation)
                params.expandDirAnim.setFloatValues(collapsedTranslation, expandedTranslation)
                params.collapseDirAnim.setProperty(TRANSLATION_X)
                params.expandDirAnim.setProperty(TRANSLATION_X)
                params.setAnimationsTarget(child)
            }
            offsetX -= (width + buttonInterval).toInt()
        }
    }

    /**
     * 摆放朝右展开方向的子控件位置
     */
    private fun onRightDirectionLayout(l: Int, t: Int, r: Int, b: Int) {
        val centerY = (b - t) / 2
        var offsetX = SHADOW_OFFSET
        val rootView = getChildAt(childCount - 1)
        rootView.layout(
            offsetX,
            centerY - rootView.measuredHeight / 2,
            offsetX + rootView.measuredWidth,
            centerY + rootView.measuredHeight / 2
        )
        offsetX += (rootView.measuredWidth + buttonInterval).toInt()
        for (i in 0 until childCount - 1) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue
            val width = child.measuredWidth
            val height = child.measuredHeight
            child.layout(offsetX, centerY - height / 2, offsetX + width, centerY + height / 2)
            val collapsedTranslation = -offsetX.toFloat()
            val expandedTranslation = 0f
            child.translationX = if (isExpanded) expandedTranslation else collapsedTranslation
            child.alpha = if (isExpanded) 1f else 0f
            val params = child.layoutParams as MenuLayoutParams
            params.collapseDirAnim.setFloatValues(expandedTranslation, collapsedTranslation)
            params.expandDirAnim.setFloatValues(collapsedTranslation, expandedTranslation)
            params.collapseDirAnim.setProperty(TRANSLATION_X)
            params.expandDirAnim.setProperty(TRANSLATION_X)
            params.setAnimationsTarget(child)
            offsetX += (width + buttonInterval).toInt()
        }
    }

    fun setButtonInterval(buttonInterval: Float) {
        this.buttonInterval = buttonInterval
        requestLayout()
    }

    fun addButton(button: FloatingActionButton?) {
        addView(button, 0)
        requestLayout()
    }

    fun addButtonAtLast(button: FloatingActionButton?) {
        addView(button, childCount - 1)
        requestLayout()
    }

    fun removeButton(button: FloatingActionButton?) {
        removeView(button)
        requestLayout()
    }

    fun setMusicCover(drawable: Drawable?) {
        floatingMusicButton!!.setCoverDrawable(drawable)
    }

    fun setMusicCover(bitmap: Bitmap?) {
        floatingMusicButton!!.setCover(bitmap)
    }

    fun setProgress(progress: Float) {
        if (floatingMusicButton != null) {
            floatingMusicButton!!.setProgress(progress)
        }
    }

    fun start() {
        floatingMusicButton!!.rotate(true)
    }

    fun stop() {
        floatingMusicButton!!.rotate(false)
    }

    fun setFloatingDirection(floatingDirection: Int) {
        this.floatingDirection = floatingDirection
        postInvalidate()
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return MenuLayoutParams(super.generateDefaultLayoutParams())
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return MenuLayoutParams(super.generateLayoutParams(attrs))
    }

    override fun generateLayoutParams(p: LayoutParams): LayoutParams {
        return MenuLayoutParams(super.generateLayoutParams(p))
    }

    private val mExpandAnimation = AnimatorSet().setDuration(ANIMATION_DURATION.toLong())
    private val mCollapseAnimation = AnimatorSet().setDuration(ANIMATION_DURATION.toLong())

    private inner class MenuLayoutParams(source: LayoutParams?) :
        LayoutParams(source) {
        var expandDirAnim = ObjectAnimator()
        var expandAlphaAnim = ObjectAnimator()
        var collapseDirAnim = ObjectAnimator()
        var collapseAlphaAnim = ObjectAnimator()
        var animationsSetToPlay = false

        init {
            expandDirAnim.interpolator = expandInterpolator
            expandAlphaAnim.interpolator = alphaExpandInterpolator
            collapseDirAnim.interpolator = collapseInterpolator
            collapseAlphaAnim.interpolator = collapseInterpolator
            collapseAlphaAnim.setProperty(ALPHA)
            collapseAlphaAnim.setFloatValues(1f, 0f)
            expandAlphaAnim.setProperty(ALPHA)
            expandAlphaAnim.setFloatValues(0f, 1f)
        }

        fun setAnimationsTarget(view: View) {
            collapseAlphaAnim.target = view
            collapseDirAnim.target = view
            expandDirAnim.target = view
            expandAlphaAnim.target = view

            // Now that the animations have targets, set them to be played
            if (!animationsSetToPlay) {
                addLayerTypeListener(expandDirAnim, view)
                addLayerTypeListener(collapseDirAnim, view)
                mCollapseAnimation.play(collapseAlphaAnim)
                mCollapseAnimation.play(collapseDirAnim)
                mExpandAnimation.play(expandAlphaAnim)
                mExpandAnimation.play(expandDirAnim)
                animationsSetToPlay = true
            }
        }

        private fun addLayerTypeListener(animator: Animator, view: View) {
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.setLayerType(LAYER_TYPE_NONE, null)
                }

                override fun onAnimationStart(animation: Animator) {
                    view.setLayerType(LAYER_TYPE_HARDWARE, null)
                }
            })
        }
    }

    fun collapse() {
        collapse(false)
    }

    fun collapseImmediately() {
        collapse(true)
    }

    private fun collapse(immediately: Boolean) {
        if (isExpanded) {
            isExpanded = false
            mCollapseAnimation.duration = if (immediately) 0 else ANIMATION_DURATION.toLong()
            mCollapseAnimation.start()
            mExpandAnimation.cancel()
        }
    }

    fun toggle() {
        if (isExpanded) {
            collapse()
        } else {
            expand()
        }
    }

    fun expand() {
        if (!isExpanded) {
            isExpanded = true
            mCollapseAnimation.cancel()
            mExpandAnimation.start()
        }
    }

    fun hide() {
        if (!isHided) {
            isHided = true
            hideAnimation!!.start()
            showAnimation!!.cancel()
        }
    }

    fun show() {
        if (isHided) {
            isHided = false
            showAnimation!!.start()
            hideAnimation!!.cancel()
        }
    }

    fun dp2px(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp,
            resources.displayMetrics
        )
    }

    /**
     *
     *
     * 上拉隐藏，下拉显示的动作行为，配合 [FloatingMusicMenu] 使用更佳
     *
     */
    class Behavior : CoordinatorLayout.Behavior<FloatingMusicMenu> {
        constructor() : super() {}
        constructor(context: Context?, attributeSet: AttributeSet?) : super(
            context,
            attributeSet
        ) {
        }

        override fun onTouchEvent(
            parent: CoordinatorLayout,
            child: FloatingMusicMenu,
            ev: MotionEvent
        ): Boolean {
            return super.onTouchEvent(parent, child, ev)
        }

        override fun onNestedScroll(
            coordinatorLayout: CoordinatorLayout,
            child: FloatingMusicMenu,
            target: View,
            dxConsumed: Int,
            dyConsumed: Int,
            dxUnconsumed: Int,
            dyUnconsumed: Int
        ) {
            super.onNestedScroll(
                coordinatorLayout,
                child,
                target,
                dxConsumed,
                dyConsumed,
                dxUnconsumed,
                dyUnconsumed
            )
            if (dyConsumed > 30 && child.visibility == VISIBLE) {
                child.hide()
            } else if (dyConsumed < -30 && child.visibility == INVISIBLE) {
                child.show()
            }
        }

        override fun onStartNestedScroll(
            coordinatorLayout: CoordinatorLayout,
            child: FloatingMusicMenu,
            directTargetChild: View,
            target: View,
            nestedScrollAxes: Int
        ): Boolean {
            return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL
        }
    }

    companion object {
        const val FLOATING_DIRECTION_UP = 0
        const val FLOATING_DIRECTION_LEFT = 1
        const val FLOATING_DIRECTION_DOWN = 2
        const val FLOATING_DIRECTION_RIGHT = 3
        private const val SHADOW_OFFSET = 20
        private const val ANIMATION_DURATION = 300
        private const val COLLAPSED_PLUS_ROTATION = 0f
        private const val EXPANDED_PLUS_ROTATION = 90f + 45f
        private val expandInterpolator: Interpolator = OvershootInterpolator()
        private val collapseInterpolator: Interpolator = DecelerateInterpolator(3f)
        private val alphaExpandInterpolator: Interpolator = DecelerateInterpolator()
    }
}