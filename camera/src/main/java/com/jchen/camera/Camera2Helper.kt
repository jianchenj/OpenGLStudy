package com.jchen.camera

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.jchen.camera.util.BitmapUtil
import com.jchen.camera.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

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
    private var mCameraDevice: CameraDevice? = null
    private var mCameraCaptureSession: CameraCaptureSession? = null
    private var canTakePic = true // TODO: 2020/7/3 是不是拍照过程中将这个flag置为false，代表需要等到拍完才能再拍


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private var mCameraFacing = CameraCharacteristics.LENS_FACING_BACK //默认使用后摄

    private val handlerThread = HandlerThread("CameraThread")

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private var mSavePicSize = Size(PREVIEW_WIDTH, PREVIEW_HEIGHT) //保存图片大小

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private var mPreviewSize = Size(PREVIEW_WIDTH, PREVIEW_HEIGHT) //预览大小

    /**
     * 打开相机，创建回话都是耗时操作
     *
     *
    1 . CameraManager-->openCamera ---> 打开相机
    2 .CameraDeviceImpl-->createCaptureSession ---> 创建捕获会话
    3. CameraCaptureSession-->setRepeatingRequest ---> 设置预览界面
    4. CameraDeviceImpl-->capture ---> 开始捕获图片
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
    private fun initCameraInfo() {
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


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(
                mActivity,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            mActivity.toast("Camera Permission denied!")
            return
        }

        mCameraManager.openCamera(mCameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                Log.i(this@Camera2Helper.javaClass.name, "Camera onOpened !")
                mCameraDevice = camera
                createCaptureSession(camera)
            }

            override fun onDisconnected(camera: CameraDevice) {
                Log.i(this@Camera2Helper.javaClass.name, "Camera onDisconnected !")
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Log.i(this@Camera2Helper.javaClass.name, "Camera onError !")
                mActivity.toast("打开相机失败！$error")
            }

        }, mCameraHandler)
    }

    /**
     * 创建预览会话
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun createCaptureSession(cameraDevice: CameraDevice) {
        val captureRequestBuilder: CaptureRequest.Builder =
            cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

        val surface = Surface(mTextureView.surfaceTexture)
        captureRequestBuilder.addTarget(surface)//将CaptureRequest的构造器与Surface对象绑定在一起
        captureRequestBuilder.set(
            CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
        ) //auto-exposure（自动曝光） 闪光灯
        captureRequestBuilder.set(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        ) //对焦模式

        //CameraDeviceImpl->createCaptureSession传入的Surface列表有几个？
        //这儿的一个Surface表示输出流，Surface表示有多个输出流，我们有几个显示载体，就需要几个输出流。
        //对于拍照而言，有两个输出流：一个用于预览、一个用于拍照。
        //对于录制视频而言，有两个输出流：一个用于预览、一个用于录制视频。
        cameraDevice.createCaptureSession(
            arrayListOf(surface/*预览用*/, mImageReader?.surface/*拍照用*/),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    mActivity.toast("开启预览会话失败")
                }

                override fun onConfigured(session: CameraCaptureSession) {
                    mCameraCaptureSession = session
                    session.setRepeatingRequest(
                        captureRequestBuilder.build(), mCaptureCallBack,
                        mCameraHandler
                    )
                }

            }, mCameraHandler
        )
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private val mCaptureCallBack = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            super.onCaptureFailed(session, request, failure)
            mActivity.toast("拍摄失败！")
        }


        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            super.onCaptureCompleted(session, request, result)
            Log.i(Camera2Helper::javaClass.name, " onCaptureCompleted ")
        }

        /**
         * *当图像捕获部分向前进行时调用此方法;一些
         *(但不是所有)图像捕获结果可用。
         */
        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            super.onCaptureProgressed(session, request, partialResult)
            Log.i(Camera2Helper::javaClass.name, " onCaptureProgressed ")
        }
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private val onImageAvailableListener = ImageReader.OnImageAvailableListener {

        val image = it.acquireNextImage()
        val byteBuffer = image.planes[0].buffer
        val byteArray = ByteArray(byteBuffer.remaining())
        byteBuffer.get(byteArray)
        it.close()
        GlobalScope.launch(Dispatchers.Main) {
            BitmapUtil.savePic(
                mActivity.applicationContext,
                byteArray,
                mCameraSensorOrientation == 270,
                { savePath: String, time: String ->
                    mActivity.toast("图片保存成功！ 保存路径：$savePath 耗时：$time")
                },
                {
                    mActivity.toast("图片保存失败 $it")
                })
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
     * 手机屏幕方向竖屏的时候，手机sensor是横着的

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
            Surface.ROTATION_0, Surface.ROTATION_180 -> {//手机屏幕无旋转竖屏
                if (cameraSensorOrientation == 90 || cameraSensorOrientation == 270) {
                    exchange = true
                }
            }
            Surface.ROTATION_90, Surface.ROTATION_270 -> {//手机横屏
                if (cameraSensorOrientation == 0 || cameraSensorOrientation == 180) {
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

    fun takePic() {
        if(mCameraDevice == null || !mTextureView.isAvailable || !canTakePic) return
    }

    private inner class CompareSizesByArea : Comparator<Size> {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun compare(o1: Size, o2: Size): Int {
            return java.lang.Long.signum(o1.width.toLong() * o1.height - o2.width.toLong() * o2.height)
        }

    }

    fun mirrorPreview() {
        val matrix = Matrix()
        matrix.setScale(-1f , 1f)//注意set... post... pre...的区别(post先，pre后)
        matrix.postTranslate(mTextureView.width.toFloat(), 0f)
        mTextureView.setTransform(matrix)
        mTextureView.invalidate()
    }

}


