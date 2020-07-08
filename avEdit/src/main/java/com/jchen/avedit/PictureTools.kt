package com.jchen.avedit

import android.R
import android.R.attr.path
import android.graphics.*
import android.media.ExifInterface
import android.util.Log
import android.widget.ImageView
import java.io.File
import java.io.IOException
import kotlin.math.ceil


object PictureTools {
    const val DISPLAY_WIDTH = 400
    const val DISPLAY_HEIGHT = 400

    /**
     * 将图片转换成Bitmap
     * @param imageFilePath
     * @return
     */
    fun getBitMap(imageFilePath: String): Bitmap? {
        //加载图像的尺寸而不是图像本身
        val options =
            BitmapFactory.Options()
        options.inJustDecodeBounds = true
        var bitmap =
            BitmapFactory.decodeFile(imageFilePath, options)
        val widthRatio = ceil(
            (options.outWidth / DISPLAY_WIDTH.toFloat()).toDouble()
        ).toInt()
        val heightRatio = ceil(
            (options.outHeight / DISPLAY_HEIGHT.toFloat()).toDouble()
        ).toInt()

        Log.v("test0708", "imageFilePath $imageFilePath")
        Log.v("test0708", "" + heightRatio)
        Log.v("test0708", "" + widthRatio)

        //如果两个比例都大于1，那么图像的一条边将大于屏幕
        if (heightRatio > 1 && widthRatio > 1) {
            options.inSampleSize = Math.max(heightRatio, widthRatio)
        }

        //对它进行真正的解码
        options.inJustDecodeBounds = false // 此处为false，不只是解码
        bitmap = BitmapFactory.decodeFile(imageFilePath, options)
        //修复图片方向
        val m = repairBitmapDirection(imageFilePath)
        if (m != null) {
            bitmap = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                m,
                true
            )
        }
        return bitmap
    }

    /**
     * 识别图片方向
     * @param filepath
     * @return
     */
    private fun repairBitmapDirection(filepath: String): Matrix? {
        //根据图片的filepath获取到一个ExifInterface的对象
        var exif: ExifInterface? = null
        exif = try {
            ExifInterface(filepath)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
        var degree = 0
        if (exif != null) {
            // 读取图片中相机方向信息
            val ori = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            degree = when (ori) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }
        if (degree != 0) {
            // 旋转图片
            val m = Matrix()
            m.postRotate(degree.toFloat())
            return m
        }
        return null
    }

    /**
     * 通过BitmapShader绘制圆角边框
     * @param bitmap
     * @param outWidth
     * @param outHeight
     * @param radius
     * @param boarder
     * @return
     */
    fun getRoundBitmapByShader(
        bitmap: Bitmap?,
        outWidth: Int,
        outHeight: Int,
        radius: Int,
        boarder: Int
    ): Bitmap? {
        if (bitmap == null) {
            return null
        }
        val width = bitmap.width
        val height = bitmap.height
        val widthScale = outWidth * 1f / width
        val heightScale = outHeight * 1f / height
        val matrix = Matrix()
        matrix.setScale(widthScale, heightScale)
        //创建输出的bitmap
        val desBitmap = Bitmap.createBitmap(
            outWidth,
            outHeight,
            Bitmap.Config.ARGB_8888
        )
        //创建canvas并传入desBitmap，这样绘制的内容都会在desBitmap上
        val canvas = Canvas(desBitmap)
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG)
        //创建着色器
        val bitmapShader = BitmapShader(
            bitmap,
            Shader.TileMode.CLAMP,
            Shader.TileMode.CLAMP
        )
        //给着色器配置matrix
        bitmapShader.setLocalMatrix(matrix)
        paint.shader = bitmapShader
        //创建矩形区域并且预留出border
        val rect =
            RectF(
                boarder.toFloat(),
                boarder.toFloat(),
                (outWidth - boarder).toFloat(),
                (outHeight - boarder).toFloat()
            )
        //把传入的bitmap绘制到圆角矩形区域内
        canvas.drawRoundRect(rect, radius.toFloat(), radius.toFloat(), paint)
        if (boarder > 0) {
            //绘制boarder
            val boarderPaint =
                Paint(Paint.ANTI_ALIAS_FLAG)
            boarderPaint.color = Color.rgb(189, 189, 189)
            boarderPaint.style = Paint.Style.STROKE
            boarderPaint.strokeWidth = boarder.toFloat()
            canvas.drawRoundRect(rect, radius.toFloat(), radius.toFloat(), boarderPaint)
        }
        return desBitmap
    }

    /**
     * 通过BitmapShader绘制圆形边框
     * @param bitmap
     * @param outWidth
     * @param outHeight
     * @param boarder
     * @return
     */
    fun getCircleBitmapByShader(
        bitmap: Bitmap?,
        outWidth: Int,
        outHeight: Int,
        boarder: Int
    ): Bitmap? {
        if (bitmap == null) {
            return null
        }
        val width = bitmap.width
        val height = bitmap.height
        val widthScale = outWidth * 1f / width
        val heightScale = outHeight * 1f / height
        val matrix = Matrix()
        matrix.setScale(widthScale, heightScale)
        val desBitmap = Bitmap.createBitmap(
            outWidth,
            outHeight,
            Bitmap.Config.ARGB_8888
        )
        val radius: Int = if (outHeight > outWidth) {
            outWidth / 2
        } else {
            outHeight / 2
        }
        //创建canvas
        val canvas = Canvas(desBitmap)
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG)
        val bitmapShader = BitmapShader(
            bitmap,
            Shader.TileMode.CLAMP,
            Shader.TileMode.CLAMP
        )
        bitmapShader.setLocalMatrix(matrix)
        paint.shader = bitmapShader
        canvas.drawCircle(
            outWidth / 2.toFloat(),
            outHeight / 2.toFloat(),
            radius - boarder.toFloat(),
            paint
        )
        if (boarder > 0) {
            //绘制boarder
            val boarderPaint =
                Paint(Paint.ANTI_ALIAS_FLAG)
            boarderPaint.color = Color.GREEN
            boarderPaint.style = Paint.Style.STROKE
            boarderPaint.strokeWidth = boarder.toFloat()
            canvas.drawCircle(
                outWidth / 2.toFloat(),
                outHeight / 2.toFloat(),
                radius - boarder.toFloat(),
                boarderPaint
            )
        }
        return desBitmap
    }
}