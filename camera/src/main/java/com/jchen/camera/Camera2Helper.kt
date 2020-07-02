package com.jchen.camera

import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.jchen.camera.util.BitmapUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

//参照:  https://www.jianshu.com/p/0ea5e201260f
class Camera2Helper(private val mActivity: Activity, private val mTextureView: TextureView) {
    companion object {
        const val PREVIEW_WIDTH = 720   //预览宽度
        const val PREVIEW_HEIGHT = 1280 //预览的高度
        const val SAVE_WIDTH = 720      //保存图片的宽度
        const val SAVE_HEIGHT = 1280    //保存图片的高度
    }

    private var mCameraSensorOrientation: Int? = 0  //摄像头方向
    private var mCameraId: String = "0"
    private lateinit var mCameraManager: CameraManager
    private var mCameraHandler: Handler
    private lateinit var mCameraCharacteristics: CameraCharacteristics
    private val mDisplayRotation = mActivity.windowManager.defaultDisplay.rotation  //手机方向
    private var mImageReader: ImageReader? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private var mCameraFacing = CameraCharacteristics.LENS_FACING_BACK //默认使用后摄

    private val handlerThread = HandlerThread("CameraThread")

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private var mSavePicSize = Size(PREVIEW_WIDTH, PREVIEW_HEIGHT) //保存图片大小

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private var mPreviewSize = Size(PREVIEW_WIDTH, PREVIEW_HEIGHT) //预览大小

