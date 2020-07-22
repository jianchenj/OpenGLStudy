package com.jchen.encodeh264

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val requestCodeCamera = 1
    private var mCamera2Preview: Camera2Preview? = null


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermissions()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA),
                requestCodeCamera
            )
        } else {
            initCameraView()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun initCameraView() {
        mCamera2Preview = Camera2Preview(this)
        camera_preview.addView(mCamera2Preview)
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodeCamera) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                initCameraView()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun toggleVideo(view: View) {
        mCamera2Preview?.let {
            if (it.toggleVideo()) {
                record_btn.text = "停止录制视频"
            } else {
                record_btn.text = "开始录制视频"
            }
        }

    }
}
