package com.jchen.openglstudy

import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jchen.avedit.VideoEditor
import com.jchen.camera.util.BitmapUtil
import com.jchen.openglstudy.activity.AudioRecorderActivity
import com.jchen.openglstudy.activity.CameraActivity
import com.jchen.openglstudy.activity.FGLViewActivity
import com.jchen.openglstudy.activity.VideoActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val data = ArrayList<MenuBean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        add("绘制形体", FGLViewActivity::class.java)
        add("录音", AudioRecorderActivity::class.java)
        add("相机", CameraActivity::class.java)
        add("音视频", VideoActivity::class.java)
        add("音视频", VideoActivity::class.java)
        add("视频合并", null)
        mList.adapter = MenuAdapter()
    }

    private fun add(name: String, clazz: Class<*>?) {
        val bean = MenuBean()
        bean.name = name
        bean.clazz = clazz
        data.add(bean)
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun onClick(v: View?) {
        if (v == null) {
            return
        }
        val bean = data[v.tag as Int]
        if (mList.adapter?.getItemViewType(v.tag as Int) == 0) {
            when(bean.name) {
                "视频合并" -> {
                    MainScope().launch {
                        val file = File("${this@MainActivity.getExternalFilesDir(Environment.DIRECTORY_MUSIC)}/test.mp4")

                        val afd: AssetFileDescriptor =
                            resources.openRawResourceFd(R.raw.test1)
                        val afd2: AssetFileDescriptor =
                            resources.openRawResourceFd(R.raw.v1080)

                        VideoEditor.combineTwoVideos(afd,
                        0, afd2, file)
                        BitmapUtil.addVideo2MediaStore(this@MainActivity ,file)
                    }
                }
            }
        } else {
            startActivity(Intent(this, bean.clazz))
        }
    }

    private class MenuBean {
        var name: String? = null
        var clazz: Class<*>? = null
    }

    private inner class MenuAdapter : RecyclerView.Adapter<MenuAdapter.MenuHolder>() {


        internal inner class MenuHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            private val mBtn: Button = itemView.findViewById<View>(R.id.mBtn) as Button
            fun setPosition(position: Int) {
                val bean: MenuBean = data[position]
                mBtn.text = bean.name
                mBtn.tag = position
            }

            init {
                mBtn.setOnClickListener(this@MainActivity)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuHolder {
            return MenuHolder(layoutInflater.inflate(R.layout.item_button, parent, false))
        }

        override fun getItemCount(): Int {
            return data.size
        }

        override fun onBindViewHolder(holder: MenuHolder, position: Int) {
            holder.position = position
        }

        override fun getItemViewType(position: Int): Int {
            return if (data[position].clazz == null) {
                0
            } else {
                1
            }
        }
    }
}