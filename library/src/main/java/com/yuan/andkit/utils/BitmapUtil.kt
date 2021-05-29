package com.yuan.andkit.utils

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 *
 * Created by wangpeiyuan on 2021/5/29.
 */
object BitmapUtil {

    fun resizeBitmap(imagePath: String, reqWidth: Int, reqHeight: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(imagePath, options)
    }

    fun resizeBitmap(res: Resources, id: Int, reqWidth: Int, reqHeight: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, id, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeResource(res, id, options)
    }

    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val width = options.outWidth
        val height = options.outHeight
        var inSampleSize = 1
        if (height > reqHeight || width > reqHeight) {
            val halfWidth = width / 2
            val haleHeight = height / 2
            while ((halfWidth / inSampleSize) >= reqWidth && (haleHeight / reqHeight) >= reqHeight) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}