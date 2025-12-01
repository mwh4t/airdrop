package com.example.cp

import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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

        val sendButton = findViewById<MaterialButton>(R.id.sendButton)
        val receiveButton = findViewById<MaterialButton>(R.id.receiveButton)
        val logoutButton = findViewById<ImageButton>(R.id.logoutButton)

        // обработчик выбора файла
        fileSelectionCard.setOnClickListener {
            filePickerLauncher.launch("*/*")
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
    }

    // загрузка ID пользователя из Firestore
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
                    "Failed to load ID: ${e.message}",
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
            // TODO: логика отправки файла
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
            "Файл выбран: $selectedFileName",
            Toast.LENGTH_SHORT
        ).show()

        // диалог для ввода ID
        val dialog = SendingDialogFragment.newInstance(selectedFileName ?: "")
        dialog.show(supportFragmentManager, "SendFileDialog")
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
            "Выберите файл!",
            Toast.LENGTH_SHORT
        ).show()
    }
}
