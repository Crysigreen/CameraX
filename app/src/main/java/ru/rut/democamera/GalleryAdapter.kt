package ru.rut.democamera

import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File
import ru.rut.democamera.databinding.ListItemImageBinding
import ru.rut.democamera.databinding.ListItemMediaBinding
import ru.rut.democamera.databinding.ListItemMediaGridBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GalleryAdapter(
    private val mediaFiles: List<File>,
    private val itemClickListener: OnMediaItemClickListener
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ListItemMediaGridBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(file: File, position: Int) {
            val extension = file.extension.lowercase()
            val isVideo = extension == "mp4" || extension == "mov"

            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val dateString = dateFormat.format(Date(file.lastModified()))

            if (isVideo) {
                val thumbnail = ThumbnailUtils.createVideoThumbnail(
                    file.path,
                    MediaStore.Video.Thumbnails.MINI_KIND
                )
                binding.thumbnailImage.setImageBitmap(thumbnail)
                binding.mediaTypeText.text = "Видео"
            } else {
                Glide.with(binding.root.context)
                    .load(file)
                    .centerCrop()
                    .into(binding.thumbnailImage)
                binding.mediaTypeText.text = "Фото"
            }

            binding.mediaDateText.text = dateString

            binding.root.setOnClickListener {
                itemClickListener.onMediaItemClick(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemMediaGridBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(mediaFiles[position], position)
    }

    override fun getItemCount(): Int = mediaFiles.size
}