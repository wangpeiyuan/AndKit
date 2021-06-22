package com.yuan.andkit.gesture

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import kotlin.math.abs

/**
 *
 * Created by wangpeiyuan on 2021/6/17.
 */
class GestureHelper private constructor(context: Context, onGestureListener: OnGestureListener?) {

    companion object {
        private const val SWIPE_THRESHOLD = 30

        fun create(context: Context, onGestureListener: OnGestureListener?): GestureHelper {
            return GestureHelper(context, onGestureListener)
        }
    }

    /**
     * 触摸，单击，双击，移动手势管理器
     */
    private val mGestureDetector: GestureDetector

    /**
     * 缩放手势管理器
     */
    private val mScaleGestureDetector: ScaleGestureDetector

    /**
     * 旋转手势管理器
     */
    private val mRotationGestureDetector: RotationGestureDetector

    private var mOnGestureListener: OnGestureListener? = null

    /**
     * 双指操作的中心点坐标
     */
    private var mMidPntX: Float = 0f

    /**
     * 双指操作的中心点坐标
     */
    private var mMidPntY: Float = 0f

    /**
     * 当前的手势是否已经放开
     * 注：增加 [.mIsUp] 和 [.mIsHandleGesture] 是为了处理单击事件和放开事件的时间差，保证处理顺序
     * Android 系统的"单击"触发顺序为：down -> up -> onSingleTapConfirmed
     * 增加两个参数将顺序改为：down -> onSingleTapConfirmed -> up
     */
    private var mIsUp = false

    /**
     * 当前的手势是否已经处理
     * 注：增加 [.mIsUp] 和 [.mIsHandleGesture] 是为了处理单击事件和放开事件的时间差，保证处理顺序
     * Android 系统的"单击"触发顺序为：down -> up -> onSingleTapConfirmed
     * 增加两个参数将顺序改为：down -> onSingleTapConfirmed -> up
     */
    private var mIsHandleGesture = false

    /**
     * 滑动的阈值，大于这个值才处理滑动事件，减少滑动的事件处理
     */
    private var mSwipeThreshold = SWIPE_THRESHOLD

    private var mIsMoveStatus = false

    init {
        mGestureDetector = GestureDetector(context, GestureListener(), null, true)
        mScaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        mRotationGestureDetector = RotationGestureDetector(RotateListener())

        mOnGestureListener = onGestureListener
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount > 1) {
            mMidPntX = (event.getX(0) + event.getX(1)) / 2
            mMidPntY = (event.getY(0) + event.getY(1)) / 2
        }
        mGestureDetector.onTouchEvent(event)
        if (mOnGestureListener?.isScaleEnabled() == true) {
            mScaleGestureDetector.onTouchEvent(event)
        }
        if (mOnGestureListener?.isRotateEnabled() == true) {
            mRotationGestureDetector.onTouchEvent(event)
        }
        if (event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_UP ||
            event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_CANCEL
        ) {
            mIsUp = true //设置为已放开手势
            mIsMoveStatus = false
            if (mOnGestureListener != null && mIsHandleGesture) { //如果手势已经处理直接调用 onUp，没有的话，暂时不调用
                mOnGestureListener?.onUp(event.x, event.y)
            }
        }
        return true
    }


    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            //重置
            mIsUp = false
            mIsHandleGesture = false
            mOnGestureListener?.onDown(e.x, e.y)
            return super.onDown(e)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            mOnGestureListener?.let {
                mIsHandleGesture = true
                it.onSingleTouch(e.x, e.y)
                //由于 onSingleTapConfirmed 的触发时机比 up 的时机来得晚，因此 up 的调用时间延迟到这里处理
                if (mIsUp) {
                    it.onUp(e.x, e.y)
                }
            }
            return super.onSingleTapConfirmed(e)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            mOnGestureListener?.let {
                mIsHandleGesture = true
                it.onDoubleTouch(e.x, e.y)
            }
            return super.onDoubleTap(e)
        }

        override fun onLongPress(e: MotionEvent) {
            mOnGestureListener?.let {
                mIsHandleGesture = true
                it.onLongPress(e)
            }
        }

        override fun onScroll(
            e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float
        ): Boolean {
            mOnGestureListener?.let {
                mIsHandleGesture = true
                if (e2.pointerCount > 1 && !mIsMoveStatus) {
                    it.onDoubleTranslate(e1, e2, distanceX, distanceY)
                } else {
                    mIsMoveStatus = true
                    val deltaX = e2.x - e1.x
                    val deltaY = e2.y - e1.y
                    if (abs(deltaX) > mSwipeThreshold || abs(deltaY) > mSwipeThreshold) {
                        it.onTranslate(-distanceX, -distanceY, e1, e2)
                    }
                }
            }
            return super.onScroll(e1, e2, distanceX, distanceY)
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (mOnGestureListener != null && !mIsMoveStatus) {
                mIsHandleGesture = true
                mOnGestureListener?.onScale(detector.scaleFactor, mMidPntX, mMidPntY)
            }
            return true
        }
    }

    private inner class RotateListener : RotationGestureDetector.OnRotationGestureListener {
        override fun onRotation(rotationDetector: RotationGestureDetector): Boolean {
            if (mOnGestureListener != null && !mIsMoveStatus) {
                mIsHandleGesture = true
                mOnGestureListener?.onRotate(rotationDetector.getAngle(), mMidPntX, mMidPntY)
            }
            return true
        }
    }
}

interface OnGestureListener {
    /**
     * 是否允许缩放
     * @return 默认允许
     */
    fun isScaleEnabled(): Boolean = true

    /**
     * 是否允许旋转
     * @return 默认允许
     */
    fun isRotateEnabled(): Boolean = true

    /**
     * 手势第一次触摸事件
     * @param px 触摸 x 坐标
     * @param py 触摸 y 坐标
     */
    fun onDown(px: Float, py: Float) = Unit

    /**
     * 手势放开
     * @param px 触摸 x 坐标
     * @param py 触摸 y 坐标
     */
    fun onUp(px: Float, py: Float) = Unit

    /**
     * 单击事件
     *
     * @param px 点击 x 坐标
     * @param py 点击 y 坐标
     */
    fun onSingleTouch(px: Float, py: Float) = Unit

    /**
     * 双击事件
     *
     * @param px 点击 x 坐标
     * @param py 点击 y 坐标
     */
    fun onDoubleTouch(px: Float, py: Float) = Unit

    fun onLongPress(e: MotionEvent) = Unit

    /**
     * 移动事件
     * @param deltaX x 轴移动的距离
     * @param deltaY y 轴移动的距离
     * @param e1     起始点
     * @param e2     移动到点
     */
    fun onTranslate(
        deltaX: Float, deltaY: Float, e1: MotionEvent, e2: MotionEvent
    ) = Unit

    fun onDoubleTranslate(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float) =
        Unit

    /**
     * 缩放事件
     * @param deltaScale 缩放比例
     * @param px         缩放中心点 x 坐标
     * @param py         缩放中心点 y 坐标
     */
    fun onScale(deltaScale: Float, px: Float, py: Float) = Unit

    /**
     * 旋转事件
     * @param deltaAngle 旋转角度
     * @param px         旋转中心点 x 坐标
     * @param py         旋转中心点 y 坐标
     */
    fun onRotate(deltaAngle: Float, px: Float, py: Float) = Unit
}