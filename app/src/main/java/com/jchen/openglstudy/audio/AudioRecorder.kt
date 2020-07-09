package com.jchen.openglstudy.audio

import android.content.Context
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.jchen.baisc.util.FileUtil.getPrintSize
import com.jchen.openglstudy.utils.AudioRecordUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AudioRecorder constructor(val context: Context, private var audioInfo: AudioInfo) {


    /**
     *  最小缓冲单位，强烈建议使用AudioRecord.getMinBufferSize 来计算
     */
    private var minBufferSize: Int = 0


    private var audioRecord: AudioRecord? = null

    private val data: ByteArray

    private var file: File? = null

    init {
        initAudioRecord()
        minBufferSize = AudioRecordUtil.getMinBufferSize(audioInfo)
        data = ByteArray(minBufferSize)
    }

    private var isRecording: Boolean = false

//    fun configAudioInfo(sampleRate: Int?, channel: Int?, format: Int?) {
//        sampleRate?.let {
//            audioInfo.sampleRate = it
//        }
//        channel?.let {
//            audioInfo.channel = it
//        }
//        format?.let {
//            audioInfo.format = it
//        }
//    }


    private fun initAudioRecord() {
        if (audioRecord == null) {

            Log.d("AudioRecorder", "initAudioRecord")
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                audioInfo.sampleRate,
                audioInfo.channel,
                audioInfo.format,
                AudioRecordUtil.getMinBufferSize(audioInfo)
            )
        }
    }


    suspend fun startRecord(filePath: String) {
        file =
            File(filePath/*"${context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)}/test.pcm"*/)
        isRecording = true
        initAudioRecord()
        audioRecord!!.startRecording()
        withContext(Dispatchers.IO) {
            try {
                val os = FileOutputStream(file)
                while (isRecording) {
                    val re = audioRecord!!.read(data, 0, minBufferSize)
                    Log.d("AudioRecorder", "Recording re = $re")
                    if (re > 0) {
                        os.write(data)
                    }
                }
                os.close()
            } catch (e: Exception) {
                Log.d("AudioRecorder", "startRecord error $e.message")
            }
        }
    }

    fun stopRecord() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        isRecording = false
        if (file != null) {
            Log.d("AudioRecorder", "stopRecord ${getPrintSize(file!!.length())}")
        }
    }


}