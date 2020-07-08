package com.jchen.avedit

import android.media.MediaFormat
import java.io.File

/**
 *
 *
1. MediaExtractor，即视频解析，主要作用是音视频分离，解析头信息，分别获取音视频流。

2. MediaCodec，即对音视频流进行解码，获取pcm和yuv数据。详见: https://blog.csdn.net/cheriyou_/article/details/92787998

3. Render，即分别对音视频进行渲染，此处涉及其他模块，我们重点需要了解的是音视频播放的时间戳对齐过程

MediaMuxer是用于复用基本流的，用它可以将音频和视频合成，目前支持输出MP4,Webm和3GP格式的视频，在Android7.0以后支持多路复用帧的MP4。

MediaFormat封装了描述媒体数据格式的信息，如音频或视频，通过它我们可以取出音频或者视频。

对应思考 ffmpeg 的 libavformat, libavcodec
 */
object VideoEditor {
    /** 
     *@param audioSource 音频源视频
     *@param startTime 音频开始时间
     *@param videoSource 视频原视频
     *
     */
    fun combineTwoVideos(audioSource: String, startTime: Long, videoSource: String, target: File) {

    }
}