package com.yuan.andkit

import androidx.fragment.app.Fragment

/**
 *
 * Created by wangpeiyuan on 2021/5/29.
 */
fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}