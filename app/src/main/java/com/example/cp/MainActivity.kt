package com.example.cp

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.cp.utils.AuthUtils
import com.example.cp.utils.FileUtils
import com.example.cp.utils.FirestoreUserManager
import com.example.cp.utils.UIUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {
    private var selectedFileUri: Uri? = null
    private var selectedFileName: String? = null

    private lateinit var idValueText: TextView
    private lateinit var fileSelectionCard: MaterialCardView
    private lateinit var fileSelectionText: TextView
    private lateinit var receiveButton: MaterialButton
    private lateinit var sendButton: MaterialButton

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            selectedFileName = FileUtils
                .getFileName(this, it)
            updateFileDisplay()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // проверка авторизации
        if (!AuthUtils.isUserLoggedIn()) {
            AuthUtils.navigateToAuth(this)
            return
        }

        // инициализация UI
        initializeUI()

        // загрузка и отображение ID пользователя
        loadUserId()
    }

    // инициализация UI элементов и обработчиков
    private fun initializeUI() {
        idValueText = findViewById(R.id.idValueText)
        fileSelectionCard = findViewById(R.id.fileSelectionCard)
        fileSelectionText = findViewById(R.id.fileSelectionText)

        receiveButton = findViewById<MaterialButton>(R.id.receiveButton)
        sendButton = findViewById<MaterialButton>(R.id.sendButton)
        val logoutButton = findViewById<ImageButton>(R.id.logoutButton)

        // обработчик выбора файла
        fileSelectionCard.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }

        // обработчик долгого нажатия для сброса выбранного файла
        fileSelectionCard.setOnLongClickListener {
            clearFileSelection()
            true
        }

        // обработчик получения файла
        receiveButton.setOnClickListener {
            handleReceiveFile()
        }

        // обработчик отправки
        sendButton.setOnClickListener {
            handleSendFile()
        }

        // обработчик выхода
        logoutButton.setOnClickListener {
            handleLogout()
        }

        // обработчик копирования ID
        idValueText.setOnClickListener {
            copyIdToClipboard()
        }
    }

    // загрузка ID пользователя
    private fun loadUserId() {
        val currentUser = AuthUtils.getCurrentUser() ?: return

        FirestoreUserManager.loadUserId(
            currentUser.uid,
            onSuccess = { numericId ->
                idValueText.text = numericId ?: "N/A"
            },
            onFailure = { e ->
                Toast.makeText(
                    this,
                    getString(R.string.failed_to_load_id) +
                            e.message,
                    Toast.LENGTH_SHORT
                ).show()
                idValueText.text = "Error"
            }
        )
    }

    // обработка получения файла
    private fun handleReceiveFile() {
        val dialog = ReceivingDialogFragment.newInstance()
        dialog.show(supportFragmentManager, "ReceiveFileDialog")
    }

    // обработка отправки файла
    private fun handleSendFile() {
        if (selectedFileUri == null) {
            highlightFileSelectionError()
        } else {
            val dialog = SendingDialogFragment.newInstance(selectedFileName ?: "")
            dialog.show(supportFragmentManager, "SendFileDialog")
        }
    }

    // обработка выхода из аккаунта
    private fun handleLogout() {
        AuthUtils.signOut()
        AuthUtils.navigateToAuth(this)
    }

    // обновление отображения выбранного файла
    private fun updateFileDisplay() {
        val selectedFileNameTextView = findViewById<TextView>(
            R.id.selectedFileName
        )
        selectedFileNameTextView.text = selectedFileName ?: ""

        Toast.makeText(
            this,
            getString(R.string.file_is_selected) +
                    selectedFileName,
            Toast.LENGTH_SHORT
        ).show()

        receiveButton.isEnabled = false
        receiveButton.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.gray)
        )
        receiveButton.strokeWidth = 2

//        val dialog = SendingDialogFragment.newInstance(selectedFileName ?: "")
//        dialog.show(supportFragmentManager, "SendFileDialog")
    }

    // подсветка ошибки при отсутствии выбранного файла
    private fun highlightFileSelectionError() {
        UIUtils.highlightFileSelectionError(
            this,
            fileSelectionCard,
            fileSelectionText,
            android.R.color.holo_red_dark,
            R.color.black
        )

        Toast.makeText(
            this,
            getString(R.string.select_the_file),
            Toast.LENGTH_SHORT
        ).show()
    }

    // сброс выбранного файла
    private fun clearFileSelection() {
        selectedFileUri = null
        selectedFileName = null

        val selectedFileNameTextView =
            findViewById<TextView>(R.id.selectedFileName)

        UIUtils.clearFileSelection(
            this,
            selectedFileNameTextView,
            receiveButton
        )
    }

    // копирование ID в буфер обмена
    private fun copyIdToClipboard() {
        val id = idValueText.text.toString()
        if (id.isNotEmpty() && id != "N/A" && id != "Error") {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as
                    android.content.ClipboardManager
            val clip = android.content.ClipData
                .newPlainText("User ID", id)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(
                this,
                getString(R.string.id_copied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
