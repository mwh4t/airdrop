package com.example.cp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.cp.utils.AuthUtils
import com.example.cp.utils.FileTransferManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ReceivingDialogFragment : DialogFragment() {

    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var loadingText: TextView
    private lateinit var buttonsLayout: LinearLayout
    private lateinit var senderIdInput: TextInputEditText
    private var pendingSenderId: String? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingSenderId?.let { startReceiving(it) }
            pendingSenderId = null
        } else {
            Toast.makeText(
                requireContext(),
                getString(
                    R.string.permission_is_required_to_save_files
                ),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        fun newInstance(): ReceivingDialogFragment {
            return ReceivingDialogFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_dialog_receiving,
            container, false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        senderIdInput = view.findViewById(R.id.senderIdInput)
        val cancelButton = view.findViewById<MaterialButton>(
            R.id.cancelReceiveButton
        )
        val receiveButton = view.findViewById<MaterialButton>(
            R.id.receiveDialogButton
        )
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
        loadingText = view.findViewById(R.id.loadingText)
        buttonsLayout = view.findViewById(R.id.buttonsLayout)

        cancelButton.setOnClickListener {
            dismiss()
        }

        receiveButton.setOnClickListener {
            val senderId = senderIdInput.text.toString()

            if (senderId.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.enter_sender_id_lk),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // проверка и запрос разрешений
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        pendingSenderId = senderId
                        requestPermissionLauncher.launch(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                        return@setOnClickListener
                    }
                }
                startReceiving(senderId)
            }
        }
    }

    private fun startReceiving(senderId: String) {
        // индикатор загрузки
        loadingProgressBar.visibility = View.VISIBLE
        loadingText.visibility = View.VISIBLE
        buttonsLayout.visibility = View.GONE
        senderIdInput.isEnabled = false

        val currentUser = AuthUtils.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(
                requireContext(),
                getString(
                    R.string.error,
                    getString(
                        R.string.user_is_not_logged_in
                    )
                ),
                Toast.LENGTH_SHORT
            ).show()
            dismiss()
            return
        }

        // получение файла
        FileTransferManager.receiveFile(
            context = requireContext(),
            receiverUid = currentUser.uid,
            senderId = senderId,
            onProgress = { status ->
                loadingText.text = status
            },
            onSuccess = { fileTransfer ->
                Toast.makeText(
                    requireContext(),
                    getString(
                        R.string.file_was_received_successfully
                    ),
                    Toast.LENGTH_LONG
                ).show()

                // закрытие диалога
                view?.postDelayed({
                    if (isAdded && !isDetached) {
                        dismiss()
                    }
                }, 1000)
            },
            onFailure = { exception ->
                // скрытие индикатора загрузки
                loadingProgressBar.visibility = View.GONE
                loadingText.visibility = View.GONE
                buttonsLayout.visibility = View.VISIBLE
                senderIdInput.isEnabled = true

                Toast.makeText(
                    requireContext(),
                    getString(
                        R.string.error,
                        exception.message
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
