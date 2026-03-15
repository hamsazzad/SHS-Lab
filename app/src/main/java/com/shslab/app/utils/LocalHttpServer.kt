package com.shslab.app.utils

import fi.iki.elonen.NanoHTTPD
import java.io.*

class LocalHttpServer(private val rootDir: File, port: Int = 8080) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        var uri = session.uri
        if (uri == "/" || uri.isEmpty()) uri = "/index.html"

        val file = File(rootDir, uri)

        return if (file.exists() && file.isFile) {
            try {
                val mime = getMime(file.extension.lowercase())
                newChunkedResponse(Response.Status.OK, mime, FileInputStream(file))
            } catch (e: Exception) {
                newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: ${e.message}")
            }
        } else if (file.isDirectory) {
            val index = File(file, "index.html")
            if (index.exists()) {
                try {
                    newChunkedResponse(Response.Status.OK, "text/html", FileInputStream(index))
                } catch (e: Exception) {
                    newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error")
                }
            } else {
                serveDirectory(file, uri)
            }
        } else {
            newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404 Not Found: $uri")
        }
    }

    private fun serveDirectory(dir: File, uri: String): Response {
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'>")
        sb.append("<title>SHS Lab - ${dir.name}</title>")
        sb.append("<style>body{font-family:monospace;background:#0d1117;color:#c9d1d9;padding:20px;}")
        sb.append("a{color:#58a6ff;text-decoration:none;display:block;padding:6px 0;}a:hover{color:#79c0ff;}</style></head><body>")
        sb.append("<h2>📁 ${dir.name}</h2>")
        if (uri != "/") sb.append("<a href='..'>⬆ Parent Directory</a><hr>")
        dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))?.forEach { f ->
            val icon = if (f.isDirectory) "📁" else "📄"
            val link = if (uri.endsWith("/")) "$uri${f.name}" else "$uri/${f.name}"
            sb.append("<a href='$link'>$icon ${f.name}</a>")
        }
        sb.append("</body></html>")
        return newFixedLengthResponse(Response.Status.OK, "text/html", sb.toString())
    }

    private fun getMime(ext: String): String = when (ext) {
        "html", "htm" -> "text/html"
        "css" -> "text/css"
        "js" -> "application/javascript"
        "json" -> "application/json"
        "xml" -> "application/xml"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "svg" -> "image/svg+xml"
        "ico" -> "image/x-icon"
        "woff" -> "font/woff"
        "woff2" -> "font/woff2"
        "ttf" -> "font/ttf"
        "txt", "md" -> "text/plain"
        "pdf" -> "application/pdf"
        else -> "application/octet-stream"
    }
}
