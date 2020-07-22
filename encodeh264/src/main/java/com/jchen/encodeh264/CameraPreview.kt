package com.jchen.encodeh264

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
import android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 *  用于展示数据的 TextureView
 *      TextureView有自己的BufferQueue, 但是没有自己的Window,
 *      所以他在WMS中并没有自己的WindowState, 也就是说它从属于App的View 树.
 *      但是它有自己的 BufferQueue（见内部的SurfaceTexture）, View树中的其他View并不共用一个BufferQueue.
 *      TextureView必须支持硬件加速.
 *  区别于 SurfaceView
 *      SurfaceView是一个View, 有自己对应的Window, 所以在WMS中有自己的WindowState,
 *      在SurfaceFlinger中有自己的Layer.
 *
 *   //necessary 注释代表实现camera预览，录像的重要变量
 *
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class Camera2Preview @JvmOverloads constructor(
    private val mContext: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    TextureView(mContext, attrs, defStyleAttr) {//necessary TextureView本身，用于展示数据

    private lateinit var mCameraManager: CameraManager//necessary
    private var mCameraDevice: CameraDevice? = null//necessary

    /**
     * 处理图像数据
     */
    private var mImageReader: ImageReader? = null//necessary
    private var mBackGroundHandler: Handler? = null
    private var mBackgroundThread: HandlerThread? = null


    /**
     * 当前使用的camera的参数
     */
    private var mCameraFacing = CameraCharacteristics.LENS_FACING_BACK//默认使用后摄
    private var mCameraId = "0"
    private var mCameraCharacteristics: CameraCharacteristics? = null

    /**
     * 用于捕捉画面（预览也是连续的画面捕捉）
     */
    private var mPreviewRequestBuilder: CaptureRequest.Builder? = null//necessary

    /**
     * 会话
     */
    var mCameraCaptureSession: CameraCaptureSession? = null//necessary

    /**
     * 用于视频编码
     */
    private var mAvcEncoder : AvcEncoder? = null

    private var mPreviewSize: Size? = null

    /**
     * 编码出来后的帧率
     */
    private val mFrameRate = 30

    private val statePreview = 0
    private val stateRecording = 1
    private var mState = statePreview
    private var stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            mCameraDevice = camera
            createCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            mCameraDevice?.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            mCameraDevice?.close()
            mCameraDevice = null
        }

    }

    init {
        keepScreenOn = true

        if (mBackGroundHandler == null) {
            mBackgroundThread = HandlerThread("Camera Background")
        }
        mBackgroundThread!!.start()
        mBackGroundHandler = Handler(mBackgroundThread!!.looper)

        /**
         * SurfaceTexture并不是一个View, 它有自己的BufferQueue, 并且可以用来生成Surface,
         * 传入到SurfaceTexture中的Buffer会被转化为GL纹理,
         * 然后可以把这个纹理交给TextureView或者GLSurfaceView进行显示(纹理只是一堆数据, 必须附加到View上才能被展示)
         */
        surfaceTextureListener = object : SurfaceTextureListener {
            //necessary TextureView内部参数，监听TextureView 内部 SurfaceTexture 的变化
            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                mCameraCaptureSession?.close()
                mCameraCaptureSession = null

                mCameraDevice?.close()
                mCameraDevice = null

                mImageReader?.close()
                mImageReader = null
                return false
            }

            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                initCamera()
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Throws(RuntimeException::class)
    private fun initCamera() {
        mCameraManager = mContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        /**
         * 获取cameraIdList 并且匹配到符合条件的默认摄像头，和其属性
         */
        val cameraIDList = mCameraManager.cameraIdList
        if (cameraIDList.isEmpty()) {
            throw RuntimeException("cameraIDList is Empty")
        }
        for (cameraId in mCameraManager.cameraIdList) {//获取所有cameraid
            val cameraCharacteristics =
                mCameraManager.getCameraCharacteristics(cameraId)//获取该camera的参数
            val facing = cameraCharacteristics[CameraCharacteristics.LENS_FACING]//该camera的朝向
            if (facing == mCameraFacing) {//如果是后摄，则使用当前camera
                mCameraId = cameraId
                mCameraCharacteristics = cameraCharacteristics
                break
            }
        }

        /**
         * 硬件是否支持
         */
        if (mCameraCharacteristics == null) {
            throw RuntimeException("mCameraCharacteristics is null")
        }
        if (mCameraCharacteristics!![CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL] == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            throw RuntimeException("HARDWARE is not supported")
        }

        /**
         * 确定预览 mPreviewSize 大小
         */
        val map = mCameraCharacteristics!![CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]!!
        val previewSizes = map.getOutputSizes(SurfaceTexture::class.java)//所有支持的预览大小
        mPreviewSize = getPreferredPreviewSize(previewSizes, width, height)
        transformImage(width, height)



        initImageReader()
        if (ActivityCompat.checkSelfPermission(
                mContext,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        /**
         * 打开相机
         */
        mCameraManager.openCamera(mCameraId, stateCallback, null)
    }

    /**
     * 在打开相机 获取CameraDevice之后 创建预览界面
     */
    private fun createCameraPreview() {
        //val texture = surfaceTexture//当前Texture 的 SurfaceTexture
        if (mPreviewSize == null) return
        //设置 surfaceTexture 默认缓存区大小（之前获取的预览Size大小）
        surfaceTexture.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)
        val surface = Surface(surfaceTexture)
        if (mCameraDevice == null) return
        /**
         * 创建CaptureRequest对象 ，指定为预览类型
         */
        mPreviewRequestBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        //将CaptureRequest的结果返回到surface 和 mImageReader的surface上
        mPreviewRequestBuilder!!.addTarget(surface)
        mPreviewRequestBuilder!!.addTarget(mImageReader!!.surface)

        mCameraDevice!!.createCaptureSession(
            mutableListOf(surface, mImageReader!!.surface),
            object :
                CameraCaptureSession.StateCallback() {
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Toast.makeText(
                        mContext,
                        "Configuration change",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onConfigured(session: CameraCaptureSession) {
                    if (mCameraDevice == null) return
                    /**
                     * 获取 CameraCaptureSession
                     */
                    mCameraCaptureSession = session
                    updatePreview()
                }

            },
            null
        )
    }

    private fun updatePreview() {
        if (null == mCameraDevice || mPreviewRequestBuilder == null) return
//        mPreviewRequestBuilder!!.set(
//            CaptureRequest.CONTROL_AE_MODE,
//            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
//        ) //auto-exposure（自动曝光） 闪光灯
//        mPreviewRequestBuilder!!.set(
//            CaptureRequest.CONTROL_AF_MODE,
//            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
//        ) //对焦模式
        mPreviewRequestBuilder!!.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        mCameraCaptureSession?.setRepeatingRequest(
            mPreviewRequestBuilder!!.build(),
            null,
            mBackGroundHandler
        )
    }


    /**
     * 用于获取每一帧画面
     */
    private fun initImageReader() {
        if (mPreviewSize == null) return
        mImageReader =
            ImageReader.newInstance(mPreviewSize!!.width, mPreviewSize!!.height, ImageFormat.YV12, 1)
        mImageReader!!.setOnImageAvailableListener({
            // onImageAvailable
            /**
             * 这里一定要调用reader.acquireNextImage()和img.close方法否则不会一直回掉了
             *  然后画面会卡住
             */
            val img: Image = it.acquireNextImage()
            when (mState) {
                statePreview -> {
                    Log.i(Camera2Preview::class.toString(), "mState: STATE_PREVIEW")
                    mAvcEncoder?.stopEncode()
                    mAvcEncoder = null
                }

                stateRecording -> {
                    var dataYuv : ByteArray? = null
                    val planes = img.planes
                    /**
                     * 平铺模式
                     */
                    if (planes.size >= 3) {
                        val bufferY = planes[0].buffer
                        val bufferU = planes[1].buffer
                        val bufferV = planes[2].buffer
                        val lengthY = bufferY.remaining()
                        val lengthU = bufferU.remaining()
                        val lengthV = bufferV.remaining()
                        dataYuv = ByteArray(lengthY + lengthU + lengthV)
                        bufferY.get(dataYuv, 0, lengthY)
                        bufferU.get(dataYuv, lengthY, lengthU)
                        bufferV.get(dataYuv, lengthY + lengthU, lengthV)
                    }


                    if (mAvcEncoder == null) {
                        mAvcEncoder = getOutPutMediaFile(MEDIA_TYPE_VIDEO)?.let { it1 ->
                            AvcEncoder(mPreviewSize!!.width, mPreviewSize!!.height, mFrameRate
                                , it1, false)
                        }
                        GlobalScope.launch {
                            try {
                                mAvcEncoder!!.startEncode()
                            } catch (e : Exception) {

                            }

                        }
                    }
                    mAvcEncoder!!.putYUVData(dataYuv)
                }

            }
            img.close()
        }, mBackGroundHandler)
    }

    /**
     * 调整角度
     */
    private fun transformImage(width: Int, height: Int) {
        // TODO: 2020/7/16
    }

    /**
     * 获取最佳预览尺寸
     *
     * 注意一般情况下 我们手机都是竖屏的时候， 相机角度是横着的
     */
    private fun getPreferredPreviewSize(mapSizes: Array<Size>, width: Int, height: Int): Size? {
        Log.i(
            Camera2Preview::class.toString(),
            "getPreferredPreviewSize width = $width, height = $height"
        )
        val collectorSizes: MutableList<Size> = ArrayList()//所有符合条件的size
        for (option in mapSizes) {
            if (width > height) {//如果我们期望的宽度大于高度，就相当于横屏，宽度就用相机的
                if (option.width > width &&
                    option.height > height
                ) {
                    collectorSizes.add(option)
                }
            } else {//我们期望的是宽度大于高度，相当于竖屏，宽高就需要调换
                if (option.width > height &&
                    option.height > width
                ) {
                    collectorSizes.add(option)
                }
            }
        }
        if (collectorSizes.size > 0) {
            return Collections.min(
                collectorSizes
            ) { lhs, rhs ->
                java.lang.Long.signum(
                    (lhs?.width?.times(lhs.height) ?: 0) -
                            (rhs?.width?.times(rhs.height.toLong()) ?: 0)
                )
            }
        }
        Log.e(
            Camera2Preview::class.toString(), "getPreferredPreviewSize: best width=" +
                    mapSizes[0].width + ",height=" + mapSizes[0].height
        )
        return mapSizes[0]
    }

    internal fun onResume() {
        if (isAvailable) {
            initCamera()
        }
    }

    /**
     * 获取输出照片的/视频的路径
     */
    @SuppressLint("SimpleDateFormat")
    fun getOutPutMediaFile(mediaType: Int): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var fileName: String? = null
        var storageDir: File? = null
        //根据mediaType，分配文件名，文件路径
        when (mediaType) {
            MEDIA_TYPE_IMAGE -> {
                fileName = "JPEG_${timeStamp}_"
                storageDir = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            }
            MEDIA_TYPE_VIDEO -> {
                fileName = "MP4_${timeStamp}_"
                storageDir = mContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            }
        }
        if (storageDir == null) return null
        if (!storageDir.exists()) {
            if (storageDir.mkdirs()) {
                throw RuntimeException("getOutPutMediaFile Exception")
            }
        }

        val result = File.createTempFile(
            fileName/* prefix */, if (mediaType == MEDIA_TYPE_IMAGE) {
                ".jpg"
            } else {
                ".h264"
            }/* suffix */,
            storageDir /* directory */        )

        Log.i("test0721", "getOutPutMediaFile $result ")
        return result
    }

    fun toggleVideo(): Boolean {
        return if (mState == statePreview) {
            mState = stateRecording
            true
        } else {
            mState = statePreview
            false
        }
    }
}