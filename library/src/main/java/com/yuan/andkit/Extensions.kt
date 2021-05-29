package com.yuan.andkit

import android.content.res.Resources
import android.graphics.RectF
import android.util.TypedValue
import androidx.core.graphics.ColorUtils

/**
 * Created by wangpeiyuan on 2021/4/8.
 */
val Float.dp
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        Resources.getSystem().displayMetrics
    )

val Int.dp
    get() = this.toFloat().dp.toInt()

val Int.parseColorToString: String
    get() = String.format("#%06X", 0xFFFFFF and this)

val Int.isDarkColor: Boolean
    get() = ColorUtils.calculateLuminance(this) < 0.5

fun RectF.scale(factor: Float) {
    left *= factor
    top *= factor
    right *= factor
    bottom *= factor
}