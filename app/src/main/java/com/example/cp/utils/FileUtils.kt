package com.example.cp.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

object FileUtils {

    // получение имени файла из URI
    fun getFileName(context: Context, uri: Uri): String {
        var fileName = "unknown"
        val cursor = context.contentResolver.query(uri,
            null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }
}
