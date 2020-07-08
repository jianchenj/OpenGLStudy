package com.jchen.openglstudy.activity

import android.graphics.RectF
import android.hardware.camera2.params.Face
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.jchen.camera.Camera2Helper
import com.jchen.openglstudy.R
import com.jchen.openglstudy.utils.PermissionUtil
import kotlinx.android.synthetic.main.activity_camera.*

class CameraActivity : AppCompatActivity(), Camera2Helper.FaceDetectListener {

    private lateinit var mCamera2Helper: Camera2Helper

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        PermissionUtil.checkCameraRecordPermissions(this)
        mCamera2Helper = Camera2Helper(this, camera_texture_view)
        mCamera2Helper.setFaceDetectListener(this)
        take_picture.setOnClickListener { mCamera2Helper.takePic() }
        mirror_camera.setOnClickListener { mCamera2Helper.mirrorPreview() }
        exchange_camera.setOnClickListener { mCamera2Helper.exchangeCamera() }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onDestroy() {
        super.onDestroy()
        mCamera2Helper.releaseCamera()
        mCamera2Helper.releaseThread()
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
        if (PermissionUtil.isGranted(grantResults) && PermissionUtil.hasPermission(
                this,
                permissions
            )
        ) {

        }
    }
}