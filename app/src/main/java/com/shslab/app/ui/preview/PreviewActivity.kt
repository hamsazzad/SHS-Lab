package com.shslab.app.ui.preview

import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shslab.app.databinding.ActivityPreviewBinding
import com.shslab.app.utils.LocalHttpServer
import java.io.File

class PreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreviewBinding
    private var httpServer: LocalHttpServer? = null
    private val SERVER_PORT = 18080

    companion object {
        const val EXTRA_DIR_PATH = "extra_dir_path"
        const val EXTRA_FILE_PATH = "extra_file_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dirPath = intent.getStringExtra(EXTRA_DIR_PATH) ?: ""
        val filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: ""

        setupToolbar(dirPath, filePath)
        setupWebView()
        startServer(dirPath, filePath)
    }

    private fun setupToolbar(dirPath: String, filePath: String) {
        val dir = File(dirPath)
        binding.tvTitle.text = "Preview: ${dir.name}"
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener {
            binding.webView.reload()
        }
        binding.btnOpenBrowser.setOnClickListener {
            Toast.makeText(this, "Server running at http://localhost:$SERVER_PORT", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressWarnings("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                loadWithOverviewMode = true
                useWideViewPort = true
            }
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    binding.progressBar.visibility = View.VISIBLE
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvUrl.text = url ?: ""
                }
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    binding.progressBar.visibility = View.GONE
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    binding.progressBar.progress = newProgress
                }
            }
        }
    }

    private fun startServer(dirPath: String, filePath: String) {
        val dir = File(dirPath)
        if (!dir.exists()) {
            Toast.makeText(this, "Directory not found", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            httpServer?.stop()
            httpServer = LocalHttpServer(dir, SERVER_PORT)
            httpServer?.start()

            val targetFile = if (filePath.isNotEmpty()) File(filePath) else null
            val url = if (targetFile != null && targetFile.exists()) {
                val relativePath = targetFile.relativeTo(dir).path
                "http://localhost:$SERVER_PORT/$relativePath"
            } else {
                "http://localhost:$SERVER_PORT/"
            }

            binding.tvUrl.text = url
            binding.webView.loadUrl(url)
        } catch (e: Exception) {
            // Fallback: use file:// if server fails
            val file = if (filePath.isNotEmpty()) File(filePath) else File(dir, "index.html")
            if (file.exists()) {
                binding.webView.loadUrl("file://${file.absolutePath}")
                binding.tvUrl.text = "file://${file.absolutePath}"
            } else {
                Toast.makeText(this, "Cannot preview: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        httpServer?.stop()
        httpServer = null
        binding.webView.destroy()
    }

    override fun onPause() {
        super.onPause()
        binding.webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
    }
}
