package com.example.cp.utils

import android.content.Context
import android.widget.TextView
import androidx.core.content.ContextCompat
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
}
