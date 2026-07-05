package com.example.preader.webview

import android.webkit.MimeTypeMap
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import java.io.File
import java.io.FileInputStream

/**
 * A custom PathHandler for WebViewAssetLoader that serves files from a local directory (java.io.File).
 */
class LocalFileResourceHandler(
    private val baseDir: File
) : WebViewAssetLoader.PathHandler {

    override fun handle(path: String): WebResourceResponse? {
        try {
            val requestedFile = File(baseDir, path)

            // Ensure the requested file is actually inside the base directory (prevent path traversal)
            if (!requestedFile.canonicalPath.startsWith(baseDir.canonicalPath)) {
                return null
            }

            if (requestedFile.isDirectory || !requestedFile.exists()) {
                return null
            }

            val inputStream = FileInputStream(requestedFile)
            val mimeType = getMimeType(requestedFile.name)

            return WebResourceResponse(mimeType, null, inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getMimeType(fileName: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(fileName)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "text/plain"
    }
}
