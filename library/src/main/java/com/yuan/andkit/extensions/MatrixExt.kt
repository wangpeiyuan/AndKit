package com.yuan.andkit.extensions

import android.graphics.Matrix

/**
 *
 * Created by wangpeiyuan on 2021/6/17.
 */
fun Matrix.getMatrixValues(): FloatArray {
    val values = FloatArray(9)
    this.getValues(values)
    return values
}