package com.example.cp.utils

import android.content.Context
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

object FirestoreUserManager {

    private val firestore = FirebaseFirestore.getInstance()

    // сохранение данных пользователя в Firestore
    fun saveUser(
        context: Context,
        userId: String,
        email: String,
        displayName: String?,
        authProvider: String,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        val userRef = firestore.collection("users")
            .document(userId)

        // существует ли уже документ пользователя
        userRef.get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    // создание нового документа пользователя
                    val numericId = generateNumericId()
                    val userData = hashMapOf(
                        "uid" to userId,
                        "id" to numericId,
                        "email" to email,
                        "displayName" to (displayName ?: email.substringBefore("@")),
                        "authProvider" to authProvider,
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    userRef.set(userData)
                        .addOnSuccessListener {
                            onSuccess?.invoke()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                context,
                                "Failed to save user data: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            onFailure?.invoke(e)
                        }
                } else {
                    // обновление некоторых полей
                    val updates = hashMapOf<String, Any>(
                        "email" to email,
                        "authProvider" to authProvider
                    )
                    if (displayName != null) {
                        updates["displayName"] = displayName
                    }

                    userRef.update(updates)
                        .addOnSuccessListener { onSuccess?.invoke() }
                        .addOnFailureListener { e -> onFailure?.invoke(e) }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    context, "Failed to check user: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                onFailure?.invoke(e)
            }
    }

    // загрузка числового ID пользователя из Firestore
    fun loadUserId(
        userId: String,
        onSuccess: (String?) -> Unit,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val numericId = document.getString("id")
                    onSuccess(numericId)
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { e ->
                onFailure?.invoke(e)
            }
    }

    // генерация ID
    private fun generateNumericId(): String {
        return (1000000000L..9999999999L).random().toString()
    }
}
