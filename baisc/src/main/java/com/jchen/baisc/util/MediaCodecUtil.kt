package com.jchen.baisc.util

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.jchen.baisc.audio.AudioInfo

object MediaCodecUtil {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    const val mCodecFormat = MediaFormat.MIMETYPE_AUDIO_AAC

    private const val mChanelCount = 1

    /**
     * 创建AAC encoder用于将pcm转化成AAC
     *
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun initAACEnCode(audioInfo: AudioInfo): MediaCodec {
        val mediaCodecInfo = selectCodec(mCodecFormat)
            ?: throw RuntimeException("$mCodecFormat is not available ")
        Log.i("MediaCodecUtil", "createMediaCodec mediaCodecInfo = ${mediaCodecInfo.name}")
        val format = MediaFormat.createAudioFormat(mCodecFormat, audioInfo.sampleRate, mChanelCount)
        format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC)
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        format.setInteger(MediaFormat.KEY_BIT_RATE, audioInfo.bitRate)
        val mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        return mediaCodec
    }

     private fun selectCodec(mimeType: String): MediaCodecInfo? {
        val numCodecs = MediaCodecList.getCodecCount()
        for (i in 0 until numCodecs) {
            val codeInfo = MediaCodecList.getCodecInfoAt(i)
            if (!codeInfo.isEncoder) {
                continue
            }
            val types = codeInfo.supportedTypes
            for (j in types.indices) {
                if (types[j].equals(mimeType, true)) {
                    return codeInfo
                }
            }
        }
        return null
    }

    /**
     * AAC 是一种压缩格式，可以直接使用播放器播放。为了实现流式播放，也就是做到边下边播，我们采用 ADTS 格式。
     * 给每帧加上 7 个字节的头信息。加上头信息就是为了告诉解码器，
     * 这帧音频长度、采样率、通道是多少，每帧都携带头信息，解码器随时都可以解码播放。
     *
     *
     * AAC的音频文件格式有ADIF和ADTS:

    ADIF：音频数据交换格式。这种格式的特点是，它只有一个统一的文件头，其余的都是音频数据。

    ADTS：音频数据传输流。它是一个有同步字的比特流。每一帧都有头信息。

    简单来说：ADIF不能随意解码，之后确定得到所有的数据以后才能解码，因为它只有一个头文件。
    ADTS可以任意解码，因为每一帧都有一个头文件。
     */
    fun addADTStoData(data: ByteArray, length: Int) {
        val profile = 2 //AAC LC
        val freqIdx = 4 //44.1KHz 存储采样率信息
        val chanCfg = 1 //CPE 存储通道信息

        data[0] = 0xFF.toByte()
        data[1] = 0xF9.toByte()
        data[2] = ((profile - 1 shl 6) + (freqIdx shl 2) + (chanCfg shr 2)).toByte()
        data[3] = (((chanCfg and 3) shl 6) + length shr 11).toByte()
        data[4] = ((length and 0x7FF) shr 3).toByte()
        data[5] = ((length and 7) shl 5 + 0x1F).toByte()
        data[6] = 0xFC.toByte()
    }
}