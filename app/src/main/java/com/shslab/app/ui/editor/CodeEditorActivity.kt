package com.shslab.app.ui.editor

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shslab.app.databinding.ActivityCodeEditorBinding
import com.shslab.app.ui.preview.PreviewActivity
import com.shslab.app.utils.FileUtils
import kotlinx.coroutines.*
import java.io.File

class CodeEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCodeEditorBinding
    private var filePath: String = ""
    private var currentFile: File? = null

    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCodeEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: ""
        currentFile = if (filePath.isNotEmpty()) File(filePath) else null

        setupToolbar()
        setupWebView()

        currentFile?.let { loadFile(it) }
    }

    private fun setupToolbar() {
        binding.tvFileName.text = currentFile?.name ?: "Untitled"
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { saveFile() }
        binding.btnLiveRun.setOnClickListener { openPreview() }
        binding.btnUndo.setOnClickListener {
            binding.editorWebView.evaluateJavascript("editor.execCommand('undo');", null)
        }
        binding.btnRedo.setOnClickListener {
            binding.editorWebView.evaluateJavascript("editor.execCommand('redo');", null)
        }
    }

    @SuppressWarnings("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.editorWebView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                setSupportZoom(false)
                builtInZoomControls = false
                cacheMode = WebSettings.LOAD_NO_CACHE
            }
            addJavascriptInterface(EditorBridge(), "AndroidBridge")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    // Page loaded, content will be injected via JS
                }
            }
            loadUrl("file:///android_asset/editor/index.html")
        }
    }

    private fun loadFile(file: File) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val content = FileUtils.readFileChunked(file) ?: ""
            val ext = file.extension.lowercase()
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                val escaped = content
                    .replace("\\", "\\\\")
                    .replace("`", "\\`")
                    .replace("$", "\\$")
                binding.editorWebView.evaluateJavascript(
                    "setContent(`$escaped`, `$ext`);", null
                )
            }
        }
    }

    private fun saveFile() {
        binding.editorWebView.evaluateJavascript("getContent();") { result ->
            if (result != null) {
                val content = result
                    .removeSurrounding("\"")
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\'", "'")
                    .replace("\\\\", "\\")

                lifecycleScope.launch(Dispatchers.IO) {
                    val file = currentFile
                    val success = file != null && FileUtils.writeFile(file, content)
                    withContext(Dispatchers.Main) {
                        if (success) {
                            Toast.makeText(this@CodeEditorActivity, "✅ Saved: ${file?.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@CodeEditorActivity, "❌ Save failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun openPreview() {
        val file = currentFile ?: return
        val dir = file.parentFile ?: return
        val intent = Intent(this, PreviewActivity::class.java).apply {
            putExtra(PreviewActivity.EXTRA_DIR_PATH, dir.absolutePath)
            putExtra(PreviewActivity.EXTRA_FILE_PATH, file.absolutePath)
        }
        startActivity(intent)
    }

    inner class EditorBridge {
        @JavascriptInterface
        fun onSaveShortcut() {
            runOnUiThread { saveFile() }
        }

        @JavascriptInterface
        fun log(msg: String) {
            // Debug logging
        }
    }

    override fun onBackPressed() {
        // Ask to save if modified
        super.onBackPressed()
    }
}
