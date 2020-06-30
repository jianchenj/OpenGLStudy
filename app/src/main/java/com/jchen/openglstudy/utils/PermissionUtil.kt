package com.jchen.openglstudy.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


object PermissionUtil {

    private val audioRecordPermissions = arrayOf<String>(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val mPermissionList = ArrayList<String>()

    @JvmStatic
    fun checkAudioRecordPermissions(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mPermissionList.clear()
            for (p in audioRecordPermissions) {
                if (ContextCompat.checkSelfPermission(activity, p) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    mPermissionList.add(p)
                }
            }
            if (mPermissionList.isNotEmpty()) {
                ActivityCompat.requestPermissions(activity, audioRecordPermissions, 1)
            }
        }
    }
}