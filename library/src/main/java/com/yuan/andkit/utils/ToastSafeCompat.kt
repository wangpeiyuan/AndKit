package com.yuan.andkit.utils

import android.content.Context
import android.view.Gravity
import android.widget.Toast
import androidx.annotation.StringRes
import me.drakeet.support.toast.ToastCompat
import java.lang.ref.WeakReference

class ToastSafeCompat private constructor() {

    companion object {
        val INSTANCE: ToastSafeCompat by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            ToastSafeCompat()
        }
    }

    private var weakReference: WeakReference<ToastCompat>? = null

    fun showMsg(
        context: Context,
        @StringRes resId: Int,
        duration: Int = Toast.LENGTH_SHORT,
        gravity: Int = Gravity.CENTER
    ) {
        this.showMsg(context, context.resources.getText(resId), duration, gravity)
    }

    fun showMsg(
        context: Context,
        text: CharSequence,
        duration: Int = Toast.LENGTH_SHORT,
        gravity: Int = Gravity.CENTER
    ) {
        weakReference?.let {
            it.get()?.baseToast?.cancel()
            it.clear()
        }

        val toastCompat = makeToast(context, text, duration)
        toastCompat.setGravity(gravity, 0, 0)
        toastCompat.show()
    }

    private fun makeToast(context: Context, text: CharSequence, duration: Int): ToastCompat {
        //使用 ToastCompat 防止系统7.1崩溃
        val toastCompat = ToastCompat.makeText(context.applicationContext, text, duration)
        weakReference = WeakReference(toastCompat)
        return toastCompat
    }
}