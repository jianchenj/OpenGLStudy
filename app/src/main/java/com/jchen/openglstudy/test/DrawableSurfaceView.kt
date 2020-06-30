package com.jchen.openglstudy.test

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import com.jchen.openglstudy.R
import kotlin.math.min

// TODO: 2020/6/29 SurfaceFlinger
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class DrawableSurfaceView(context: Context, attributeSet: AttributeSet?) :
    SurfaceView(context, attributeSet), SurfaceHolder.Callback {

    private var mBitMap: Bitmap? = null

    init {
        val drawable: Drawable
        if (attributeSet != null) {
            val array: TypedArray =
                context.obtainStyledAttributes(attributeSet, R.styleable.DrawableSurfaceView)
            drawable = array.getDrawable(R.styleable.DrawableSurfaceView_src)!!
            array.recycle()
        } else {
            drawable = context.resources.getDrawable(R.mipmap.draw_image_surface)
        }
        if (drawable == null) {
            throw RuntimeException("DrawableSurfaceView get null drawable")
        }
        holder.addCallback(this)
        mBitMap = (drawable as BitmapDrawable).bitmap
        drawable.callback = this
        drawable.level = 0
    }
    private var w: Int = 0
    private var h: Int = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        if (mBitMap == null) {
            throw  RuntimeException("DrawableView get null mBitmap")
        }

        // 处理 wrap_content

        w = when (widthMode) {
            MeasureSpec.EXACTLY -> {
                widthSize
            }
            MeasureSpec.AT_MOST -> {
                min(widthSize, mBitMap!!.width)
            }
            else -> {
                mBitMap!!.width
            }
        }

        h = when (heightMode) {
            MeasureSpec.EXACTLY -> {
                heightSize
            }
            MeasureSpec.AT_MOST -> {
                min(heightSize, mBitMap!!.height)
            }
            else -> {
                mBitMap!!.width
            }
        }

        setMeasuredDimension(w, h + 150);
    }

    var mPaint : Paint = Paint()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)
        mBitMap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
        mPaint.color = Color.WHITE
        mPaint.textSize = 30f
        canvas.drawText("surfaceView 显示", 0f , h.toFloat() + 30, mPaint)
    }


    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
    }


    /**
     *
     * 这里有一个值得注意的地方，即在 DrawableSurfaceView 中绘制图片时，首先需要 lockSurface 返回一个 canvas，
     * 然后调用 canvas.drawBitmap()，最后再 unlockCanvasAndPost。
     * 而直接继承 View 的 DrawableView 则不需要，这是为什么呢？
     * 其实 DrawableView 也执行了 lockSurface 和 unlockCanvasAndPost，
     * 只不过是 ViewRootImpl 在遍历 View Tree 的时候执行的，而上面有提到，
     * SurfaceView 的内容由 SurfaceFlinger 直接合成，因此它不归 ViewRootImpl 管，所以要自己手动调用这两个方法。
     *  ViewRootImpl 遍历 View Tree 的对应的方法为 performTraversals，
     *  其中有三大流程：performMeasure、performLayout、performDraw，
     *  lockCanvas 和 unlockCanvasAndPost 这两个方法就是在 performDraw 里调用的：
     *
     */
    override fun surfaceCreated(holder: SurfaceHolder) {
        var canvas: Canvas? = null
        try {
            canvas = holder.lockCanvas()
            draw(canvas)
        } finally {
            if (canvas != null) {
                holder.unlockCanvasAndPost(canvas)
            }
        }

    }
}