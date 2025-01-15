package ru.rut.democamera

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.rut.democamera.databinding.ListItemDetailPagerBinding
import java.io.File

class MediaDetailPagerAdapter(
    private val mediaFiles: MutableList<File>
) : RecyclerView.Adapter<MediaDetailPagerAdapter.DetailViewHolder>() {

    inner class DetailViewHolder(private val binding: ListItemDetailPagerBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(file: File) {
            val extension = file.extension.lowercase()
            val isVideo = (extension == "mp4" || extension == "mov")

            if (isVideo) {
                // Показываем видео
                binding.detailImageView.visibility = View.GONE
                binding.detailVideoView.visibility = View.VISIBLE

                val uri = Uri.fromFile(file)
                val mediaController = MediaController(binding.root.context)
                mediaController.setAnchorView(binding.detailVideoView)

                binding.detailVideoView.setMediaController(mediaController)
                binding.detailVideoView.setVideoURI(uri)
                binding.detailVideoView.start()

            } else {
                // Показываем фото
                binding.detailVideoView.visibility = View.GONE
                binding.detailImageView.visibility = View.VISIBLE

                Glide.with(binding.root.context)
                    .load(file)
                    .into(binding.detailImageView)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        val binding = ListItemDetailPagerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        holder.bind(mediaFiles[position])
    }

    override fun getItemCount(): Int = mediaFiles.size
}