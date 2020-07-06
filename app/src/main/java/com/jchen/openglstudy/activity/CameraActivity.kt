package com.jchen.openglstudy.activity

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.jchen.camera.Camera2Helper
import com.jchen.openglstudy.R
import com.jchen.openglstudy.utils.PermissionUtil
import kotlinx.android.synthetic.main.activity_camera.*

class CameraActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var mCamera2Helper: Camera2Helper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        PermissionUtil.checkCameraRecordPermissions(this)
        mCamera2Helper = Camera2Helper(this, camera_texture_view)
        take_picture.setOnClickListener(this)
        mirror_camera.setOnClickListener(this)
        exchange_camera.setOnClickListener(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (PermissionUtil.isGranted(grantResults) && PermissionUtil.hasPermission(
                this,
                permissions
            )
        ) {

        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.mirror_camera -> {
                mCamera2Helper.mirrorPreview()
            }

            R.id.take_picture -> {
                mCamera2Helper.takePic()
            }

            R.id.exchange_camera -> {
                mCamera2Helper.exchangeCamera()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onDestroy() {
        super.onDestroy()
        mCamera2Helper.releaseCamera()
        mCamera2Helper.releaseThread()
    }
}