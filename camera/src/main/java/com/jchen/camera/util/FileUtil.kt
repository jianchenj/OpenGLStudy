package com.jchen.camera.util

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File

object FileUtil {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun createCameraFile(context: Context, folderName: String, fileName : String): File? {
        return try {
            val rootFile =
                File("${context.getExternalFilesDir(Environment.DIRECTORY_DCIM)}${File.separator}$folderName")

            if (!rootFile.exists()) {
                rootFile.mkdirs()
            }
            Log.i("FileUtil", "${rootFile.absolutePath}${File.separator}$fileName")
            File("${rootFile.absolutePath}${File.separator}$fileName")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}