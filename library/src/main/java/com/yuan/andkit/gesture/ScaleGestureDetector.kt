package com.yuan.andkit.gesture

import android.content.Context
import android.os.Build
import android.os.Handler
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.yuan.andkit.R
import kotlin.math.abs
import kotlin.math.hypot

/**
 *copy from android.view.ScaleGestureDetector
 * Created by wangpeiyuan on 2021/6/22.
 */
class ScaleGestureDetector constructor(
    context: Context,
    listener: OnScaleGestureListener,
    handler: Handler? = null
) {

    companion object {
        private const val TAG = "ScaleGestureDetector"
        private const val SCALE_FACTOR = .5f
        private const val ANCHORED_SCALE_MODE_NONE = 0
        private const val ANCHORED_SCALE_MODE_DOUBLE_TAP = 1
        private const val ANCHORED_SCALE_MODE_STYLUS = 2
    }

    private val mContext: Context = context
    private val mListener: OnScaleGestureListener = listener
    private val mHandler: Handler? = handler

    private var mFocusX = 0f
    private var mFocusY = 0f
    private var mQuickScaleEnabled = false
    private var mStylusScaleEnabled = false
    private var mCurrSpan = 0f
    private var mPrevSpan = 0f
    private var mInitialSpan = 0f
    private var mCurrSpanX = 0f
    private var mCurrSpanY = 0f
    private var mPrevSpanX = 0f
    private var mPrevSpanY = 0f
    private var mCurrTime: Long = 0
    private var mPrevTime: Long = 0
    private var mInProgress = false
    private var mSpanSlop = 0
    private var mMinSpan = 0
    private var mAnchoredScaleStartX = 0f
    private var mAnchoredScaleStartY = 0f
    private var mAnchoredScaleMode: Int = ANCHORED_SCALE_MODE_NONE

    private var mGestureDetector: GestureDetector? = null

    private var mEventBeforeOrAboveStartingGestureEvent = false

    init {
        val viewConfiguration = ViewConfiguration.get(context)
        mSpanSlop = viewConfiguration.scaledTouchSlop * 2
        mMinSpan = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            viewConfiguration.scaledMinimumScalingSpan
        } else {
            context.resources.getDimensionPixelSize(R.dimen.config_minScalingSpan)
        }
        // Quick scale is enabled by default after JB_MR2
        val targetSdkVersion = context.applicationInfo.targetSdkVersion
        if (targetSdkVersion > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            setQuickScaleEnabled(true)
        }
        // Stylus scale is enabled by default after LOLLIPOP_MR1
        // Stylus scale is enabled by default after LOLLIPOP_MR1
        if (targetSdkVersion > Build.VERSION_CODES.LOLLIPOP_MR1) {
            setStylusScaleEnabled(true)
        }
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        mCurrTime = event.eventTime
        val action = event.actionMasked

        // Forward the event to check for double tap gesture
        if (mQuickScaleEnabled) {
            mGestureDetector?.onTouchEvent(event)
        }
        val count = event.pointerCount
        val isStylusButtonDown = event.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY != 0

        val anchoredScaleCancelled =
            mAnchoredScaleMode == ANCHORED_SCALE_MODE_STYLUS && !isStylusButtonDown
        val streamComplete =
            action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || anchoredScaleCancelled

        if (action == MotionEvent.ACTION_DOWN || streamComplete) {
            // Reset any scale in progress with the listener.
            // If it's an ACTION_DOWN we're beginning a new event stream.
            // This means the app probably didn't give us all the events. Shame on it.
            if (mInProgress) {
                mListener.onScaleEnd(this)
                mInProgress = false
                mInitialSpan = 0f
                mAnchoredScaleMode = ANCHORED_SCALE_MODE_NONE
            } else if (inAnchoredScaleMode() && streamComplete) {
                mInProgress = false
                mInitialSpan = 0f
                mAnchoredScaleMode = ANCHORED_SCALE_MODE_NONE
            }
            if (streamComplete) {
                return true
            }
        }

        if (!mInProgress && mStylusScaleEnabled && !inAnchoredScaleMode()
            && !streamComplete && isStylusButtonDown
        ) {
            // Start of a button scale gesture
            mAnchoredScaleStartX = event.x
            mAnchoredScaleStartY = event.y
            mAnchoredScaleMode = ScaleGestureDetector.ANCHORED_SCALE_MODE_STYLUS
            mInitialSpan = 0f
        }

        val configChanged = action == MotionEvent.ACTION_DOWN ||
                action == MotionEvent.ACTION_POINTER_UP ||
                action == MotionEvent.ACTION_POINTER_DOWN || anchoredScaleCancelled

        val pointerUp = action == MotionEvent.ACTION_POINTER_UP
        val skipIndex = if (pointerUp) event.actionIndex else -1

        // Determine focal point
        var sumX = 0f
        var sumY = 0f
        val div = if (pointerUp) count - 1 else count
        val focusX: Float
        val focusY: Float
        if (inAnchoredScaleMode()) {
            // In anchored scale mode, the focal pt is always where the double tap
            // or button down gesture started
            focusX = mAnchoredScaleStartX
            focusY = mAnchoredScaleStartY
            mEventBeforeOrAboveStartingGestureEvent = event.y < focusY
        } else {
            for (i in 0 until count) {
                if (skipIndex == i) continue
                sumX += event.getX(i)
                sumY += event.getY(i)
            }
            focusX = sumX / div
            focusY = sumY / div
        }

        // Determine average deviation from focal point
        var devSumX = 0f
        var devSumY = 0f
        for (i in 0 until count) {
            if (skipIndex == i) continue

            // Convert the resulting diameter into a radius.
            devSumX += abs(event.getX(i) - focusX)
            devSumY += abs(event.getY(i) - focusY)
        }
        val devX = devSumX / div
        val devY = devSumY / div

        // Span is the average distance between touch points through the focal point;
        // i.e. the diameter of the circle with a radius of the average deviation from
        // the focal point.
        val spanX = devX * 2
        val spanY = devY * 2
        val span: Float = if (inAnchoredScaleMode()) {
            spanY
        } else {
            hypot(spanX.toDouble(), spanY.toDouble()).toFloat()
        }

        // Dispatch begin/end events as needed.
        // If the configuration changes, notify the app to reset its current state by beginning
        // a fresh scale event stream.
        val wasInProgress = mInProgress
        mFocusX = focusX
        mFocusY = focusY
        if (!inAnchoredScaleMode() && mInProgress && (span < mMinSpan || configChanged)) {
            mListener.onScaleEnd(this)
            mInProgress = false
            mInitialSpan = span
        }
        if (configChanged) {
            mPrevSpanX = spanX
            mCurrSpanX = spanX
            mPrevSpanY = spanY
            mCurrSpanY = spanY
            mInitialSpan = span
            mPrevSpan = span
            mCurrSpan = span
        }

        val minSpan = if (inAnchoredScaleMode()) mSpanSlop else mMinSpan
        if (!mInProgress && span >= minSpan &&
            (wasInProgress || abs(span - mInitialSpan) > mSpanSlop)
        ) {
            mPrevSpanX = spanX
            mCurrSpanX = spanX
            mPrevSpanY = spanY
            mCurrSpanY = spanY
            mPrevSpan = span
            mCurrSpan = span
            mPrevTime = mCurrTime
            mInProgress = mListener.onScaleBegin(this)
        }

        // Handle motion; focal point and span/scale factor are changing.
        if (action == MotionEvent.ACTION_MOVE) {
            mCurrSpanX = spanX
            mCurrSpanY = spanY
            mCurrSpan = span

            var updatePrev = true

            if (mInProgress) {
                updatePrev = mListener.onScale(this)
            }

            if (updatePrev) {
                mPrevSpanX = mCurrSpanX
                mPrevSpanY = mCurrSpanY
                mPrevSpan = mCurrSpan
                mPrevTime = mCurrTime
            }
        }
        return true
    }

    private fun inAnchoredScaleMode(): Boolean = mAnchoredScaleMode != ANCHORED_SCALE_MODE_NONE

    fun setQuickScaleEnabled(scales: Boolean) {
        mQuickScaleEnabled = scales
        if (mQuickScaleEnabled && mGestureDetector == null) {
            val gestureListener: GestureDetector.SimpleOnGestureListener =
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        // Double tap: start watching for a swipe
                        mAnchoredScaleStartX = e.x
                        mAnchoredScaleStartY = e.y
                        mAnchoredScaleMode = ANCHORED_SCALE_MODE_DOUBLE_TAP
                        return true
                    }
                }
            mGestureDetector = GestureDetector(mContext, gestureListener, mHandler)
        }
    }

    fun isQuickScaleEnabled(): Boolean = mQuickScaleEnabled

    fun setStylusScaleEnabled(scales: Boolean) {
        mStylusScaleEnabled = scales
    }

    fun isStylusScaleEnabled(): Boolean = mStylusScaleEnabled

    fun isInProgress(): Boolean = mInProgress

    fun getFocusX(): Float = mFocusX

    fun getFocusY(): Float = mFocusY

    fun getCurrentSpan(): Float = mCurrSpan

    fun getCurrentSpanX(): Float = mCurrSpanX

    fun getCurrentSpanY(): Float = mCurrSpanY

    fun getPreviousSpan(): Float = mPrevSpan

    fun getPreviousSpanX(): Float = mPrevSpanX

    fun getPreviousSpanY(): Float = mPrevSpanY

    fun getScaleFactor(): Float {
        if (inAnchoredScaleMode()) {
            // Drag is moving up; the further away from the gesture
            // start, the smaller the span should be, the closer,
            // the larger the span, and therefore the larger the scale
            val scaleUp = mEventBeforeOrAboveStartingGestureEvent && mCurrSpan < mPrevSpan ||
                    !mEventBeforeOrAboveStartingGestureEvent && mCurrSpan > mPrevSpan
            val spanDiff: Float = abs(1 - mCurrSpan / mPrevSpan) * SCALE_FACTOR
            return if (mPrevSpan <= mSpanSlop) 1f else if (scaleUp) 1 + spanDiff else 1 - spanDiff
        }
        return if (mPrevSpan > 0) mCurrSpan / mPrevSpan else 1f
    }

    fun getTimeDelta(): Long = mCurrTime - mPrevTime

    fun getEventTime(): Long = mCurrTime

    interface OnScaleGestureListener {
        fun onScale(detector: ScaleGestureDetector?): Boolean = false
        fun onScaleBegin(detector: ScaleGestureDetector?): Boolean = true
        fun onScaleEnd(detector: ScaleGestureDetector?) = Unit
    }
}