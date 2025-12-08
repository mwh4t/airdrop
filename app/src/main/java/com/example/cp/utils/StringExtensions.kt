package com.example.cp.utils

import android.content.Context
import com.example.cp.R

fun Context.getErrorMessage(error: String?): String {
    return getString(R.string.error, error)
}
