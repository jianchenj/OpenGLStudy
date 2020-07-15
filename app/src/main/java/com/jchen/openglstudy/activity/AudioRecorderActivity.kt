package com.jchen.openglstudy.activity

import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.jchen.baisc.audio.AudioInfo
import com.jchen.baisc.audio.AudioPlayer
import com.jchen.baisc.audio.AudioRecorder
import com.jchen.baisc.util.AudioRecordUtil
import com.jchen.baisc.util.PermissionUtil
import com.jchen.openglstudy.R
import kotlinx.android.synthetic.main.content_audio_record.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File

class AudioRecorderActivity : AppCompatActivity(), View.OnClickListener {

    private var audioRecorder: AudioRecorder? = null
    private var audioPlayer: AudioPlayer? = null

    private var pcmPath: String? = null
    private var wavPath: String? = null
    private var aacPath: String? = null

    private var mAudioInfo: AudioInfo =
        AudioInfo()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_record)
        setSupportActionBar(findViewById(R.id.toolbar))
        pcmPath = "${this.getExternalFilesDir(Environment.DIRECTORY_MUSIC)}/test.pcm"
        wavPath = "${this.getExternalFilesDir(Environment.DIRECTORY_MUSIC)}/test.wav"
        aacPath = "${this.getExternalFilesDir(Environment.DIRECTORY_MUSIC)}/test.aac"
        PermissionUtil.checkAudioRecordPermissions(this@AudioRecorderActivity)
        audioRecorder =
            AudioRecorder(applicationContext, mAudioInfo)
        audioPlayer = AudioPlayer(applicationContext)
        start_recording.setOnClickListener(this)
        stop_recording.setOnClickListener(this)
        play_recording.setOnClickListener(this)
        convert_recording.setOnClickListener(this)
        stop_playing.setOnClickListener(this)
        play_recording2.setOnClickListener(this)
        pause.setOnClickListener(this)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.start_recording -> {
                MainScope().launch {

                    //audioRecorder!!.startRecord(pcmPath!!)
                    audioRecorder!!.startRecordAAC(aacPath!!)
                }
            }

            R.id.stop_recording -> {
                audioRecorder!!.stopRecord()
            }

            R.id.convert_recording -> {
                MainScope().launch {
                    AudioRecordUtil.pcmToWav(pcmPath, wavPath, mAudioInfo)
                    val file = File(wavPath)
                    Log.d(
                        "AudioRecorder",
                        "aaaaaaaaaa  $wavPath ,  ${com.jchen.baisc.util.FileUtil.getPrintSize(file.length())}"
                    )
                }
            }

            R.id.play_recording -> {
                MainScope().launch {
                    aacPath?.let { audioPlayer!!.playInModeStream(mAudioInfo, "/storage/emulated/0/Android/data/com.jchen.openglstudy/files/Music/ch00.aac") }
                }
            }

            R.id.play_recording2 -> {
                MainScope().launch {
                    Toast.makeText(this@AudioRecorderActivity, "todo", Toast.LENGTH_SHORT).show()
                    wavPath?.let { audioPlayer!!.playInModeStatic() }
                }
            }

            R.id.stop_playing -> {
                audioPlayer!!.stop();
            }

            R.id.pause -> {
                audioPlayer!!.pause()
            }


        }
    }
}