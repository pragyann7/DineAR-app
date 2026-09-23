package com.ps.dinear.util

import android.content.Context
import java.io.File

object ARStorageManager {
    private const val MAX_STORAGE_SIZE = 100 * 1024 * 1024 // 100 MB
    private const val TARGET_SIZE = 70 * 1024 * 1024 // Cleanup down to 70 MB

    fun getCacheSize(context: Context): Long {
        val dir = context.getExternalFilesDir("models") ?: return 0
        if (!dir.exists()) return 0
        return dir.listFiles()?.sumOf { it.length() } ?: 0
    }

    fun clearCache(context: Context) {
        val modelsDir = context.getExternalFilesDir("models")
        if (modelsDir?.exists() == true) {
            modelsDir.listFiles()?.forEach { it.delete() }
        }
        val markersDir = context.getExternalFilesDir("markers")
        if (markersDir?.exists() == true) {
            markersDir.listFiles()?.forEach { it.delete() }
        }
    }

    fun getMarkerFile(context: Context, restaurantId: Int, url: String? = null): File {
        val dir = context.getExternalFilesDir("markers") ?: File(context.filesDir, "markers")
        if (!dir.exists()) dir.mkdirs()
        
        if (url != null) {
            // Use a simplified hash of the URL to support cache busting when the marker is replaced
            val urlHash = url.substringAfterLast("/").substringBeforeLast(".").take(8)
            return File(dir, "marker_${restaurantId}_${urlHash}.imgdb")
        }
        
        return File(dir, "marker_${restaurantId}.imgdb")
    }

    fun checkAndCleanup(context: Context) {
        val dir = context.getExternalFilesDir("models") ?: return
        if (!dir.exists()) return

        val files = dir.listFiles() ?: return
        var currentSize = files.sumOf { it.length() }

        if (currentSize > MAX_STORAGE_SIZE) {
            // Sort by last modified - oldest first
            val sortedFiles = files.sortedBy { it.lastModified() }
            
            for (file in sortedFiles) {
                val fileSize = file.length()
                if (file.delete()) {
                    currentSize -= fileSize
                }
                if (currentSize <= TARGET_SIZE) break
            }
        }
    }

    fun formatSize(size: Long): String {
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0

        return when {
            size >= gb -> String.format("%.2f GB", size / gb)
            size >= mb -> String.format("%.2f MB", size / mb)
            size >= kb -> String.format("%.2f KB", size / kb)
            else -> String.format("%d Bytes", size)
        }
    }
}
