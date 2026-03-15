package com.shslab.app.ui.filemanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shslab.app.R
import com.shslab.app.utils.FileUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileAdapter(
    private val onFileClick: (File) -> Unit,
    private val onFileLongClick: (File) -> Unit,
    private val onZipExtract: (File) -> Unit,
    private val onCompress: (File) -> Unit
) : ListAdapter<File, FileAdapter.FileViewHolder>(FILE_DIFF) {

    private val selectedItems = mutableSetOf<String>()

    companion object {
        private val FILE_DIFF = object : DiffUtil.ItemCallback<File>() {
            override fun areItemsTheSame(oldItem: File, newItem: File) =
                oldItem.absolutePath == newItem.absolutePath
            override fun areContentsTheSame(oldItem: File, newItem: File) =
                oldItem.lastModified() == newItem.lastModified() &&
                oldItem.length() == newItem.length()
        }
        private val DATE_FORMAT = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    }

    fun clearSelection() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.iv_file_icon)
        val tvName: TextView = itemView.findViewById(R.id.tv_file_name)
        val tvInfo: TextView = itemView.findViewById(R.id.tv_file_info)
        val ivSelected: ImageView = itemView.findViewById(R.id.iv_selected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = getItem(position)
        holder.tvName.text = file.name

        val isSelected = selectedItems.contains(file.absolutePath)
        holder.ivSelected.visibility = if (isSelected) View.VISIBLE else View.GONE

        if (file.isDirectory) {
            holder.ivIcon.setImageResource(R.drawable.ic_folder)
            val count = file.listFiles()?.size ?: 0
            holder.tvInfo.text = "$count items"
        } else {
            holder.ivIcon.setImageResource(getFileIcon(file))
            holder.tvInfo.text = "${FileUtils.formatSize(file.length())} • ${DATE_FORMAT.format(Date(file.lastModified()))}"
        }

        // Highlight selected
        holder.itemView.alpha = if (isSelected) 0.7f else 1.0f

        holder.itemView.setOnClickListener {
            if (selectedItems.isNotEmpty()) {
                toggleSelection(file, holder)
            } else {
                onFileClick(file)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (selectedItems.isEmpty()) {
                onFileLongClick(file)
            } else {
                toggleSelection(file, holder)
            }
            true
        }
    }

    private fun toggleSelection(file: File, holder: FileViewHolder) {
        if (selectedItems.contains(file.absolutePath)) {
            selectedItems.remove(file.absolutePath)
            holder.ivSelected.visibility = View.GONE
            holder.itemView.alpha = 1.0f
        } else {
            selectedItems.add(file.absolutePath)
            holder.ivSelected.visibility = View.VISIBLE
            holder.itemView.alpha = 0.7f
        }
    }

    fun getSelectedFiles(): List<File> {
        return currentList.filter { selectedItems.contains(it.absolutePath) }
    }

    private fun getFileIcon(file: File): Int {
        return when (file.extension.lowercase()) {
            "zip", "rar", "7z", "tar", "gz" -> R.drawable.ic_zip
            "html", "htm" -> R.drawable.ic_html
            "css" -> R.drawable.ic_css
            "js", "ts" -> R.drawable.ic_js
            "php" -> R.drawable.ic_php
            "json" -> R.drawable.ic_json
            "xml" -> R.drawable.ic_xml
            "jpg", "jpeg", "png", "gif", "bmp", "webp" -> R.drawable.ic_image
            "mp4", "avi", "mkv", "mov" -> R.drawable.ic_video
            "mp3", "wav", "ogg", "aac" -> R.drawable.ic_audio
            "pdf" -> R.drawable.ic_pdf
            "apk" -> R.drawable.ic_apk
            "txt", "log", "md" -> R.drawable.ic_text
            "java", "kt", "py", "c", "cpp", "h", "rb", "go", "rs", "swift", "dart" -> R.drawable.ic_code
            else -> R.drawable.ic_file
        }
    }
}
