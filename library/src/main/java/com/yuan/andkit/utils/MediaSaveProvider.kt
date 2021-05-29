package com.yuan.andkit.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.text.TextUtils
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 *
 * Created by wangpeiyuan on 2021/5/29.
 */
object MediaSaveProvider {

    fun saveImageToGallery(context: Context, bitmap: Bitmap, imageName: String) {
        val contentResolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
            put(MediaStore.Images.Media.DESCRIPTION, imageName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val galleryFileUri = contentResolver.insert(collection, values)
        galleryFileUri?.let { uri ->
            contentResolver.openOutputStream(uri).use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it)
                it?.close()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
            }
        }

    }

    fun saveVideoToGallery(
        context: Context,
        videoPath: String,
        videoName: String = getVideoName()
    ) {
        val contentResolver = context.contentResolver
        val videoCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val contentValues = ContentValues().apply {
            //配置视频的显示名称
            put(MediaStore.Video.Media.TITLE, getFileName(videoName))
            put(MediaStore.Video.Media.DISPLAY_NAME, videoName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //配置视频的状态为：等待中...
                put(MediaStore.Video.Media.IS_PENDING, 1)
            } else {
                val path =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                if (!path.exists()) {
                    path.mkdirs()
                }
                val videoPath = if (path.absolutePath.endsWith(File.separator)) {
                    path.absolutePath + videoName
                } else {
                    path.absolutePath + File.separator + videoName
                }
                put(MediaStore.Video.Media.DATA, videoPath)
            }
        }

        //开始插入视频
        val videoUri = contentResolver.insert(videoCollection, contentValues)
        videoUri?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.openFileDescriptor(videoUri, "w", null)?.use {
                    val inParcelFileDescriptor =
                        ParcelFileDescriptor.open(
                            File(videoPath),
                            ParcelFileDescriptor.MODE_READ_WRITE
                        )
                    android.os.FileUtils.copy(
                        inParcelFileDescriptor.fileDescriptor,
                        it.fileDescriptor
                    )
                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    contentResolver.update(videoUri, contentValues, null, null)
                    it.close()
                    inParcelFileDescriptor.close()
                }
            } else {
                contentResolver.openOutputStream(videoUri, "rw")?.use {
                    val fileInputStream = FileInputStream(File(videoPath))
                    fileInputStream.copyTo(it)
                    fileInputStream.close()
                    it.close()
                }
            }
        }
    }

    private fun getFileName(name: String): String {
        if (TextUtils.isEmpty(name)) {
            return ""
        }
        val start = name.lastIndexOf(".")
        return if (start != -1) {
            name.substring(0, start)
        } else {
            ""
        }
    }

    private fun getVideoName(): String {
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.CHINA)
        return "VID_${sdf.format(Date())}.mp4"
    }
}