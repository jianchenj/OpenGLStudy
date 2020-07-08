package com.jchen.openglstudy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jchen.openglstudy.activity.AudioRecorderActivity
import com.jchen.openglstudy.activity.CameraActivity
import com.jchen.openglstudy.activity.FGLViewActivity
import com.jchen.openglstudy.activity.VideoActivity
import kotlinx.android.synthetic.main.activity_main.*

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
        mList.adapter = MenuAdapter()
    }

    private fun add(name: String, clazz: Class<*>) {
        val bean = MenuBean()
        bean.name = name
        bean.clazz = clazz
        data.add(bean)
    }

    override fun onClick(v: View?) {
        if (v == null) {
            return
        }
        val bean = data[v.tag as Int]
        startActivity(Intent(this, bean.clazz))
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
    }
}