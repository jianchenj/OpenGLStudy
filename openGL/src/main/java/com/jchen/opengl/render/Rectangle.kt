package com.jchen.opengl.render

import android.opengl.GLES20
import android.view.View
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class Rectangle(view: View) : Shape(view) {//implements GLSurfaceView.Renderer

    private val vertexShaderCode = "attribute mat4 uMVPMatrix;" +//接收传入的转换矩阵
            "attribute vec4 vPosition;" + //接收传入的顶点
            "void main() {" +
            "  gl_Position = uMVPMatrix * vPosition;" + //矩阵变换计算之后的位置
            "}"

    private val fragmentShaderCode = "precision mediump float;" +
            "uniform vec4 vColor;" +
            "void main() {" +
            "  gl_FragColor = vColor;" +
            "}"

    private var mProgram: Int = 0

    private val coordsPerVertex = 3//每个顶点用三位数字确定坐标(x, y, z)

    private var vertexBuffer: FloatBuffer? = null//顶点坐标数据要转化成FloatBuffer格式
    private val indexBuffer : ShortBuffer? = null//所引法需要

    //当前绘制的顶点位置句柄
    private var mPositionHandle = 0
    //片元着色器颜色句柄
    private var mColorHandle = 0
    //变换矩阵句柄
    private var mMVPMatrixHandle = 0

    private val rectangleCoords = floatArrayOf(
        -1f, 0.5f, 0.0f,  // top left
        -1f, -0.5f, 0.0f,  // bottom left
        1f, 0.5f, 0.0f, // top right
        1f, -0.5f, 0f // bottom right
    )

    //用索引表示两个三角形，012和123
    var index = shortArrayOf(
        0, 1, 2, 1, 2, 3
    )


    //设置颜色，依次为RGBA
    private val color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)

    //顶点个数
    private val vertexCount = rectangleCoords.size / coordsPerVertex

    //顶点之间的偏移量,一个顶点有3个float，一个float是4个字节，所以一个顶点要12字节
    private val vertexStride = coordsPerVertex * 4 //跨度, 每个顶点四个字节


    init {
        //val bb = ByteBuffer.allocateDirect(rectangleCoords.size * 4)
        val bb = ByteBuffer.allocateDirect(rectangleCoords.size * 4)
        bb.order(ByteOrder.nativeOrder())

        //val bb2 =ByteBuffer.allocate()

        vertexBuffer = bb.asFloatBuffer()
        if (vertexBuffer == null) {
            throw RuntimeException("vertexBuffer is NULL")
        }
        vertexBuffer!!.put(rectangleCoords)
        vertexBuffer!!.position()

        //        int shader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
//        //将资源加入到着色器，并编译
//        GLES20.glShaderSource(shader, vertexShaderCode);
//        GLES20.glCompileShader(shader);
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)


//        int shader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);//创建shader
//        //讲资源加入到着色器，并编译
//        GLES20.glShaderSource(shader, fragmentShaderCode);//设置shader源
//        GLES20.glCompileShader(shader);//compile shader
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        //创建一个空的OpenGLES程序
        mProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(mProgram, vertexShader)
        GLES20.glAttachShader(mProgram, fragmentShader)
        GLES20.glLinkProgram(mProgram)

    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glUseProgram(mProgram)

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        GLES20.glEnableVertexAttribArray(mPositionHandle)
//        GLES20.glVertexAttribPointer(
//            mPositionHandle, Triangle.COORDS_PER_VERTEX,
//            GLES20.GL_FLOAT, false,
//            vertexStride, vertexBuffer
//        )
        GLES20.glVertexAttribPointer(
            mPositionHandle, coordsPerVertex, GLES20.GL_FLOAT,
            false, vertexStride, vertexBuffer
        )
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
        GLES20.glUniform4fv(mColorHandle, 1, color, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
        GLES20.glDisableVertexAttribArray(mPositionHandle)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {

    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {

    }

}