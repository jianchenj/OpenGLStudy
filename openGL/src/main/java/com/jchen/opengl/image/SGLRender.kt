package com.jchen.opengl.image

import android.opengl.GLSurfaceView
import android.view.View
import com.jchen.opengl.image.filter.AFilter
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class SGLRender constructor(view: View) : GLSurfaceView.Renderer {
    private var mFilter : AFilter? = null
    init {
       // mFilter = ContrastColorFilter()
    }

    override fun onDrawFrame(gl: GL10?) {
        TODO("Not yet implemented")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        TODO("Not yet implemented")
    }
}