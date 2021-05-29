package com.yuan.andkit

import android.os.SystemClock
import android.view.View

class SafeClickListener(
    private val interval: Int = 500,
    private val onSafeClick: (View) -> Unit
) : View.OnClickListener {

    constructor(click: (View) -> Unit) : this(500, click)

    private var lastClickTime: Long = 0L

    override fun onClick(v: View) {
        if (SystemClock.elapsedRealtime() - lastClickTime < interval) {
            return
        }
        lastClickTime = SystemClock.elapsedRealtime()
        onSafeClick(v)
    }
}