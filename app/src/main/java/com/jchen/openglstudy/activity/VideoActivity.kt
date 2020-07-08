package com.jchen.openglstudy.activity

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jchen.avedit.AvItem
import com.jchen.avedit.Photo
import com.jchen.openglstudy.R
import kotlinx.android.synthetic.main.content_video.*
import kotlinx.android.synthetic.main.item_av_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class VideoActivity : AppCompatActivity() {

    private val mExImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    private val mExVideoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    private val mInUri = MediaStore.Images.Media.INTERNAL_CONTENT_URI
    private var mAvItemList: MutableList<AvItem> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        setSupportActionBar(findViewById(R.id.toolbar))

        MainScope().launch(Dispatchers.Main) {
            initData()
            mList.layoutManager =
                LinearLayoutManager(this@VideoActivity, LinearLayoutManager.VERTICAL, false)
            mList.adapter = PhotoListViewAdapter()
        }

        gallery()
    }

    private suspend fun initData() {
        mAvItemList.clear()
        initDataVideo()
        initDataPhoto()
    }

    private suspend fun initDataVideo() {

    }

    fun gallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "video/*"
        startActivityForResult(intent, 1111)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var icon_path : String? = ""
        when (requestCode) {
            1111 -> if (data != null) {
                icon_path = if (data.dataString!!.contains("content")) {
                    data.data?.let { getRealPathFromURI(it) }
                } else {
                    data!!.dataString?.replace("file://", "")
                }
            }
        }
        Log.i("test0708", "onActivityResult $icon_path")
    }

    private fun getRealPathFromURI(contentURI: Uri): String? {
        val cursor =
            contentResolver.query(contentURI, null, null, null, null)
        return if (cursor == null) { // Source is Dropbox or other similar local file path
            contentURI.path
        } else {
            cursor.moveToFirst()
            val idx =
                cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            cursor.getString(idx)
        }
    }

    private suspend fun initDataPhoto() {
        withContext(Dispatchers.IO) {
            val projImage = arrayOf(
                MediaStore.Images.Media._ID
                , MediaStore.Images.Media.DATA
                , MediaStore.Images.Media.DATE_MODIFIED
                , MediaStore.Images.Media.SIZE
                , MediaStore.Images.Media.DISPLAY_NAME
            )
            val mCursor: Cursor? = contentResolver.query(
                mExImageUri,
                projImage,
//                MediaStore.Images.Media.MIME_TYPE + "=? or " + MediaStore.Images.Media.MIME_TYPE + "=?",
//                arrayOf("image/jpeg", "image/png"),
//                MediaStore.Images.Media.DATE_MODIFIED + " desc"
                null, null, null
            )

            if (mCursor != null) {
                while (mCursor.moveToNext()) {
                    // 获取图片的路径
                    val path =
                        mCursor.getString(mCursor.getColumnIndex(MediaStore.Images.Media.DATA))
                    //Log.i("test0708", "path = $path")
                    val size =
                        mCursor.getInt(mCursor.getColumnIndex(MediaStore.Images.Media.SIZE)) / 1024
                    //Log.i("test0708", "size = $size")
                    val displayName =
                        mCursor.getString(mCursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME))
                    //Log.i("test0708", "displayName = $displayName")
                    val date =
                        mCursor.getInt(mCursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED))
                    //Log.i("test0708", "size = $size")
                    val photo = Photo(displayName, date.toString(), size.toLong(), path)
                    mAvItemList.add(photo)
                }
                mCursor.close()
            }
        }
    }

    private inner class PhotoListViewAdapter :
        RecyclerView.Adapter<PhotoListViewAdapter.ListHolder>() {

        private val photo = 1;
        private val video = 2;

        internal inner class ListHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListHolder {
            return ListHolder(layoutInflater.inflate(R.layout.item_av_item, parent, false))
        }

        override fun getItemCount(): Int {
            return mAvItemList.size
        }

        override fun onBindViewHolder(holder: ListHolder, position: Int) {
            val avItem = mAvItemList[position]
            holder.itemView.title.text = avItem.name
            Glide.with(holder.itemView.image).load(avItem.path).into(holder.itemView.image)
        }

        override fun getItemViewType(position: Int): Int {

            return when (mAvItemList[position]) {
                is Photo -> {
                    photo
                }
                else -> {
                    video
                }
            }
        }
    }
}