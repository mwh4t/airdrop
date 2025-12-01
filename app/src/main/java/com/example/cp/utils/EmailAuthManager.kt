package com.example.cp.utils

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class EmailAuthManager(
    private val activity: AppCompatActivity,
    private val auth: FirebaseAuth
) {

    // вход через email и пароль
    fun signIn(
        email: String,
        password: String,
        onFailure: ((String) -> Unit)? = null,
        onSuccess: () -> Unit
    ) {
        if (!validateInput(email, password)) {
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        FirestoreUserManager.saveUser(
                            activity,
                            it.uid,
                            it.email ?: "",
                            it.displayName,
                            "email",
                            onSuccess = onSuccess
                        )
                    }
                } else {
                    val errorMsg = "Authentication failed: ${task.exception?.message}"
                    Toast.makeText(activity, errorMsg,
                        Toast.LENGTH_SHORT).show()
                    onFailure?.invoke(errorMsg)
                }
            }
    }

    // регистрация через email и пароль
    fun register(
        email: String,
        password: String,
        onFailure: ((String) -> Unit)? = null,
        onSuccess: () -> Unit
    ) {
        if (!validateInput(email, password)) {
            return
        }

        if (password.length < 6) {
            Toast.makeText(
                activity, "Password must be at least 6 characters",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        FirestoreUserManager.saveUser(
                            activity,
                            it.uid,
                            it.email ?: "",
                            it.displayName,
                            "email",
                            onSuccess = {
                                Toast.makeText(
                                    activity,
                                    "Registration successful!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onSuccess()
                            }
                        )
                    }
                } else {
                    val errorMsg = "Registration failed: ${task.exception?.message}"
                    Toast.makeText(activity, errorMsg,
                        Toast.LENGTH_SHORT).show()
                    onFailure?.invoke(errorMsg)
                }
            }
    }

    // валидация введенных данных
    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(activity, "Fill in all fields",
                Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}
