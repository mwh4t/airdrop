package com.example.cp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {
    private var selectedFileUri: Uri? = null
    private var selectedFileName: String? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            selectedFileName = getFileName(it)
            updateFileDisplay()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val fileSelectionCard = findViewById<MaterialCardView>(R.id.fileSelectionCard)
        val fileSelectionText = findViewById<TextView>(R.id.fileSelectionText)
        val sendButton = findViewById<MaterialButton>(R.id.sendButton)
        val receiveButton = findViewById<MaterialButton>(R.id.receiveButton)

        // обработчик выбора файла
        fileSelectionCard.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }

        // обработчик получения файла
        receiveButton.setOnClickListener {
            val dialog = ReceivingDialogFragment.newInstance()
            dialog.show(supportFragmentManager, "ReceiveFileDialog")
        }

        // обработчик отправки
        sendButton.setOnClickListener {
            if (selectedFileUri == null) {
                highlightFileSelectionError(
                    fileSelectionCard,
                    fileSelectionText
                )
            } else {
                // TODO: логика отправки файла
            }
        }

        findViewById<ImageButton>(R.id.logoutButton).setOnClickListener {
            val intent = Intent(
                this,
                AuthActivity::class.java
            )
            startActivity(intent)
            finish()
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "unknown"
        val cursor = contentResolver.query(uri, null, null, null, null)
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

    private fun updateFileDisplay() {
        val selectedFileNameTextView = findViewById<TextView>(R.id.selectedFileName)
        selectedFileNameTextView.text = selectedFileName ?: ""

        Toast.makeText(
            this,
            "Файл выбран: $selectedFileName",
            Toast.LENGTH_SHORT
        ).show()

        // диалог для ввода ID
        val dialog = SendingDialogFragment.newInstance(selectedFileName ?: "")
        dialog.show(supportFragmentManager, "SendFileDialog")
    }

    private fun highlightFileSelectionError(cardView: MaterialCardView, textView: TextView) {
        val originalStrokeColor = ContextCompat
            .getColor(this, R.color.black)
        val originalTextColor = ContextCompat
            .getColor(this, R.color.black)
        val errorColor = ContextCompat
            .getColor(this, android.R.color.holo_red_dark)

        cardView.strokeColor = errorColor
        textView.setTextColor(errorColor)

        cardView.postDelayed({
            cardView.strokeColor = originalStrokeColor
            textView.setTextColor(originalTextColor)
        }, 500)

        Toast.makeText(
            this,
            "Выберите файл!",
            Toast.LENGTH_SHORT
        ).show()
    }
}
