package com.yuan.andkit

import kotlinx.coroutines.*

/**
 *
 * Created by wangpeiyuan on 2021/6/14.
 */
fun startCoroutineTimer(
    delayMillis: Long = 0,
    repeatMillis: Long = 0,
    runActionOnUiThread: Boolean = false,
    action: () -> Unit
) = GlobalScope.launch {
    delay(delayMillis)
    if (repeatMillis > 0) {
        while (true) {
            if (runActionOnUiThread) {
                launchOnUiThread { action() }
            } else {
                action()
            }
            delay(repeatMillis)
        }
    } else {
        if (runActionOnUiThread) {
            launchOnUiThread { action() }
        } else {
            action()
        }
    }
}

fun launchOnUiThread(action: suspend CoroutineScope.() -> Unit) =
    GlobalScope.launch(Dispatchers.Main, block = action)