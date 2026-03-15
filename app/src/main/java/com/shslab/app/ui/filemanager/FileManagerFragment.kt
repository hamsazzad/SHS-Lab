package com.shslab.app.ui.filemanager

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.shslab.app.R
import com.shslab.app.databinding.FragmentFileManagerBinding
import com.shslab.app.ui.editor.CodeEditorActivity
import com.shslab.app.ui.preview.PreviewActivity
import com.shslab.app.utils.FileUtils
import com.shslab.app.utils.ZipUtils
import kotlinx.coroutines.*
import java.io.File

class FileManagerFragment : Fragment() {

    private var _binding: FragmentFileManagerBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: FileAdapter
    private var currentDir: File = Environment.getExternalStorageDirectory()
    private val selectedFiles = mutableListOf<File>()

    private val pickFilesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) importFiles(uris)
    }

    private val pickZipLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handleZipImport(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFileManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFab()
        setupToolbar()
        loadDirectory(currentDir)
    }

    private fun setupToolbar() {
        binding.tvPath.text = currentDir.absolutePath
        binding.btnBack.setOnClickListener {
            if (currentDir.parentFile != null &&
                currentDir != Environment.getExternalStorageDirectory()
            ) {
                navigateTo(currentDir.parentFile!!)
            }
        }
        binding.btnImportZip.setOnClickListener {
            pickZipLauncher.launch("application/zip")
        }
        binding.btnImportFiles.setOnClickListener {
            pickFilesLauncher.launch("*/*")
        }
    }

    private fun setupRecyclerView() {
        adapter = FileAdapter(
            onFileClick = { file -> handleFileClick(file) },
            onFileLongClick = { file -> showContextMenu(file) },
            onZipExtract = { file -> showExtractDialog(file) },
            onCompress = { file -> showCompressDialog(listOf(file)) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.recyclerView.setHasFixedSize(true)
    }

    private fun setupFab() {
        binding.fabMenu.setOnClickListener {
            showFabMenu()
        }
    }

    private fun showFabMenu() {
        val options = arrayOf("📁 New Folder", "📄 New File", "🗜️ Compress Selected")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showCreateDialog(isFolder = true)
                    1 -> showCreateDialog(isFolder = false)
                    2 -> {
                        if (selectedFiles.isEmpty()) {
                            Toast.makeText(requireContext(), "Long-press files to select", Toast.LENGTH_SHORT).show()
                        } else {
                            showCompressDialog(selectedFiles.toList())
                        }
                    }
                }
            }
            .show()
    }

    private fun loadDirectory(dir: File) {
        currentDir = dir
        binding.tvPath.text = dir.absolutePath
        lifecycleScope.launch(Dispatchers.IO) {
            val files = try {
                dir.listFiles()
                    ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                    ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            withContext(Dispatchers.Main) {
                adapter.submitList(files)
                binding.tvEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun navigateTo(dir: File) {
        selectedFiles.clear()
        adapter.clearSelection()
        loadDirectory(dir)
    }

    private fun handleFileClick(file: File) {
        if (file.isDirectory) {
            navigateTo(file)
        } else if (FileUtils.isZipFile(file)) {
            showZipOptions(file)
        } else if (FileUtils.isCodeFile(file)) {
            openInEditor(file)
        } else {
            FileUtils.openFileExternal(requireContext(), file)
        }
    }

    private fun showZipOptions(file: File) {
        val options = arrayOf("📂 Extract Here", "📂 Extract to Folder", "✏️ Rename", "🗑️ Delete")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(file.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> extractZipHere(file)
                    1 -> showExtractDialog(file)
                    2 -> showRenameDialog(file)
                    3 -> confirmDelete(file)
                }
            }
            .show()
    }

    private fun showContextMenu(file: File) {
        val options = arrayOf("✏️ Rename", "🗑️ Delete", "🗜️ Compress", "👁️ Preview")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(file.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameDialog(file)
                    1 -> confirmDelete(file)
                    2 -> showCompressDialog(listOf(file))
                    3 -> openPreview(file)
                }
            }
            .show()
    }

    private fun showCreateDialog(isFolder: Boolean) {
        val title = if (isFolder) "Create Folder" else "Create File"
        val hint = if (isFolder) "Folder name" else "File name (e.g. index.html)"
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            this.hint = hint
            setPadding(48, 24, 48, 24)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val newItem = File(currentDir, name)
                        val success = if (isFolder) newItem.mkdir() else newItem.createNewFile()
                        withContext(Dispatchers.Main) {
                            if (success) {
                                loadDirectory(currentDir)
                                Toast.makeText(requireContext(), "$name created", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Failed to create", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRenameDialog(file: File) {
        val input = EditText(requireContext()).apply {
            setText(file.name)
            inputType = InputType.TYPE_CLASS_TEXT
            selectAll()
            setPadding(48, 24, 48, 24)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rename")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty() && newName != file.name) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val dest = File(file.parent, newName)
                        val success = file.renameTo(dest)
                        withContext(Dispatchers.Main) {
                            if (success) {
                                loadDirectory(currentDir)
                                Toast.makeText(requireContext(), "Renamed to $newName", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Rename failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(file: File) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete")
            .setMessage("Delete '${file.name}'? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val success = FileUtils.deleteRecursive(file)
                    withContext(Dispatchers.Main) {
                        if (success) {
                            loadDirectory(currentDir)
                            Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun extractZipHere(file: File) {
        showProgress("Extracting...")
        lifecycleScope.launch(Dispatchers.IO) {
            val result = ZipUtils.extractZip(file, currentDir) { progress ->
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.progressBar.progress = progress
                }
            }
            withContext(Dispatchers.Main) {
                hideProgress()
                if (result.isSuccess) {
                    loadDirectory(currentDir)
                    Toast.makeText(requireContext(), "Extracted successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Extract failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showExtractDialog(file: File) {
        val input = EditText(requireContext()).apply {
            setText(file.nameWithoutExtension)
            hint = "Destination folder name"
            setPadding(48, 24, 48, 24)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Extract to Folder")
            .setView(input)
            .setPositiveButton("Extract") { _, _ ->
                val folderName = input.text.toString().trim()
                if (folderName.isNotEmpty()) {
                    val destDir = File(currentDir, folderName)
                    destDir.mkdirs()
                    showProgress("Extracting...")
                    lifecycleScope.launch(Dispatchers.IO) {
                        val result = ZipUtils.extractZip(file, destDir) { progress ->
                            lifecycleScope.launch(Dispatchers.Main) {
                                binding.progressBar.progress = progress
                            }
                        }
                        withContext(Dispatchers.Main) {
                            hideProgress()
                            if (result.isSuccess) {
                                loadDirectory(currentDir)
                                Toast.makeText(requireContext(), "Extracted to $folderName", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Extract failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCompressDialog(files: List<File>) {
        val input = EditText(requireContext()).apply {
            hint = "ZIP file name (without .zip)"
            setPadding(48, 24, 48, 24)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create ZIP")
            .setView(input)
            .setPositiveButton("Compress") { _, _ ->
                val zipName = input.text.toString().trim()
                if (zipName.isNotEmpty()) {
                    val destZip = File(currentDir, "$zipName.zip")
                    showProgress("Compressing...")
                    lifecycleScope.launch(Dispatchers.IO) {
                        val result = ZipUtils.createZip(files, destZip) { progress ->
                            lifecycleScope.launch(Dispatchers.Main) {
                                binding.progressBar.progress = progress
                            }
                        }
                        withContext(Dispatchers.Main) {
                            hideProgress()
                            selectedFiles.clear()
                            adapter.clearSelection()
                            if (result.isSuccess) {
                                loadDirectory(currentDir)
                                Toast.makeText(requireContext(), "Created $zipName.zip", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Compress failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun importFiles(uris: List<Uri>) {
        showProgress("Importing files...")
        lifecycleScope.launch(Dispatchers.IO) {
            var success = 0
            uris.forEachIndexed { i, uri ->
                val fileName = FileUtils.getFileName(requireContext(), uri) ?: "file_$i"
                val dest = File(currentDir, fileName)
                val result = FileUtils.copyUriToFile(requireContext(), uri, dest)
                if (result) success++
                withContext(Dispatchers.Main) {
                    binding.progressBar.progress = ((i + 1) * 100 / uris.size)
                }
            }
            withContext(Dispatchers.Main) {
                hideProgress()
                loadDirectory(currentDir)
                Toast.makeText(requireContext(), "Imported $success/${uris.size} files", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleZipImport(uri: Uri) {
        val fileName = FileUtils.getFileName(requireContext(), uri) ?: "archive.zip"
        val dest = File(currentDir, fileName)
        showProgress("Importing ZIP...")
        lifecycleScope.launch(Dispatchers.IO) {
            FileUtils.copyUriToFile(requireContext(), uri, dest)
            withContext(Dispatchers.Main) {
                hideProgress()
                loadDirectory(currentDir)
                Toast.makeText(requireContext(), "ZIP imported: $fileName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openInEditor(file: File) {
        val intent = Intent(requireContext(), CodeEditorActivity::class.java).apply {
            putExtra(CodeEditorActivity.EXTRA_FILE_PATH, file.absolutePath)
        }
        startActivity(intent)
    }

    private fun openPreview(file: File) {
        val rootDir = if (file.isDirectory) file else file.parentFile ?: file
        val intent = Intent(requireContext(), PreviewActivity::class.java).apply {
            putExtra(PreviewActivity.EXTRA_DIR_PATH, rootDir.absolutePath)
            if (!file.isDirectory) putExtra(PreviewActivity.EXTRA_FILE_PATH, file.absolutePath)
        }
        startActivity(intent)
    }

    private fun showProgress(message: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.progress = 0
        binding.tvProgress.visibility = View.VISIBLE
        binding.tvProgress.text = message
    }

    private fun hideProgress() {
        binding.progressBar.visibility = View.GONE
        binding.tvProgress.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
