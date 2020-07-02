package com.jchen.openglstudy.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.jchen.openglstudy.R
import com.jchen.openglstudy.utils.PermissionUtil
import kotlinx.android.synthetic.main.activity_camera.*

class CameraActivity : AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        PermissionUtil.checkCameraRecordPermissions(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(PermissionUtil.isGranted(grantResults) && PermissionUtil.hasPermission(this, permissions)){
            open_camera.setOnClickListener(this)
        }
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.open_camera -> {

            }
        }
    }
}