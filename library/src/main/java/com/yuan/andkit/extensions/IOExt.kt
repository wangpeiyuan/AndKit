package com.yuan.andkit

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader

/**
 *
 * Created by wangpeiyuan on 2021/6/14.
 */
fun OutputStream?.closeStream() {
    try {
        this?.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

fun InputStream?.closeStream() {
    try {
        this?.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

fun Reader?.closeStream() {
    try {
        this?.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}