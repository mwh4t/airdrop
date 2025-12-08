package com.example.cp.utils

import android.content.Context
import android.content.res.ColorStateList
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.cp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

object UIUtils {

    // подсветка ошибки на карточке выбора файла
    fun highlightFileSelectionError(
        context: Context,
        cardView: MaterialCardView,
        textView: TextView,
        errorColorRes: Int,
        normalColorRes: Int
    ) {
        val errorColor = ContextCompat
            .getColor(context, errorColorRes)
        val normalColor = ContextCompat
            .getColor(context, normalColorRes)

        cardView.strokeColor = errorColor
        textView.setTextColor(errorColor)

        cardView.postDelayed({
            cardView.strokeColor = normalColor
            textView.setTextColor(normalColor)
        }, 500)
    }

    // обновление отображения выбранного файла
    fun updateFileDisplay(
        context: Context,
        fileName: String?,
        selectedFileNameTextView: TextView,
        receiveButton: MaterialButton
    ) {
        selectedFileNameTextView.text = fileName ?: ""

        Toast.makeText(
            context,
            "Файл выбран: $fileName",
            Toast.LENGTH_SHORT
        ).show()

        receiveButton.isEnabled = false
        receiveButton.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, R.color.gray)
        )
        receiveButton.strokeWidth = 2
    }

    // сброс выбранного файла
    fun clearFileSelection(
        context: Context,
        selectedFileNameTextView: TextView,
        receiveButton: MaterialButton
    ) {
        selectedFileNameTextView.text = ""

        receiveButton.isEnabled = true
        receiveButton.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, R.color.black)
        )
        receiveButton.strokeWidth = 0

        Toast.makeText(
            context,
            "Файл отменён",
            Toast.LENGTH_SHORT
        ).show()
    }
}
