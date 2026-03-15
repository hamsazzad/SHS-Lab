package com.shslab.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.*

object FileUtils {

    private const val BUFFER_SIZE = 8192

    val CODE_EXTENSIONS = setOf(
        "html", "htm", "css", "js", "ts", "jsx", "tsx",
        "php", "json", "xml", "yaml", "yml", "md",
        "java", "kt", "py", "c", "cpp", "h", "rb",
        "go", "rs", "swift", "dart", "sh", "sql",
        "txt", "log", "ini", "cfg", "conf", "toml",
        "gradle", "properties", "gitignore", "env"
    )

    val ZIP_EXTENSIONS = setOf("zip", "apk")

    fun isCodeFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in CODE_EXTENSIONS || file.name.startsWith(".")
    }

    fun isZipFile(file: File): Boolean {
        return file.extension.lowercase() in ZIP_EXTENSIONS
    }

    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    fun deleteRecursive(file: File): Boolean {
        return if (file.isDirectory) {
            file.listFiles()?.all { deleteRecursive(it) } != false && file.delete()
        } else {
            file.delete()
        }
    }

    fun getFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            }
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }

    fun copyUriToFile(context: Context, uri: Uri, dest: File): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedOutputStream(FileOutputStream(dest), BUFFER_SIZE).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var len: Int
                    while (input.read(buffer).also { len = it } != -1) {
                        output.write(buffer, 0, len)
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openFileExternal(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val mime = getMimeType(file) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            // No app found
        }
    }

    fun getMimeType(file: File): String? {
        val ext = file.extension.lowercase()
        return when (ext) {
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "application/javascript"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "txt", "md", "log" -> "text/plain"
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            "apk" -> "application/vnd.android.package-archive"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        }
    }

    /**
     * Read a file in chunks — safe for low-memory devices.
     * Returns null if file is too large or unreadable.
     */
    fun readFileChunked(file: File, maxBytes: Long = 512 * 1024): String? {
        return try {
            if (file.length() > maxBytes) {
                val sb = StringBuilder()
                var totalRead = 0L
                BufferedReader(FileReader(file), BUFFER_SIZE).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null && totalRead < maxBytes) {
                        sb.append(line).append('\n')
                        totalRead += (line?.length ?: 0) + 1
                    }
                }
                if (file.length() > maxBytes) sb.append("\n\n[File truncated — too large to display fully]")
                sb.toString()
            } else {
                file.readText()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun writeFile(file: File, content: String): Boolean {
        return try {
            BufferedWriter(FileWriter(file), BUFFER_SIZE).use { writer ->
                writer.write(content)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
