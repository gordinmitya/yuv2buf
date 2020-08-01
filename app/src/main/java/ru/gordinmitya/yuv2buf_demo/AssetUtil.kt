package ru.gordinmitya.yuv2buf_demo

import android.content.Context
import java.io.File

object AssetUtil {
    fun copyFileToCache(context: Context, fileName: String): File {
        val file = File(context.cacheDir, fileName)
        if (file.parentFile?.exists() != true) {
            file.parentFile?.mkdirs()
        }
        if (file.exists())
            return file
        context.assets.open(fileName).use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }
}