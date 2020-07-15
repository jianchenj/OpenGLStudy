package com.jchen.baisc.util

import android.media.AudioFormat
import android.media.AudioRecord
import com.jchen.baisc.audio.AudioInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object AudioRecordUtil {

    fun getMinBufferSize(info: AudioInfo): Int {
        return AudioRecord.getMinBufferSize(
            info.sampleRate,
            info.channel,
            info.format
        )
    }

    /**
     * pcm文件转wav文件
     *
     * @param srcFilename 源文件路径
     * @param dstFilename 目标文件路径
     */
    suspend fun pcmToWav(srcFilename: String?, dstFilename: String?, audioInfo: AudioInfo) {
        withContext(Dispatchers.IO) {
            if (srcFilename == null || dstFilename == null) {
                throw RuntimeException("pcmToWav error !")
            }
            val `in`: FileInputStream
            val out: FileOutputStream
            val totalAudioLen: Long
            val totalDataLen: Long
            val longSampleRate = audioInfo.sampleRate.toLong()
            val channels = if (audioInfo.channel == AudioFormat.CHANNEL_IN_MONO) 1 else 2
            val byteRate = 16 * audioInfo.sampleRate * channels / 8.toLong()
            val data = ByteArray(
                getMinBufferSize(
                    audioInfo
                )
            )
            try {
                `in` = FileInputStream(srcFilename)
                out = FileOutputStream(dstFilename)
                totalAudioLen = `in`.channel.size()
                totalDataLen = totalAudioLen + 36
                writeWaveFileHeader(
                    out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate
                )
                while (`in`.read(data) != -1) {
                    out.write(data)
                }
                `in`.close()
                out.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    /**
     * 加入wav文件头
     */
    @Throws(IOException::class)
    private suspend fun writeWaveFileHeader(
        out: FileOutputStream, totalAudioLen: Long,
        totalDataLen: Long, longSampleRate: Long, channels: Int, byteRate: Long
    ) {
        withContext(Dispatchers.IO) {
            val header = ByteArray(44)
            // RIFF/WAVE header
            header[0] = 'R'.toByte()
            header[1] = 'I'.toByte()
            header[2] = 'F'.toByte()
            header[3] = 'F'.toByte()
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = (totalDataLen shr 8 and 0xff).toByte()
            header[6] = (totalDataLen shr 16 and 0xff).toByte()
            header[7] = (totalDataLen shr 24 and 0xff).toByte()
            //WAVE
            header[8] = 'W'.toByte()
            header[9] = 'A'.toByte()
            header[10] = 'V'.toByte()
            header[11] = 'E'.toByte()
            // 'fmt ' chunk
            header[12] = 'f'.toByte()
            header[13] = 'm'.toByte()
            header[14] = 't'.toByte()
            header[15] = ' '.toByte()
            // 4 bytes: size of 'fmt ' chunk
            header[16] = 16
            header[17] = 0
            header[18] = 0
            header[19] = 0
            // format = 1
            header[20] = 1
            header[21] = 0
            header[22] = channels.toByte()
            header[23] = 0
            header[24] = (longSampleRate and 0xff).toByte()
            header[25] = (longSampleRate shr 8 and 0xff).toByte()
            header[26] = (longSampleRate shr 16 and 0xff).toByte()
            header[27] = (longSampleRate shr 24 and 0xff).toByte()
            header[28] = (byteRate and 0xff).toByte()
            header[29] = (byteRate shr 8 and 0xff).toByte()
            header[30] = (byteRate shr 16 and 0xff).toByte()
            header[31] = (byteRate shr 24 and 0xff).toByte()
            // block align
            header[32] = (2 * 16 / 8).toByte()
            header[33] = 0
            // bits per sample
            header[34] = 16
            header[35] = 0
            //data
            header[36] = 'd'.toByte()
            header[37] = 'a'.toByte()
            header[38] = 't'.toByte()
            header[39] = 'a'.toByte()
            header[40] = (totalAudioLen and 0xff).toByte()
            header[41] = (totalAudioLen shr 8 and 0xff).toByte()
            header[42] = (totalAudioLen shr 16 and 0xff).toByte()
            header[43] = (totalAudioLen shr 24 and 0xff).toByte()
            out.write(header, 0, 44)
        }

    }
}