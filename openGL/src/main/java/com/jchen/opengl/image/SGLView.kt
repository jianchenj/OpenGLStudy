package com.jchen.opengl.image

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class SGLView constructor(context: Context?, attrs: AttributeSet? = null) :
    GLSurfaceView(context, attrs) {

    private var render : SGLRender? = null

    init {
        setEGLContextClientVersion(2)
        render = SGLRender(this)
        setRenderer(render)
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

    }

}