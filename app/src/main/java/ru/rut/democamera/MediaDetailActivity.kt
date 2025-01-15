package ru.rut.democamera

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import ru.rut.democamera.databinding.ActivityMediaDetailBinding
import java.io.File

class MediaDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMediaDetailBinding

    private lateinit var allFiles: MutableList<File>
    private var startPosition: Int = 0
    private var currentPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val paths = intent.getStringArrayListExtra(EXTRA_PATHS) ?: arrayListOf()
        startPosition = intent.getIntExtra(EXTRA_POSITION, 0)
        allFiles = paths.map { File(it) }.toMutableList()

        val pagerAdapter = MediaDetailPagerAdapter(allFiles)
        binding.detailViewPager.adapter = pagerAdapter
        binding.detailViewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        binding.detailViewPager.setCurrentItem(startPosition, false)
        currentPosition = startPosition

        binding.detailViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPosition = position
            }
        })

        binding.deleteFile.setOnClickListener {
            if (allFiles.isNotEmpty() && currentPosition in allFiles.indices) {
                val fileToDelete = allFiles[currentPosition]
                fileToDelete.delete()
                allFiles.removeAt(currentPosition)

                if (allFiles.isEmpty()) {
                    finish()
                } else {
                    pagerAdapter.notifyItemRemoved(currentPosition)
                    if (currentPosition >= allFiles.size) {
                        currentPosition = allFiles.size - 1
                    }
                    binding.detailViewPager.setCurrentItem(currentPosition, false)
                }
            }
        }
    }

    companion object {
        private const val EXTRA_PATHS = "extra_paths"
        private const val EXTRA_POSITION = "extra_position"

        fun newIntent(context: Context, files: List<File>, position: Int): Intent {
            val intent = Intent(context, MediaDetailActivity::class.java)
            // Кладём пути
            val paths = ArrayList(files.map { it.absolutePath })
            intent.putStringArrayListExtra(EXTRA_PATHS, paths)
            intent.putExtra(EXTRA_POSITION, position)
            return intent
        }
    }
}