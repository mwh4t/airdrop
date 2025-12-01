package com.example.cp

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cp.utils.AuthUtils
import com.example.cp.utils.EmailAuthManager
import com.example.cp.utils.GoogleAuthManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var emailAuthManager: EmailAuthManager
    private lateinit var googleAuthManager: GoogleAuthManager

    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // инициализация Firebase Auth
        auth = FirebaseAuth.getInstance()

        // авторизован ли уже пользователь
        if (AuthUtils.isUserLoggedIn()) {
            navigateToMain()
            return
        }

        // инициализация менеджеров авторизации
        emailAuthManager = EmailAuthManager(this, auth)
        googleAuthManager = GoogleAuthManager(
            this,
            auth,
            getString(R.string.default_web_client_id),
            activityResultRegistry
        )
        lifecycle.addObserver(googleAuthManager)

        // инициализация UI элементов
        initializeUI()
    }

    // инициализация UI элементов и обработчиков
    private fun initializeUI() {
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)

        // вход
        findViewById<MaterialButton>(R.id.loginButton)
            .setOnClickListener {
            handleSignIn()
        }

        // регистрация
        findViewById<MaterialButton>(R.id.registerButton)
            .setOnClickListener {
            handleRegister()
        }

        // google авторизация
        findViewById<MaterialButton>(R.id.googleButton)
            .setOnClickListener {
            handleGoogleSignIn()
        }
    }

    // обработка входа через email
    private fun handleSignIn() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        emailAuthManager.signIn(email, password) {
            navigateToMain()
        }
    }

    // обработка регистрации через email
    private fun handleRegister() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        emailAuthManager.register(email, password) {
            navigateToMain()
        }
    }

    // обработка входа через Google
    private fun handleGoogleSignIn() {
        googleAuthManager.signIn {
            navigateToMain()
        }
    }

    // переход на главный экран
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
