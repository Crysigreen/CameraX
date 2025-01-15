package ru.rut.democamera

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import ru.rut.democamera.databinding.ActivityGalleryBinding
import java.io.File

class GalleryActivity : AppCompatActivity(), OnMediaItemClickListener {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var adapter: GalleryAdapter
    private lateinit var allMediaFiles: List<File>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val directory = File(externalMediaDirs.firstOrNull()?.absolutePath ?: "")
        val files = directory.listFiles() ?: emptyArray()

        allMediaFiles = files.sortedByDescending { it.lastModified() }

        binding.galleryRecyclerView.layoutManager = GridLayoutManager(this, 3)
        adapter = GalleryAdapter(allMediaFiles, this)
        binding.galleryRecyclerView.adapter = adapter

        binding.backTo.setOnClickListener {
            finish()
        }
    }

    override fun onMediaItemClick(position: Int) {
        val intent = MediaDetailActivity.newIntent(this, allMediaFiles, position)
        startActivity(intent)
    }
}

interface OnMediaItemClickListener {
    fun onMediaItemClick(position: Int)
}