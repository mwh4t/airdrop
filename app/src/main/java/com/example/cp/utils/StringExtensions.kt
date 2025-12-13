package com.example.cp.utils

import android.content.Context
import androidx.fragment.app.Fragment
import com.example.cp.R

fun Context.getErrorMessage(error: String?): String {
    return getString(R.string.error, error)
}

fun Fragment.getErrorMessage(error: String?): String {
    return requireContext().getString(R.string.error, error)
}
