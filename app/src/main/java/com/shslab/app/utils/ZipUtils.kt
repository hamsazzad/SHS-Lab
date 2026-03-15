package com.shslab.app.utils

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils {

    private const val BUFFER_SIZE = 8192

    /**
     * Extract a ZIP file to a destination directory.
     * Processes in chunks to avoid OOM on low-memory devices.
     */
    fun extractZip(
        zipFile: File,
        destDir: File,
        onProgress: (Int) -> Unit = {}
    ): Result<Unit> {
        return try {
            destDir.mkdirs()
            val totalSize = zipFile.length().toFloat()
            var processed = 0L

            ZipInputStream(BufferedInputStream(FileInputStream(zipFile), BUFFER_SIZE)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val outFile = File(destDir, sanitizeEntryName(entry.name))

                    // Security: prevent path traversal
                    val canonicalDest = destDir.canonicalPath
                    if (!outFile.canonicalPath.startsWith(canonicalDest)) {
                        entry = zis.nextEntry
                        continue
                    }

                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        BufferedOutputStream(FileOutputStream(outFile), BUFFER_SIZE).use { bos ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var len: Int
                            while (zis.read(buffer).also { len = it } != -1) {
                                bos.write(buffer, 0, len)
                                processed += len
                                val progress = ((processed / totalSize) * 100).toInt().coerceIn(0, 100)
                                onProgress(progress)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            onProgress(100)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a ZIP file from a list of files/folders.
     * Processes in chunks to avoid OOM on low-memory devices.
     */
    fun createZip(
        sources: List<File>,
        destZip: File,
        onProgress: (Int) -> Unit = {}
    ): Result<Unit> {
        return try {
            val allFiles = mutableListOf<Pair<File, String>>()
            sources.forEach { source ->
                collectFiles(source, source.name, allFiles)
            }
            val total = allFiles.size.toFloat()
            var done = 0

            ZipOutputStream(BufferedOutputStream(FileOutputStream(destZip), BUFFER_SIZE)).use { zos ->
                allFiles.forEach { (file, entryName) ->
                    if (file.isDirectory) {
                        zos.putNextEntry(ZipEntry("$entryName/"))
                        zos.closeEntry()
                    } else {
                        zos.putNextEntry(ZipEntry(entryName))
                        BufferedInputStream(FileInputStream(file), BUFFER_SIZE).use { bis ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var len: Int
                            while (bis.read(buffer).also { len = it } != -1) {
                                zos.write(buffer, 0, len)
                            }
                        }
                        zos.closeEntry()
                    }
                    done++
                    onProgress(((done / total) * 100).toInt().coerceIn(0, 100))
                }
            }
            onProgress(100)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun collectFiles(file: File, entryName: String, result: MutableList<Pair<File, String>>) {
        result.add(Pair(file, entryName))
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                collectFiles(child, "$entryName/${child.name}", result)
            }
        }
    }

    private fun sanitizeEntryName(name: String): String {
        return name.replace("..", "").replace("//", "/").trimStart('/')
    }
}
