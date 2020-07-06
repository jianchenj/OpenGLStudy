package com.jchen.openglstudy.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.jchen.openglstudy.utils.AudioRecordUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream


class AudioPlayer constructor(val context: Context) {

    private var audioTrack: AudioTrack? = null
    private var currentPosition: Long = 0

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    suspend fun playInModeStream(audioInfo: AudioInfo, path: String) {
        val buffSize = initAudioTrack(audioInfo)
        Log.d("AudioPlayer", " playInModeStream  playState ${audioTrack!!.playState}")
        val fileInputStream = FileInputStream(File(path))
        Log.d("AudioPlayer", "play skip = $currentPosition")
        fileInputStream.skip(currentPosition)
        audioTrack!!.play()
        withContext(Dispatchers.IO) {
            try {
                val tempBuff = ByteArray(buffSize)
                while (fileInputStream.available() > 0 && audioTrack!!.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    val readCount = fileInputStream.read(tempBuff)
                    if (readCount == AudioTrack.ERROR_INVALID_OPERATION ||
                        readCount == AudioTrack.ERROR_BAD_VALUE
                    ) {
                        continue
                    }
                    if (readCount != 0 && readCount != -1) {
                        audioTrack!!.write(tempBuff, 0, readCount)
                        currentPosition += readCount
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioPlayer", " ${e.stackTrace} ")
            }

        }
    }

    suspend fun playInModeStatic() {
        withContext(Dispatchers.IO) {
            //val `in` = context.resources.openRawResource(R.raw.ding)
        }
    }

    fun stop() {
        if (audioTrack == null && audioTrack!!.playState == AudioTrack.PLAYSTATE_PLAYING) return
        Log.d("AudioPlayer", "stop play!")
        currentPosition = 0
        audioTrack!!.stop()
        audioTrack!!.release()
    }

    fun pause() {
        if (audioTrack == null && audioTrack!!.playState != AudioTrack.PLAYSTATE_PLAYING) return
        Log.d("AudioPlayer", "pause() currentPosition = $currentPosition")
        audioTrack!!.pause()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun initAudioTrack(audioInfo: AudioInfo): Int {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val audioFormat = AudioFormat.Builder()
            .setEncoding(audioInfo.format)
            .setChannelMask(audioInfo.channel)
            .build()
        val buffSize = AudioRecordUtil.getMinBufferSize(audioInfo)
        audioTrack = AudioTrack(
            audioAttributes,
            audioFormat,
            buffSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        return buffSize
    }
}