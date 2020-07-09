package com.jchen.openglstudy.activity

import android.content.Intent
import android.graphics.RectF
import android.hardware.camera2.params.Face
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.jchen.camera.Camera2Helper
import com.jchen.openglstudy.R
import kotlinx.android.synthetic.main.activity_camera.*

class CameraActivity : AppCompatActivity(), Camera2Helper.FaceDetectListener {

    private lateinit var mCamera2Helper: Camera2Helper

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        com.jchen.baisc.util.PermissionUtil.checkCameraRecordPermissions(this)
        mCamera2Helper = Camera2Helper(this, camera_texture_view)
        mCamera2Helper.setFaceDetectListener(this)
        take_picture.setOnClickListener { mCamera2Helper.takePic() }
        mirror_camera.setOnClickListener { mCamera2Helper.mirrorPreview() }
        exchange_camera.setOnClickListener { /*mCamera2Helper.exchangeCamera() */ gotoCaptureVideo()}
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onDestroy() {
        super.onDestroy()
        mCamera2Helper.releaseCamera()
        mCamera2Helper.releaseThread()
    }

    private fun gotoCaptureVideo() {
        var intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        if (intent.resolveActivity(packageManager) != null)
            startActivityForResult(intent, 11)
    }

    override fun onFaceDetect(faces: Array<Face>, facesRect: ArrayList<RectF>) {
        face_view.setFaces(facesRect)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (com.jchen.baisc.util.PermissionUtil.isGranted(grantResults) && com.jchen.baisc.util.PermissionUtil.hasPermission(
                this,
                permissions
            )
        ) {

        }
    }
}