package com.jchen.opengl.image.filter

import android.content.res.Resources
import java.nio.FloatBuffer
import java.nio.ShortBuffer

abstract class AFilter {
    private val TAG = "Filter"

    val KEY_OUT = 0x101
    val KEY_IN = 0x102
    val KEY_INDEX = 0x201

    var DEBUG = true

    /**
     * 单位矩阵
     */
    //val OM: FloatArray = MatrixUtils.getOriginalMatrix()

    /**
     * 程序句柄
     */
    protected var mProgram = 0

    /**
     * 顶点坐标句柄
     */
    protected var mHPosition = 0

    /**
     * 纹理坐标句柄
     */
    protected var mHCoord = 0

    /**
     * 总变换矩阵句柄
     */
    protected var mHMatrix = 0

    /**
     * 默认纹理贴图句柄
     */
    protected var mHTexture = 0

    protected var mRes: Resources? = null


    /**
     * 顶点坐标Buffer
     */
    protected var mVerBuffer: FloatBuffer? = null

    /**
     * 纹理坐标Buffer
     */
    protected var mTexBuffer: FloatBuffer? = null

    /**
     * 索引坐标Buffer
     */
    protected var mindexBuffer: ShortBuffer? = null
}