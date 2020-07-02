package com.jchen.camera.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BitmapUtil {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    suspend fun savePic(
        context: Context,
        data: ByteArray?,
        isMirror: Boolean = false,
        onSuccess: (savePath: String, time: String) -> Unit,
        onFailed: (msg: String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val temp = System.currentTimeMillis()
            val picFile = FileUtil.createCameraFile(context, "camera2")
            if (picFile != null && data != null) {
                val rawBitMap = BitmapFactory.decodeByteArray(data, 0, data.size)
                val resultBitmap = if (isMirror) mirror(rawBitMap) else rawBitMap
                // TODO: 2020/7/2  
            }
        }
    }

    private fun mirror(raw: Bitmap): Bitmap {
        // TODO: 2020/7/2 www
    }
}