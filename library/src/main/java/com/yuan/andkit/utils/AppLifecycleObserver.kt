package com.yuan.andkit.utils

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * App 生命周期监听
 * Created by wangpeiyuan on 2021/5/13.
 */
class AppLifecycleObserver private constructor() : LifecycleObserver {

    companion object {
        const val TAG = "AppLifecycleObserver"

        val instance: AppLifecycleObserver by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            AppLifecycleObserver()
        }

        fun register() {
            ProcessLifecycleOwner.get().lifecycle.addObserver(instance)
        }
    }

    private val listenerList = ArrayList<OnAppLifecycleListener>()

    /**
     * 在应用程序的整个生命周期中只会被调用一次
     * */
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        Log.d(TAG, "onCreate: ")
    }

    /**
     * 应用程序出现到前台时调用
     * */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        Log.d(TAG, "onStart: ")
        for (listener in listenerList) {
            listener.appInForeground()
        }
    }

    /**
     * 应用程序出现到前台时调用
     * */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        Log.d(TAG, "onResume: ")
    }

    /**
     * 应用程序退出到后台时调用
     * */
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        Log.d(TAG, "onPause: ")
    }

    /**
     * 应用程序退出到后台时调用
     * */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        Log.d(TAG, "onStop: ")
        for (listener in listenerList) {
            listener.appInBackground()
        }
    }

    fun addOnAppLifecycleListener(listener: OnAppLifecycleListener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener)
        }
    }

    fun removeOnAppLifecycleListener(listener: OnAppLifecycleListener) {
        if (listenerList.contains(listener)) {
            listenerList.remove(listener)
        }
    }

    fun clearOnAppLifecycleListener() {
        listenerList.clear()
    }

    interface OnAppLifecycleListener {
        fun appInBackground()
        fun appInForeground()
    }
}