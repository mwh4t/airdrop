package com.example.cp.utils

import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.cp.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class GoogleAuthManager(
    private val activity: AppCompatActivity,
    private val auth: FirebaseAuth,
    private val webClientId: String,
    registry: ActivityResultRegistry
) : DefaultLifecycleObserver {

    private lateinit var googleSignInClient: GoogleSignInClient
    private var onSuccessCallback: (() -> Unit)? = null

    private val googleSignInLauncher: ActivityResultLauncher<android.content.Intent> =
        registry.register(
            "google_sign_in",
            activity,
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken)
            } catch (e: ApiException) {
                Toast.makeText(
                    activity,
                    activity.getErrorMessage(e.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        initializeGoogleSignIn()
    }

    // инициализация Google Sign-In
    private fun initializeGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(activity, gso)
    }

    // запуск процесса Google авторизации
    fun signIn(onSuccess: () -> Unit) {
        onSuccessCallback = onSuccess
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    // авторизация в Firebase через Google токен
    private fun firebaseAuthWithGoogle(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        FirestoreUserManager.saveUser(
                            activity,
                            it.uid,
                            it.email ?: "",
                            it.displayName,
                            "google",
                            onSuccess = { onSuccessCallback?.invoke() }
                        )
                    }
                } else {
                    Toast.makeText(
                        activity,
                        activity.getErrorMessage(
                            task.exception?.message
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}
