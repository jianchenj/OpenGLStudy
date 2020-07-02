package com.jchen.camera.util

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

object FileUtil {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun createCameraFile(context: Context, folderName: String): File? {
        return try {
            val rootFile = File("${context.externalMediaDirs}  ${File.separator}  $folderName")
            Log.i("FileUtil", "createCameraFile rootFile =  $rootFile")
            if (!rootFile.exists()) {
                rootFile.mkdirs()
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val fileName = "IMG_$timeStamp.jpg"
            File("$rootFile.absolutePath${File.separator}$fileName")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}