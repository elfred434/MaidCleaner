package com.maidcleaner.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object SizeFormatter {
    private val units = arrayOf("B", "KB", "MB", "GB", "TB")
    private val formatter = DecimalFormat("#,##0.#")

    fun format(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val unitIndex = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            .coerceIn(0, units.lastIndex)
        val value = bytes / Math.pow(1024.0, unitIndex.toDouble())
        return "${formatter.format(value)} ${units[unitIndex]}"
    }

    fun formatSpeed(bytesPerSecond: Long): String = "${format(bytesPerSecond)}/s"
}

object DateFormatter {
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))
    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))
    fun formatDateTime(timestamp: Long): String = dateTimeFormat.format(Date(timestamp))

    fun formatRelative(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
            else -> formatDate(timestamp)
        }
    }
}

object HashUtils {
    private const val BUFFER_SIZE = 8192

    /**
     * Compute MD5 hash of a file for duplicate detection.
     * Uses streaming to handle large files efficiently.
     */
    fun md5(file: java.io.File): String {
        val digest = java.security.MessageDigest.getInstance("MD5")
        file.inputStream().buffered(BUFFER_SIZE).use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Quick size + partial hash comparison.
     * First checks file size, then hashes first 4KB + last 4KB
     * before doing a full hash. This avoids hashing entire large files
     * when they clearly differ.
     */
    suspend fun quickHash(file: java.io.File): String {
        val size = file.length()
        val digest = java.security.MessageDigest.getInstance("MD5")
        digest.update(size.toString().toByteArray())

        file.inputStream().buffered().use { input ->
            // Hash first 4KB
            val head = ByteArray(4096)
            val headRead = input.read(head)
            if (headRead > 0) digest.update(head, 0, headRead)

            // Hash last 4KB
            if (size > 8192) {
                input.skip(size - 4096 - headRead)
                val tail = ByteArray(4096)
                val tailRead = input.read(tail)
                if (tailRead > 0) digest.update(tail, 0, tailRead)
            }
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

object FileClassifier {
    private val videoExtensions = setOf(
        "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp", "ts", "mpeg"
    )
    private val audioExtensions = setOf(
        "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus", "amr"
    )
    private val imageExtensions = setOf(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "tiff", "heic", "heif"
    )
    private val documentExtensions = setOf(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv", "odt", "ods"
    )
    private val archiveExtensions = setOf(
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "zst"
    )

    fun classify(fileName: String): com.maidcleaner.data.model.FileType {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when {
            ext in videoExtensions -> com.maidcleaner.data.model.FileType.VIDEO
            ext in audioExtensions -> com.maidcleaner.data.model.FileType.AUDIO
            ext in imageExtensions -> com.maidcleaner.data.model.FileType.IMAGE
            ext in documentExtensions -> com.maidcleaner.data.model.FileType.DOCUMENT
            ext in archiveExtensions -> com.maidcleaner.data.model.FileType.ARCHIVE
            ext == "apk" -> com.maidcleaner.data.model.FileType.APK
            else -> com.maidcleaner.data.model.FileType.OTHER
        }
    }

    fun isJunkFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in setOf("tmp", "temp", "log", "bak", "old", "swp", "cache")
    }
}

object StorageUtils {
    /**
     * Get storage stats for a given storage path.
     */
    fun getStorageStats(path: String): com.maidcleaner.data.model.StorageStats {
        val stat = android.os.StatFs(path)
        val totalBytes = stat.totalBytes
        val freeBytes = stat.availableBytes
        val usedBytes = totalBytes - freeBytes
        return com.maidcleaner.data.model.StorageStats(
            totalBytes = totalBytes,
            usedBytes = usedBytes,
            freeBytes = freeBytes
        )
    }
}
