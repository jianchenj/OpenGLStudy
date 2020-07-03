package com.jchen.camera.util

import android.content.Context
import android.widget.Toast

fun Context.toast(msg: String) {//Context 的拓展函数
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}