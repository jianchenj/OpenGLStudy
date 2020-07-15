package com.jchen.baisc.audio

import android.content.Context
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.jchen.baisc.util.AudioRecordUtil
import com.jchen.baisc.util.FileUtil.getPrintSize
import com.jchen.baisc.util.MediaCodecUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer


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


    /**
     * 录音，保存原始PCM文件
     */
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
            } finally {
                audioRecord?.stop()
                audioRecord?.release()
            }
        }
    }


    private var aacEncoder: MediaCodec? = null

    /**
     * 录音并编码成AAC
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    suspend fun startRecordAAC(filePath: String) {
        isRecording = true
        file = File(filePath)
        initAudioRecord()
        audioRecord!!.startRecording()

        withContext(Dispatchers.IO) {
            val fos = FileOutputStream(file)
            aacEncoder = MediaCodecUtil.initAACEnCode(audioInfo)
            aacEncoder!!.start()
            try {
                while (isRecording) {
                    /**

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

                     */
                    val re = audioRecord!!.read(data, 0, minBufferSize)
                    if (re > 0) {
                        val inputBufferIndex = aacEncoder!!.dequeueInputBuffer(-1)//从输入流队列中取数据进行编码操作
                        if (inputBufferIndex >= 0) {
                            val inputBuffer: ByteBuffer? =
                                aacEncoder!!.getInputBuffer(inputBufferIndex)//获取需要编码数据的输入流
                            inputBuffer!!.clear()
                            inputBuffer.put(data)
                            inputBuffer.limit(data.size)
                            aacEncoder!!.queueInputBuffer(inputBufferIndex, 0, re, 0, 0)//输入流入队列
                        }

                        val bufferInfo = MediaCodec.BufferInfo()
                        var outputBufferIndex = aacEncoder!!.dequeueOutputBuffer(bufferInfo, 0)
                        while (outputBufferIndex >= 0 && isRecording) {
                            val outputBuffer = aacEncoder!!.getOutputBuffer(outputBufferIndex)
                            outputBuffer!!.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            val chunkAudio = ByteArray(bufferInfo.size + 7)// 这 7 byte 是 ADTS 的大小
                            MediaCodecUtil.addADTStoData(chunkAudio, chunkAudio.size)
                            outputBuffer.position(bufferInfo.offset)
                            fos.write(chunkAudio)
                            aacEncoder!!.releaseOutputBuffer(outputBufferIndex, false)
                            outputBufferIndex = aacEncoder!!.dequeueOutputBuffer(bufferInfo, 0)
                        }
                    } else {
                        Log.e("AudioRecorder", "test0714 startRecordAAC audio buffer error: $re")
                        break
                    }

                }
            } catch (e: Exception) {
                Log.e("AudioRecorder", "test0714 startRecordAAC $e")
            } finally {
                isRecording = false
                if (audioRecord?.state != AudioRecord.RECORDSTATE_STOPPED) {
                    audioRecord?.stop()
                    audioRecord?.release()
                    audioRecord = null
                }
                aacEncoder?.stop()
                aacEncoder?.release()
                aacEncoder = null
                fos.close()
            }
        }
    }

    fun stopRecord() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        aacEncoder?.stop()
        aacEncoder?.release()
        aacEncoder = null
        isRecording = false
        if (file != null) {
            Log.d("AudioRecorder", "stopRecord ${getPrintSize(file!!.length())}")
        }
    }



}