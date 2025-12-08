package com.example.cp.utils

import android.content.Context
import android.content.Intent
import com.example.cp.AuthActivity
import com.google.firebase.auth.FirebaseAuth

object AuthUtils {

    // авторизован ли пользователь
    fun isUserLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    // получение текущего пользователя
    fun getCurrentUser() = FirebaseAuth.getInstance()
        .currentUser

    // выход из системы
    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }

    // переход на экран авторизации
    fun navigateToAuth(context: Context) {
        val intent = Intent(
            context,
            AuthActivity::class.java
        )
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }
}
