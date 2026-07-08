package com.maidcleaner.data.scanner

import com.maidcleaner.data.model.FileEntry
import com.maidcleaner.data.model.FileType
import com.maidcleaner.data.model.ScanProgress
import com.maidcleaner.data.model.StorageCategory
import com.maidcleaner.util.FileClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageAnalyzerScanner @Inject constructor() {

    fun scan(storageRoot: String): Flow<ScanProgress<Pair<List<StorageCategory>, List<FileEntry>>>> = flow {
        emit(ScanProgress.Scanning(0, "Analyzing storage..."))

        val root = File(storageRoot)
        val categorySizes = mutableMapOf<FileType, Long>()
        FileType.entries.forEach { categorySizes[it] = 0L }

        val allFiles = mutableListOf<FileEntry>()
        val totalSize = root.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }

        var processedSize = 0L

        // Categorize files
        emit(ScanProgress.Scanning(10, "Categorizing files..."))
        root.walkTopDown().maxDepth(6).forEach { file ->
            if (file.isFile) {
                processedSize += file.length()
                val progress = if (totalSize > 0) {
                    ((processedSize * 80) / totalSize).toInt().coerceAtMost(80)
                } else 80
                if (processedSize % (10 * 1024 * 1024) < file.length()) {
                    emit(ScanProgress.Scanning(10 + progress / 10, "Analyzing..."))
                }

                val fileType = FileClassifier.classify(file.name)
                categorySizes[fileType] = (categorySizes[fileType] ?: 0L) + file.length()

                allFiles.add(
                    FileEntry(
                        path = file.absolutePath,
                        name = file.name,
                        size = file.length(),
                        isDirectory = false,
                        lastModified = file.lastModified(),
                        fileType = fileType
                    )
                )
            }
        }

        // Build storage categories with chart colors
        emit(ScanProgress.Scanning(90, "Building categories..."))
        val colors = mapOf(
            FileType.APK to 0xFF1B6B4A,
            FileType.VIDEO to 0xFF3C6472,
            FileType.IMAGE to 0xFF8B6914,
            FileType.AUDIO to 0xFF7B5B8A,
            FileType.DOCUMENT to 0xFF4E6354,
            FileType.ARCHIVE to 0xFFBA1A1A,
            FileType.OTHER to 0xFF666666
        )

        val totalCategorized = categorySizes.values.sum()
        val categories = categorySizes
            .filter { it.value > 0 }
            .map { (type, size) ->
                StorageCategory(
                    name = type.name.lowercase().replaceFirstChar { it.uppercase() },
                    size = size,
                    percentage = if (totalCategorized > 0) (size.toFloat() / totalCategorized) * 100f else 0f,
                    color = colors[type] ?: 0xFF666666
                )
            }
            .sortedByDescending { it.size }

        val largestFiles = allFiles.sortedByDescending { it.size }.take(100)

        emit(ScanProgress.Complete(Pair(categories, largestFiles)))
    }.flowOn(Dispatchers.IO)

    fun drillDown(
        directory: File,
        maxDepth: Int = 3
    ): List<FileEntry> {
        val entries = mutableListOf<FileEntry>()
        if (!directory.exists() || !directory.isDirectory) return entries

        directory.listFiles()?.forEach { file ->
            val fileType = if (file.isFile) FileClassifier.classify(file.name) else FileType.OTHER
            val size = if (file.isFile) file.length() else calculateDirSize(file)

            entries.add(
                FileEntry(
                    path = file.absolutePath,
                    name = file.name,
                    size = size,
                    isDirectory = file.isDirectory,
                    lastModified = file.lastModified(),
                    fileType = fileType
                )
            )
        }

        return entries.sortedByDescending { it.size }
    }

    private fun calculateDirSize(dir: File): Long {
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