    /**
     * 打开相机，创建回话都是耗时操作
     */
    init {
        handlerThread.start()
        mCameraHandler = Handler(handlerThread.looper)
        mTextureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture?,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                releaseCamera()
                return true
            }

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture?,
                width: Int,
                height: Int
            ) {
                initCameraInfo()
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private suspend fun initCameraInfo() {
        mCameraManager = mActivity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIdLIst = mCameraManager.cameraIdList
        if (cameraIdLIst.isEmpty()) {
            Log.e("Camera2Helper", "没有camera")
            return
        }
        for (id in mCameraManager.cameraIdList) {
            val cameraCharacteristics = mCameraManager.getCameraCharacteristics(id)
            val facing = cameraCharacteristics[CameraCharacteristics.LENS_FACING]
            if (facing == mCameraFacing) {
                mCameraId = id
                mCameraCharacteristics = cameraCharacteristics
            }
            Log.d("Camera2Helper", "initCameraInfo 使用camera id = $id")
        }

        val supportLevel =
            mCameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        if (supportLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            Log.e("Camera2Helper", "相机不支持新特性")
        }

        //摄像头方向
        mCameraSensorOrientation =
            mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
        //获取摄像头管理支持的格式和尺寸的map
        val configurationMap: StreamConfigurationMap? =
            mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

        configurationMap?.let {
            val savePicSizes = it.getOutputSizes(ImageFormat.JPEG)  //保存照片尺寸
            val previewSizes = it.getOutputSizes(SurfaceTexture::class.java) //预览尺寸
            val exchange = exchangeWidthAndHeight(mDisplayRotation, mCameraSensorOrientation)

            mSavePicSize = getBestSize(
                if (exchange) mSavePicSize.height else mSavePicSize.width,
                if (exchange) mSavePicSize.width else mSavePicSize.height,
                if (exchange) mSavePicSize.height else mSavePicSize.width,
                if (exchange) mSavePicSize.width else mSavePicSize.height,
                savePicSizes.toList()
            )

            mPreviewSize = getBestSize(
                if (exchange) mPreviewSize.height else mPreviewSize.width,
                if (exchange) mPreviewSize.width else mPreviewSize.height,
                if (exchange) mTextureView.height else mTextureView.width,
                if (exchange) mTextureView.width else mTextureView.height,
                previewSizes.toList()
            )

            mTextureView.surfaceTexture.setDefaultBufferSize(
                mPreviewSize.width,
                mPreviewSize.height
            )

            Log.i(
                this.javaClass.name,
                "预览最优尺寸 ：${mPreviewSize.width} * ${mPreviewSize.height}, 比例  ${mPreviewSize.width.toFloat() / mPreviewSize.height}"
            )
            Log.i(
                this.javaClass.name,
                "保存图片最优尺寸 ：${mSavePicSize.width} * ${mSavePicSize.height}, 比例  ${mSavePicSize.width.toFloat() / mSavePicSize.height}"
            )
            mImageReader = ImageReader.newInstance(
                mSavePicSize.width,
                mSavePicSize.height,
                ImageFormat.JPEG,
                1
            )
            mImageReader?.setOnImageAvailableListener(onImageAvailableListener, mCameraHandler)
            openCamera()
        }
    }


    private fun openCamera() {
    }

    private suspend fun ImageReader.OnImageAvailableListener.doInBackground() = this.apply {
        BitmapUtil.savePic(
            mActivity.applicationContext,
            byteArray,
            mCameraSensorOrientation == 270,
            { savedPath, time ->
                mActivity.runOnUiThread {
                    //mActivity.toast("图片保存成功！ 保存路径：$savedPath 耗时：$time")
                }
            },
            { msg ->
                mActivity.runOnUiThread {
                    //mActivity.toast("图片保存失败！ $msg")
                }
            })
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private val onImageAvailableListener = ImageReader.OnImageAvailableListener {

        val image = it.acquireNextImage()
        val byteBuffer = image.planes[0].buffer
        val byteArray = ByteArray(byteBuffer.remaining())
        byteBuffer.get(byteArray)
        it.close()
        GlobalScope.launch(Dispatchers.Main) {
            BitmapUtil.savePic(mActivity.applicationContext, byteArray, mCameraSensorOrientation == 270, {savePath, time ->

            }, {
                
            })
        }
    }


}


/**
 * 根据提供的参数值返回预指定宽高最接近的尺寸
 *
 * @param targetW   目标宽度
 * @param targetH  目标高度
 * @param maxW      最大宽度(即TextureView的宽度)
 * @param maxH     最大高度(即TextureView的高度)
 * @param sizeList      摄像头支持的Size列表
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun getBestSize(
    targetW: Int,
    targetH: Int,
    maxW: Int,
    maxH: Int,
    sizeList: List<Size>
): Size {
    val bigEnough = ArrayList<Size>()
    val notBigEnough = ArrayList<Size>()
    for (size in sizeList) {//根据目标大小，将摄像头支持的size分为足够大，和不够大的
        //宽<=最大宽度  &&  高<=最大高度  &&  宽高比 == 目标值宽高比
        if (size.width <= maxW && size.height <= maxH
            && size.width == size.height * targetW / targetH
        ) {
            if (size.width >= targetW && size.height >= targetH) {
                bigEnough.add(size)
            } else {
                notBigEnough.add(size)
            }
        }
        Log.i(
            "Camera2Helper",
            "系统支持的尺寸: ${size.width} * ${size.height} ,  比例 ：${size.width.toFloat() / size.height}"
        )
    }

    //选择bigEnough中最小的值  或 notBigEnough中最大的值
    return when {
        bigEnough.size > 0 -> Collections.min(bigEnough, CompareSizesByArea())
        notBigEnough.size > 0 -> Collections.max(notBigEnough, CompareSizesByArea())
        else -> sizeList[0]
    }
}

/**
 * 根据提供的 手机屏幕方向 displayRotation 和 相机方向cameraSensorOrientation 返回是否需要交换宽高
 * 手机屏幕方向 0 ,180度为横屏, 摄像头方向为0， 180 为竖屏
 * 手机方向90 180 是竖屏， 摄像头90 180位横屏
 *
 * 手机屏幕竖屏的时候（90, 270），手机摄像头sensor默认是横着的(0, 180)
 *
 * 比如我们手机竖屏放置，设置的预览宽高是 720 * 1280 ，
 * 我们希望设置的是宽为 720，高为 1280 。
 * 而后置摄像头相对于竖直方向是 90°，也就说 720 相对于是摄像头来说是它的高度，
 * 1280 是它的宽度，这跟我们想要设置的刚好相反。
 * 所以，我们通过exchangeWidthAndHeight这个方法得出来是否需要交换宽高值，
 * 如果需要，那变成了把 1280 * 720 设置给摄像头，即它的宽为 720，高为 1280 。这样就与我们预期的宽高值一样了
 */
private fun exchangeWidthAndHeight(
    displayRotation: Int,
    cameraSensorOrientation: Int?
): Boolean {
    var exchange = false
    when (displayRotation) {
        Surface.ROTATION_0, Surface.ROTATION_180 -> {//手机横屏
            if (cameraSensorOrientation == 90 || cameraSensorOrientation == 270) {
                exchange = true
            }
        }
        Surface.ROTATION_90, Surface.ROTATION_270 -> {//手机竖屏
            if (cameraSensorOrientation == 0 || mCameraSensorOrientation == 180) {
                exchange = true
            }
        }
        else -> Log.e("Camera2Helper", "Display rotation is invalid: $displayRotation")
    }
    Log.i("Camera2Helper", "exchangeWidthAndHeight displayRotation $displayRotation")
    Log.i(
        "Camera2Helper",
        "exchangeWidthAndHeight cameraSensorOrientation $cameraSensorOrientation"
    )
    return exchange
}

private fun releaseCamera() {

}

private inner class CompareSizesByArea : Comparator<Size> {
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun compare(o1: Size, o2: Size): Int {
        return java.lang.Long.signum(o1.width.toLong() * o1.height - o2.width.toLong() * o2.height)
    }

}
}