package com.jchen.camera.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.FileUtils
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*


object BitmapUtil {

    private fun toByteArray(bitmap: Bitmap): ByteArray {
        val os = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
        return os.toByteArray()
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    suspend fun savePic(
        context: Context,
        data: ByteArray?,
        isMirror: Boolean = false,
        onSuccess: (savePath: String, time: String) -> Unit,
        onFailed: (msg: String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val temp = System.currentTimeMillis()
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                val fileName = "IMG_$timeStamp.jpg"
                val picFile = FileUtil.createCameraFile(context, "camera2", fileName)
                if (picFile != null && data != null) {
                    val rawBitMap = BitmapFactory.decodeByteArray(data, 0, data.size)
                    val resultBitmap = if (isMirror) mirror(rawBitMap) else rawBitMap

                    ////Okio Sink和Source Okio.sink(picFile)已经弃用 ，改用file的拓展方法
                    //sink 类似OutputStream， Source类似InputStream
                    picFile.sink().buffer().write(toByteArray(resultBitmap)).close()

                    onSuccess(picFile.absolutePath, "${System.currentTimeMillis() - temp}")

                    Log.i(
                        "BitmapUtil",
                        "savePic! time：${System.currentTimeMillis() - temp}    path：  ${picFile.absolutePath}"
                    )
                    addMediaStore(context, picFile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onFailed("${e.message}")
            }

        }
    }

    private fun mirror(raw: Bitmap): Bitmap {
        val matrix = Matrix()
        // x坐标-1 镜像
        matrix.postScale(-1f, 1f)//https://blog.csdn.net/maxchenfuhai/article/details/51690857
        return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
    }


    private fun addMediaStore(
        context: Context,
        targetFile: File
    ) {
        val resolver = context.contentResolver
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, targetFile.name)
        contentValues.put(MediaStore.Images.Media.TITLE, targetFile.name)
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        val uri: Uri =
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return
        val out: OutputStream = resolver.openOutputStream(uri) ?: return
        val fis = FileInputStream(targetFile)
        FileUtils.copy(fis, out)
        fis.close()
        out.close()
    }
}