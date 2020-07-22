package com.jchen.encodeh264

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ArrayBlockingQueue

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class AvcEncoder(
    private val width: Int,
    private val height: Int,
    private var frameRate: Int,
    private var outFile: File,
    private var isCamera: Boolean
) {

    private val timeOutUs  = 12000;
    private var mMediaCodec: MediaCodec? = null
    private var mOutPutStream: BufferedOutputStream? = null

    private val mYuvQueueSize = 10

    //用来存储录制的YUV数据
    private var mYuvQueue = ArrayBlockingQueue<ByteArray>(mYuvQueueSize)

    private var mConfigByte: ByteArray? = null

    init {
        /**
         * 创建 MediaFormat ，配置给编解码器(这里是编码) MediaCodec
         */
        val mediaFormat =
            MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
        mediaFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
        )
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 5)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)//关键帧(I帧)间隔时间1s
        mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)

        mMediaCodec!!.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mMediaCodec!!.start()//必须要先start
        mOutPutStream = BufferedOutputStream(FileOutputStream(outFile))
    }


    private var isRunning = false
    suspend fun startEncode() {
        withContext(Dispatchers.IO) {
            isRunning = true
            Log.i("test0722", "startEncoder ")
            while (isRunning) {
                var input: ByteArray? = null
                var pts: Long = 0
                var generateIndex: Long = 0
                if (mMediaCodec == null) throw RuntimeException("mMediaCodec = null")
                if (mYuvQueue.size > 0) {
                    input = mYuvQueue.poll()
                    val yuv420sp = ByteArray(width * height * 3 / 2)
                    if (isCamera) {//Camera NV21
                        //NV21 数据所需空间如下
                        // Y=w*h; U=w*h/4; V=w*h/4
                        //所以总数是 (1 + 1/4 + 1/4 = 3/2)*w*h
                        NV21ToNV12(input, yuv420sp)
                    } else {//Camera2
                        YV12toNV12(input, yuv420sp)
                    }
                    input = yuv420sp
                }
                if (input == null ) {
                    Log.w("AvcEncoder" , "input null delay 500 !!!! ")
                    delay(500)
                    continue
                }
                /**
                 * 一次 queueInputBuffer 可能会对应多次 dequeueOutputBuffer
                 */
                val inputBufferIndex = mMediaCodec!!.dequeueInputBuffer(-1)
                if (inputBufferIndex >= 0) {
                    pts = computePts(generateIndex)
                    val inputBuffer = mMediaCodec!!.getInputBuffer(inputBufferIndex)
                    inputBuffer?.clear()
                    inputBuffer?.put(input)
                    mMediaCodec!!.queueInputBuffer(inputBufferIndex, 0, input.size, pts, 0)
                    generateIndex += 1
                }

                val bufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()
                var outputBufferIndex = mMediaCodec!!.dequeueOutputBuffer(bufferInfo, timeOutUs.toLong())
                while (outputBufferIndex >= 0) {

                    val outputBuffer = mMediaCodec!!.getOutputBuffer(outputBufferIndex)
                    val outData = ByteArray(bufferInfo.size)
                    outputBuffer.get(outData)
                    when(bufferInfo.flags) {
                        MediaCodec.BUFFER_FLAG_CODEC_CONFIG -> {
                            mConfigByte = ByteArray(bufferInfo.size)
                            mConfigByte = outData
                            Log.i("AvcEncoder" , "write CONFIG " + bufferInfo.size)
                        }

                        MediaCodec.BUFFER_FLAG_KEY_FRAME-> {//关键帧头部插入config
                            if (mConfigByte == null) throw RuntimeException(" mConfigByte = null error!!")
                            val keyframe = ByteArray(bufferInfo.size + mConfigByte!!.size)
                            System.arraycopy(mConfigByte!!, 0, keyframe, 0, mConfigByte!!.size)
                            System.arraycopy(outData,0, keyframe, mConfigByte!!.size, outData.size)
                            if (mOutPutStream == null)  throw RuntimeException(" mOutPutStream = null error!!")
                            Log.i("AvcEncoder" , "write KEY " + keyframe.size)
                            mOutPutStream!!.write(keyframe,0 ,keyframe.size)
                        }

                        else -> {//非关键帧直接写入
                            Log.i("AvcEncoder" , "write " + outData.size)
                            mOutPutStream!!.write(outData, 0, outData.size)
                        }
                    }
                    mMediaCodec!!.releaseOutputBuffer(outputBufferIndex, false)
                    outputBufferIndex = mMediaCodec!!.dequeueOutputBuffer(bufferInfo, timeOutUs.toLong())
                }
            }
        }
    }


    /**
     * YU12: YYYYYYYY UUVV
     *   NV12 YYYYYYYY UVUV
     */
    private fun YV12toNV12(yv12: ByteArray, nv12: ByteArray) {
        val nLenY = width * height
        val nLenU = nLenY / 4

        System.arraycopy(yv12, 0, nv12, 0, width * height)
        for (i in 0 until nLenU) {
            nv12[nLenY + 2 * i] = yv12[nLenY + i]
            nv12[nLenY + 2 * i + 1] = yv12[nLenY + nLenU + i];
        }
    }

    private fun NV21ToNV12(nv21: ByteArray, nv12: ByteArray) {
        val frameSize = width * height
        System.arraycopy(nv21, 0, nv12, 0, frameSize)
        for (i in 0 until frameSize) {
            nv12[i] = nv21[i]
        }

        /** 替换 NV12: YYYYYYYY UVUV  和 NV21: YYYYYYYY VUVU
         * 处理u 和 v 数据 uv数据长度总共 frameSize/2,
         *  他的index 从 frameSize 开始(开始位置可能是u或者v,根据NV21还是NV12)
         *   数组总长度是 frameSize*3/2
         *   step 2 是因为这两个格式uv 是一个隔一个的
         */
        for (i in 0 until frameSize / 2 step 2) {
            nv12[frameSize + i - 1] = nv21[i + frameSize]
        }
        for (i in 0 until frameSize / 2 step 2) {
            nv12[frameSize + i] = nv21[i + frameSize - 1]
        }
    }

    private fun computePts(frameIndex: Long): Long {
        return 132 + frameIndex * 1000000 / frameRate
    }

    fun stopEncode() {
        if (!isRunning) return
        isRunning = false
        mMediaCodec?.stop()
        mMediaCodec?.release()
        mMediaCodec = null

        mOutPutStream?.flush()
        mOutPutStream?.close()
        mOutPutStream = null

    }

    fun putYUVData(data : ByteArray?) {
        if (data == null) return
        if (mYuvQueue.size >= 10) {
            mYuvQueue.poll()
        }
        mYuvQueue.add(data)
    }
}