package com.example.cp

import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.cp.utils.AuthUtils
import com.example.cp.utils.FileTransferManager
import com.example.cp.utils.FirestoreConstants.Collections
import com.example.cp.utils.FirestoreConstants.Fields
import com.example.cp.utils.getErrorMessage
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class SendingDialogFragment : DialogFragment() {

    private var fileName: String? = null
    private var fileUri: Uri? = null
    private var progressDialog: ProgressDialog? = null

    companion object {
        private const val ARG_FILE_NAME = "file_name"
        private const val ARG_FILE_URI = "file_uri"

        fun newInstance(fileName: String, fileUri: Uri): SendingDialogFragment {
            val fragment = SendingDialogFragment()
            val args = Bundle()
            args.putString(ARG_FILE_NAME, fileName)
            args.putParcelable(ARG_FILE_URI, fileUri)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileName = arguments?.getString(ARG_FILE_NAME)
        fileUri = arguments?.getParcelable(ARG_FILE_URI)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_dialog_sending,
            container, false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recipientIdInput = view.findViewById<TextInputEditText>(
            R.id.recipientIdInput
        )
        val cancelButton = view.findViewById<MaterialButton>(
            R.id.cancelButton
        )
        val sendButton = view.findViewById<MaterialButton>(
            R.id.sendDialogButton
        )

        cancelButton.setOnClickListener {
            dismiss()
        }

        sendButton.setOnClickListener {
            val recipientId = recipientIdInput.text.toString().trim()

            when {
                recipientId.isEmpty() -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.enter_receiver_id),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                recipientId.length != 10 || !recipientId.all { it.isDigit() } -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.id_must_contain_exactly_10_digits),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                fileUri == null -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.file_is_not_selected),
                        Toast.LENGTH_SHORT
                    ).show()
                    dismiss()
                }

                else -> {
                    // отключение кнопок во время отправки
                    sendButton.isEnabled = false
                    cancelButton.isEnabled = false

                    // поиск получателя по ID и загрузка файла
                    findReceiverAndUploadFile(
                        recipientId, sendButton, cancelButton
                    )
                }
            }
        }
    }

    // поиск получателя и загрузка файла
    private fun findReceiverAndUploadFile(
        recipientNumericId: String,
        sendButton: MaterialButton,
        cancelButton: MaterialButton
    ) {
        // поиск получателя в Firestore по ID
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection(Collections.USERS)
            .whereEqualTo(Fields.ID, recipientNumericId)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                when {
                    documents.isEmpty -> {
                        showErrorAndEnableButtons(
                            getString(R.string.user_has_not_been_found),
                            sendButton,
                            cancelButton
                        )
                    }

                    else -> {
                        // получение UID получателя
                        val receiverFirebaseUid = documents.documents[0]
                            .getString(Fields.UID)
                        if (receiverFirebaseUid != null) {
                            uploadFile(receiverFirebaseUid)
                        } else {
                            showErrorAndEnableButtons(
                                "Ошибка получения данных пользователя",
                                sendButton,
                                cancelButton
                            )
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                showErrorAndEnableButtons(
                    "${e.message}",
                    sendButton,
                    cancelButton
                )
            }
    }

    // показ ошибки и активации кнопок
    private fun showErrorAndEnableButtons(
        message: String,
        sendButton: MaterialButton,
        cancelButton: MaterialButton
    ) {
        Toast.makeText(
            requireContext(),
            getErrorMessage(message),
            Toast.LENGTH_SHORT
        ).show()
        sendButton.isEnabled = true
        cancelButton.isEnabled = true
    }

    private fun uploadFile(receiverFirebaseUid: String) {
        val currentUser = AuthUtils.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error,
                    getString(R.string.user_is_not_logged_in)),
                Toast.LENGTH_SHORT
            ).show()
            dismiss()
            return
        }

        // создание диалога прогресса
        progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Загрузка файла...")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            setCancelable(false)
            max = 100
            show()
        }

        FileTransferManager.uploadFile(
            context = requireContext(),
            fileUri = fileUri!!,
            fileName = fileName ?: "unknown",
            senderUid = currentUser.uid,
            receiverUid = receiverFirebaseUid,
            onProgress = { progress ->
                progressDialog?.progress = progress
            },
            onSuccess = { _ ->
                progressDialog?.dismiss()
                Toast.makeText(
                    requireContext(),
                    "Файл '$fileName' успешно отправлен!",
                    Toast.LENGTH_LONG
                ).show()
                dismiss()
            },
            onFailure = { e ->
                progressDialog?.dismiss()
                Toast.makeText(
                    requireContext(),
                    "Ошибка загрузки файла: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                dismiss()
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

    override fun onDestroy() {
        super.onDestroy()
        progressDialog?.dismiss()
    }
}
