package com.jchen.openglstudy.test

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import com.jchen.openglstudy.R
import kotlin.math.min


class DrawableView constructor(context: Context, attributeSet: AttributeSet?) :
    View(context, attributeSet) {
    var mBitmap: Bitmap? = null

    init {
        val drawable: Drawable?
        if (attributeSet != null) {
            val array = context.obtainStyledAttributes(attributeSet, R.styleable.DrawableView)
            drawable = array.getDrawable(R.styleable.DrawableView_src)
            array.recycle()
        } else {
            drawable = context.resources.getDrawable(R.mipmap.draw_image_surface);
        }
        if (drawable == null) {
            throw  RuntimeException("DrawableView get null drawable")
        }
        mBitmap = (drawable as BitmapDrawable).bitmap
        drawable.callback = this
        drawable.level = 0
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        if (mBitmap == null) {
            throw  RuntimeException("DrawableView get null mBitmap")
        }
        val w: Int
        val h: Int
        w = when (widthMode) {
            MeasureSpec.EXACTLY -> {
                widthSize
            }
            MeasureSpec.AT_MOST -> {
                min(widthSize, mBitmap!!.width)
            }
            else -> {
                mBitmap!!.width
            }
        }

        h = when (heightMode) {
            MeasureSpec.EXACTLY -> {
                heightSize
            }
            MeasureSpec.AT_MOST -> {
                min(heightSize, mBitmap!!.height)
            }
            else -> {
                mBitmap!!.width
            }
        }

        setMeasuredDimension(w, h);
    }

    private var w = 0
    private var h = 0
    private var mRectSrc: Rect? = null
    private var mRectDst: Rect? = null

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null || mBitmap == null) {
            return
        }
        //支持padding
        w = width - paddingRight
        h = height - paddingBottom
        canvas.drawColor(Color.BLACK)
        if (mRectSrc == null) {
            mRectSrc = Rect(0, 0, mBitmap!!.width, mBitmap!!.height)
        }
        if (mRectDst == null || mRectDst!!.left != paddingLeft || mRectDst!!.top != paddingTop || mRectDst!!.right != width || mRectDst!!.bottom != height
        ) {
            mRectDst = Rect(paddingLeft, paddingTop, width, height)
        }
        canvas.drawBitmap(mBitmap!!, mRectSrc, mRectDst!!, null)
    }
}