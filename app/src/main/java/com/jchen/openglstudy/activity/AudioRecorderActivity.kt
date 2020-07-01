package com.jchen.openglstudy.activity

import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.jchen.openglstudy.R
import com.jchen.openglstudy.audio.AudioInfo
import com.jchen.openglstudy.audio.AudioPlayer
import com.jchen.openglstudy.audio.AudioRecorder
import com.jchen.openglstudy.utils.AudioRecordUtil
import com.jchen.openglstudy.utils.FileUtil
import com.jchen.openglstudy.utils.PermissionUtil
import kotlinx.android.synthetic.main.content_audio_record.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File

class AudioRecorderActivity : AppCompatActivity(), View.OnClickListener {

    private var audioRecorder: AudioRecorder? = null
    private var audioPlayer: AudioPlayer? = null

    private var pcmPath: String? = null
    private var wavPath: String? = null

    private var mAudioInfo: AudioInfo = AudioInfo()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_record)
        setSupportActionBar(findViewById(R.id.toolbar))
        pcmPath = "${this.getExternalFilesDir(Environment.DIRECTORY_MUSIC)}/test.pcm"
        wavPath = "${this.getExternalFilesDir(Environment.DIRECTORY_MUSIC)}/test.wav"
        PermissionUtil.checkAudioRecordPermissions(this@AudioRecorderActivity)
        audioRecorder = AudioRecorder(applicationContext, mAudioInfo)
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

                    audioRecorder!!.startRecord(pcmPath!!)
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
                        "aaaaaaaaaa  $wavPath ,  ${FileUtil.getPrintSize(file.length())}"
                    )
                }
            }

            R.id.play_recording -> {
                MainScope().launch {
                    wavPath?.let { audioPlayer!!.playInModeStream(mAudioInfo, it) }
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