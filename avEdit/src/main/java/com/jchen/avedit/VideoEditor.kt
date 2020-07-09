package com.jchen.avedit

import android.content.res.AssetFileDescriptor
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 *  android.resource://"+getPackageName()+"/"+R.raw.video
 *
1. MediaExtractor，即视频解析，主要作用是音视频分离，解析头信息，分别获取音视频流。

2. MediaCodec，即对音视频流进行解码，获取pcm和yuv数据。详见: https://blog.csdn.net/cheriyou_/article/details/92787998

3. Render，即分别对音视频进行渲染，此处涉及其他模块，我们重点需要了解的是音视频播放的时间戳对齐过程 https://blog.csdn.net/cheriyou_/article/details/101207443

MediaMuxer是用于复用基本流的，用它可以将音频和视频合成，目前支持输出MP4,Webm和3GP格式的视频，在Android7.0以后支持多路复用帧的MP4。

MediaFormat封装了描述媒体数据格式的信息，如音频或视频，通过它我们可以取出音频或者视频。

对应思考 ffmpeg 的 libavformat, libavcodec
 */
object VideoEditor {
    /**
     * 将audioSource的音频流和videoSource的视频流整合成新的文件
     *@param audioSource 音频源视频
     *@param audioStartTime 音频开始时间
     *@param videoSource 视频原视频
     *@param target 目标文件
     */
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    suspend fun combineTwoVideos(
        audioSource: AssetFileDescriptor,
        audioStartTime: Long,
        videoSource: AssetFileDescriptor,
        target: File
    ) {
        val extractorAudioSource = MediaExtractor()
        var sourceAudioTrackIndex = -1//音频源音频流index
        var targetAudioTrackIndex = -1//合成后音频流index
        var audioInputSize = 0;//输入数据缓冲区的最大大小

        val extractorVideoSource = MediaExtractor()
        var sourceVideoTrackIndex = -1//视频源视频流index
        var targetVideoTrackIndex = -1//合成后的视频流index
        var videoInputSize = 0//输入数据缓冲区的最大大小
        var frameRate = 0 //帧率
        var videoDuration: Long = 0//时长
        Log.i("VideoEditor", "audioSource = $audioSource")
        Log.i("VideoEditor", "videoSource = $videoSource")
        Log.i("VideoEditor", "target = ${target.absolutePath}")
        val muxer = MediaMuxer(target.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)//指定输出为MP4
        try {

            withContext(Dispatchers.IO) {
                Log.i("VideoEditor", "combineTwoVideos : ${Thread.currentThread()}")
                //音频信息
                Log.i("VideoEditor", "************* AudioSource **************")
                findTrackIndex(
                    extractorAudioSource,
                    audioSource
                ) block@{ mime: String, index: Int ->
                    if (mime.startsWith("audio/")) {// TODO: 2020/7/9 这里没有对存在多个音频流的情况做处理，比如一个视频源可能有普通音频流/杜比音频流两个
                        val format = extractorAudioSource.getTrackFormat(index)
                        sourceAudioTrackIndex = index
                        Log.i("VideoEditor", "sourceAudioTrackIndex $sourceAudioTrackIndex")
                        targetAudioTrackIndex = muxer.addTrack(format)//将音轨添加到MediaMuxer
                        Log.i("VideoEditor", "targetAudioTrackIndex $targetAudioTrackIndex")
                        audioInputSize =
                            format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)//该音轨缓冲区最大值
                        Log.i("VideoEditor", "audioInputSize $audioInputSize")
                        return@block true
                    }
                    return@block false
                }

                //视频信息
                Log.i("VideoEditor", "************* VideoSource **************")
                findTrackIndex(extractorVideoSource, videoSource) block@{ mime, index ->
                    if (mime.startsWith("video/")) {// TODO: 2020/7/9 同音频，这里取的是第一个视频流，可能还会有其他不同清晰度的视频流
                        val format = extractorVideoSource.getTrackFormat(index)
                        sourceVideoTrackIndex = index
                        Log.i("VideoEditor", "sourceVideoTrackIndex $sourceVideoTrackIndex")
                        targetVideoTrackIndex = muxer.addTrack(format)//将视频轨添加到MediaMuxer，并返回新的轨道
                        Log.i("VideoEditor", "targetVideoTrackIndex $targetVideoTrackIndex")
                        videoInputSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                        frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE)//获取视频流帧率
                        videoDuration = format.getLong(MediaFormat.KEY_DURATION)
                        return@block true
                    }
                    return@block false
                }

                muxer.start()//开始合成

                Log.i("VideoEditor", "------------ writeSampleData Audio -----------")
                extractorAudioSource.selectTrack(sourceAudioTrackIndex)//将音频源视频切换到目标音轨
                val audioBufferInfo = MediaCodec.BufferInfo()
                val audioByteBuffer: ByteBuffer = ByteBuffer.allocate(audioInputSize)
                Log.i("VideoEditor_1", "extractorAudioSource $extractorAudioSource")
                while (getInputBuffer(
                        extractorAudioSource,
                        audioByteBuffer,
                        audioStartTime,
                        videoDuration
                    ) { sampleSize, sampleTime ->
                        audioBufferInfo.size = sampleSize
                        audioBufferInfo.offset = 0
                        audioBufferInfo.flags = extractorAudioSource.sampleFlags
                        audioBufferInfo.presentationTimeUs = sampleTime - audioStartTime
                    }
                ) {
                    muxer.writeSampleData(targetAudioTrackIndex, audioByteBuffer, audioBufferInfo)
                    extractorAudioSource.advance()//推进到下一个样本，类似快进
                }
                extractorAudioSource.unselectTrack(sourceAudioTrackIndex)//取消选中音轨

                Log.i("VideoEditor", "------------ writeSampleData Video -----------")
                extractorVideoSource.selectTrack(sourceVideoTrackIndex)
                val videoBufferInfo =
                    MediaCodec.BufferInfo()
                val videoByteBuffer =
                    ByteBuffer.allocate(videoInputSize)
                Log.i("VideoEditor_1", "extractorVideoSource $extractorVideoSource")
                while (getInputBuffer(
                        extractorVideoSource,
                        videoByteBuffer,
                        -1,
                        videoDuration
                    ) { sampleSize, _ ->
                        //设置样本编码信息
                        videoBufferInfo.size = sampleSize
                        videoBufferInfo.offset = 0
                        videoBufferInfo.flags = extractorVideoSource.sampleFlags
                        videoBufferInfo.presentationTimeUs += 1000 * 1000 / frameRate
                    }
                ) {
                    muxer.writeSampleData(targetVideoTrackIndex, videoByteBuffer, videoBufferInfo)
                    extractorVideoSource.advance() //推进到下一个样本
                }
                extractorVideoSource.unselectTrack(sourceVideoTrackIndex)
            }
        } catch (e: Exception) {
            Log.e("VideoEditor", "combineTwoVideos: $e.message")
        } finally {
            Log.e("VideoEditor", "release")
            extractorAudioSource.release()
            extractorVideoSource.release()
            muxer.stop()
            muxer.release()
        }
    }

    private suspend fun getInputBuffer(
        extractor: MediaExtractor,
        byteBuffer: ByteBuffer, startTime: Long, videoDuration: Long,
        initBufferInfo: (sampleSize: Int, sampleTime: Long) -> Unit
    ): Boolean {

        val readSampleSize =
            extractor.readSampleData(byteBuffer, 0)//获取样本到缓冲区中
        Log.i("VideoEditor", "getInputBuffer $readSampleSize")
        if (readSampleSize < 0) {//没有样本可以获取，退出循环，取消select
            Log.i("VideoEditor", "readSampleSize $readSampleSize < 0")
            return false
        }
        val sampleTime = extractor.sampleTime//当前样本时间
        if (startTime >= 0 && sampleTime < startTime) {//还没到我们期望的开始时间,快进
            extractor.advance()
            Log.i("VideoEditor", "$sampleTime < $startTime, advance")
            return true
        }
        if (startTime >= 0 && sampleTime > (startTime + videoDuration)) {//
            Log.i(
                "VideoEditor",
                "$sampleTime > ($startTime + $videoDuration) stop"
            )
            return false
        }
        initBufferInfo.invoke(readSampleSize, sampleTime)
        return true
    }

    private suspend fun findTrackIndex(
        extractor: MediaExtractor,
        afd: AssetFileDescriptor,
        block: (mime: String, index: Int) -> Boolean
    ) {
        withContext(Dispatchers.IO) {
            extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)//设置视频源
            //extractor.setDataSource(sourcePath)
            Log.i("VideoEditor", "findTrackIndex : ${Thread.currentThread()}")
            val audioSourceTrackCounts = extractor.trackCount//获取源的轨道数
            for (i in 0 until audioSourceTrackCounts) {//遍历轨道，找到需要的音频轨道
                val format = extractor.getTrackFormat(i)//获取指定索引的MediaFormat
                val mime = format.getString(MediaFormat.KEY_MIME)
                Log.i("VideoEditor", "$i : mime: $mime")
                if (mime == null) continue

                if (block.invoke(mime, i)) {
                    break
                }
            }
        }
    }
}