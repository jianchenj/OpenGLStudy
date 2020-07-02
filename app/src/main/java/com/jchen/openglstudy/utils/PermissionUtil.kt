package com.jchen.openglstudy.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker


object PermissionUtil {

    private val audioRecordPermissions = arrayOf<String>(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val cameraRecordPermissions = arrayOf<String>(
        Manifest.permission.CAMERA,
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

    @JvmStatic
    fun checkCameraRecordPermissions(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mPermissionList.clear()
            for (p in cameraRecordPermissions) {
                if (ContextCompat.checkSelfPermission(activity, p) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    mPermissionList.add(p)
                }
            }
            if (mPermissionList.isNotEmpty()) {
                ActivityCompat.requestPermissions(activity, cameraRecordPermissions, 2)
            }
        }
    }

    fun isGranted(grantResult: IntArray): Boolean {
        if (grantResult.isEmpty()) {
            return false
        }
        for (result in grantResult) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    fun hasPermission(
        context: Context, permissions: Array<String>
    ): Boolean {
        if (permissions.isEmpty()) {
            return false
        }
        for (per in permissions) {
            val result: Int = PermissionChecker.checkSelfPermission(context, per)
            if (result != PermissionChecker.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
}