package com.jchen.decodeh264

import android.Manifest
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.io.*
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {

    lateinit var dataInputStream: DataInputStream

    // https://zhuanlan.zhihu.com/p/27896239
    /**
     * SPS 和 PPS相关 00 00 00 01是分隔符
     *  00 00 00 01 67    (SPS)
    00 00 00 01 68    (PPS)
    00 00 00 01 65    (IDR帧)
    00 00 00 01 61    (P帧)
     */
    //对于H.264来说，"csd-0"和"csd-1"分别对应sps和pps；对于AAC来说，"csd-0"对应ADTS
    //mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(header_sps)) 在设置的时候  MediaFormat
    private val Use_SPS_and_PPS = false

    //编解码器 这里是解码
    private var mMediaCodec: MediaCodec? = null

    private var mStopFlag = false

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermissions()
        getFileInputStream("/storage/emulated/0/Android/data/com.jchen.encodeh264/files/Movies/MP4_20200723_175846_7697462492440647644.h264")
        initMediaCodec()
    }

    /**
     * 获取需要解码的文件流
     */
    private fun getFileInputStream(path: String) {
        val file = File(path)
        dataInputStream =
            DataInputStream(assets.open("MP4_20200723_175846_7697462492440647644.h264"))
    }

    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE),
                2
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun initMediaCodec() {
        surface_view.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {

            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {
            }


            override fun surfaceCreated(holder: SurfaceHolder) {
                mMediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)

                //MIMETYPE_VIDEO_AVC 是 h264的别称
                val mediaFormat = MediaFormat.createVideoFormat(
                    MediaFormat.MIMETYPE_VIDEO_AVC,
                    holder.surfaceFrame.width(),
                    holder.surfaceFrame.height()
                )

                //获取H264文件中的pps和sps数据
                /*h264常见的帧头数据为：
                00 00 00 01 67    (SPS)
                00 00 00 01 68    (PPS)
                00 00 00 01 65    (IDR帧)
                00 00 00 01 61    (P帧)*/
                if (Use_SPS_and_PPS) {
                    // TODO: 2020/7/22 这段数组看不明白
                    val header_sps = byteArrayOf(0, 0, 0, 1, 67, 66, 0, 42, 149.toByte(), 168.toByte(), 30, 0, 137.toByte(), 249.toByte(), 102, 224.toByte(), 32, 32, 32, 64)
                    val header_pps = byteArrayOf(0, 0, 0, 1, 68, 206.toByte(), 60, 128.toByte(), 0, 0, 0, 1, 6, 229.toByte(), 1, 151.toByte(), 128.toByte())
                    //对于H.264来说，"csd-0"和"csd-1"分别对应sps和pps；对于AAC来说，"csd-0"对应ADTS
                    mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(header_sps))
                    mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(header_pps))
                }

                //设置帧率
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 40)
                //解码后的数据渲染到 holder.surface
                mMediaCodec?.configure(
                    mediaFormat,
                    holder.surface,
                    null,
                    0/* 区别于 MediaCodec.CONFIGURE_FLAG_ENCODE */
                )
                GlobalScope.launch { doDecode() }
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private suspend fun doDecode() {
        if (mMediaCodec == null) return
        mMediaCodec!!.start()
        //解码后的数据，包含每一个buffer的元数据信息
        val bufferInfo = MediaCodec.BufferInfo()
        val startMs = System.currentTimeMillis()
        val timeoutUs = 10000
        //00 00 00 01 是帧的分割符，用于检测帧头
        val maker0 = byteArrayOf(0, 0, 0, 1)

        val dummyFrame = byteArrayOf(0x00, 0x00, 0x01, 0x20)
        var streamBuffer: ByteArray? = null



        withContext(Dispatchers.IO) {
            //返回可用的字节数组
            streamBuffer = getBytes(dataInputStream)

            var byteCount = 0
            while (!mStopFlag) {
                //得到可用字节数组长度
                byteCount = streamBuffer!!.size
                if (byteCount == 0) {
                    streamBuffer = dummyFrame
                }
                var startIndex = 0
                val remaining = byteCount//定义记录剩余字节的变量
                while (true) {
                    if (remaining == 0 || startIndex >= remaining) {//当剩余的字节为0 或者开始读取的字节的下标大于可用的字节数时，退出循环
                        Log.i("test0723", "doDecode break")
                        break
                    }
                    //寻找帧头部，找不到返回-1 todo 这里为啥 startIndex + 2
                    var nextFrameStart = KMPMatch(maker0, streamBuffer!!, startIndex + 2, remaining)
                    if (nextFrameStart == -1) nextFrameStart = remaining

                    Log.i("test0723", " nextFrameStart = $nextFrameStart")
                    //得到可用的缓存区index
                    val inputIndex: Int = mMediaCodec!!.dequeueInputBuffer(timeoutUs.toLong())
                    //有可用缓存区
                    if (inputIndex >= 0) {
                        val byteBuffer = mMediaCodec!!.getInputBuffer(inputIndex)
                        byteBuffer!!.clear()
                        //将可用的字节数组，传入缓冲区
                        byteBuffer.put(streamBuffer, startIndex, nextFrameStart - startIndex)
                        //把数据传递给解码器
                        mMediaCodec!!.queueInputBuffer(inputIndex, 0, nextFrameStart - startIndex, 0, 0)
                        //指定下一帧的位置
                        startIndex = nextFrameStart
                        Log.i("test0723", " startIndex = $startIndex")
                    } else {
                        Log.i("test0723", "doDecode continue")
                        continue
                    }

                    val outputIndex: Int =
                        mMediaCodec!!.dequeueOutputBuffer(bufferInfo, timeoutUs.toLong())
                    if (outputIndex >= 0) {
                        //帧控制是不在这种情况下工作，因为没有PTS H264是可用的
                        while (bufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                            delay(100)
                        }
                        val doRender = bufferInfo.size != 0
                        Log.i("test0723", "doRender = $doRender")
                        //对outputbuffer的处理完后，调用这个函数把buffer重新返回给codec类。
                        mMediaCodec!!.releaseOutputBuffer(outputIndex, doRender)
                    } else {
                    }
                }
                mStopFlag = true
            }
        }

    }

    /**
     * 获得可用的字节数组
     */
    private suspend fun getBytes(`is`: InputStream): ByteArray? {
        var len = 0
        var size = 1024
        var buf: ByteArray? = null
        if (`is` is ByteArrayInputStream) {//字节数组
            size = `is`.available()//获取一段大小
            buf = ByteArray(size)//创建该大小的数组
            len = `is`.read(buf, 0, size)//读取到这个数组里
        } else {
            val bos = ByteArrayOutputStream()
            buf = ByteArray(size)

            while (`is`.read(buf, 0, size).also { len = it } != -1) {//当读取到的len不为 -1
                bos.write(buf, 0, len)//将读取的数据写入到字节输出流
            }
            buf = bos.toByteArray()
        }
        return buf
    }

    /**
     * 查找帧头部的位置
     *
     * @param pattern 文件头字节数组
     * @param bytes   可用的字节数组
     * @param start   开始读取的下标
     * @param remain  可用的字节数量
     */
    private suspend fun KMPMatch(
        pattern: ByteArray,
        bytes: ByteArray,
        start: Int,
        remain: Int
    ): Int {
        delay(30)// TODO: 2020/7/23  这是为啥
        val lsp = computeLspTable(pattern)

        var j = 0 //pattern中匹配的下标
        for (i in start until remain) {
            while (j > 0 && bytes[i] != pattern[j]) {
                // Fall back in the pattern
                j = lsp[j - 1] // Strictly decreasing
            }
            if (bytes[i] == pattern[j]) {
                // Next char matched, increment position
                j++
                if (j == pattern.size) return i - (j - 1)
            }
        }
        return -1
    }

    // TODO: 2020/7/23 这是啥
    //0 1 2 0
    private suspend fun computeLspTable(pattern: ByteArray): IntArray {
        val lsp = IntArray(pattern.size)
        lsp[0] = 0 //base case
        for (i in 1 until pattern.size) {
            // Start by assuming we're extending the previous LSP
            var j = lsp[i - 1]
            while (j > 0 && pattern[i] != pattern[j]) {
                j = lsp[j - 1]
            }
            if (pattern[i] == pattern[j]) j++
            lsp[i] = j
        }
        return lsp
    }
}
