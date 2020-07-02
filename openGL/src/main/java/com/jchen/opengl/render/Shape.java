package com.jchen.opengl.render;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.view.View;

public abstract class Shape implements GLSurfaceView.Renderer {

    protected View mView;

    public Shape(View view) {
        this.mView = view;
    }

    public int loadShader(int type, String shaderCode) {
        //根据type创建顶点着色器or片元着色器
        int shader = GLES20.glCreateShader(type);
        //讲资源加入到着色器，并编译
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
