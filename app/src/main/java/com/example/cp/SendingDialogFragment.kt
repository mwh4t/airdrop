package com.example.cp

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.cp.utils.AuthUtils
import com.example.cp.utils.FileTransferManager
import com.example.cp.utils.FirestoreConstants.Collections
import com.example.cp.utils.FirestoreConstants.Fields
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class SendingDialogFragment : DialogFragment() {

    private var fileName: String? = null
    private var fileUri: Uri? = null
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var loadingText: TextView
    private lateinit var buttonsLayout: LinearLayout
    private lateinit var recipientIdInput: TextInputEditText
    private var onFileSentListener: OnFileSentListener? = null

    interface OnFileSentListener {
        fun onFileSent()
    }

    fun setOnFileSentListener(listener: OnFileSentListener) {
        this.onFileSentListener = listener
    }

    companion object {
        private const val ARG_FILE_NAME = "file_name"
        private const val ARG_FILE_URI = "file_uri"

        fun newInstance(fileName: String, fileUri: Uri):
                SendingDialogFragment {
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

        recipientIdInput = view.findViewById(R.id.recipientIdInput)
        val cancelButton = view.findViewById<MaterialButton>(
            R.id.cancelButton
        )
        val sendButton = view.findViewById<MaterialButton>(
            R.id.sendDialogButton
        )
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
        loadingText = view.findViewById(R.id.loadingText)
        buttonsLayout = view.findViewById(R.id.buttonsLayout)

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
                        getString(
                            R.string.id_must_contain_exactly_10_digits
                        ),
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
                    // показать индикатор загрузки
                    loadingProgressBar.visibility = View.VISIBLE
                    loadingText.visibility = View.VISIBLE
                    buttonsLayout.visibility = View.GONE
                    recipientIdInput.isEnabled = false

                    // поиск получателя по ID и загрузка файла
                    findReceiverAndUploadFile(recipientId)
                }
            }
        }
    }

    // поиск получателя и загрузка файла
    private fun findReceiverAndUploadFile(recipientNumericId: String) {
        // поиск получателя в Firestore по ID
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection(Collections.USERS)
            .whereEqualTo(Fields.ID, recipientNumericId)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                when {
                    documents.isEmpty -> {
                        showErrorAndRestoreUI(
                            getString(R.string.user_has_not_been_found)
                        )
                    }

                    else -> {
                        // получение UID получателя
                        val receiverFirebaseUid = documents.documents[0]
                            .getString(Fields.UID)
                        if (receiverFirebaseUid != null) {
                            uploadFile(receiverFirebaseUid)
                        } else {
                            showErrorAndRestoreUI(
                                getString(
                                    R.string.error,
                                    getString(
                                        R.string.user_has_not_been_found
                                    )
                                )
                            )
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                showErrorAndRestoreUI("${e.message}")
            }
    }

    // показ ошибки и восстановление UI
    private fun showErrorAndRestoreUI(message: String) {
        loadingProgressBar.visibility = View.GONE
        loadingText.visibility = View.GONE
        buttonsLayout.visibility = View.VISIBLE
        recipientIdInput.isEnabled = true

        Toast.makeText(
            requireContext(),
            getString(R.string.error, message),
            Toast.LENGTH_SHORT
        ).show()
    }

    @SuppressLint("SetTextI18n")
    private fun uploadFile(receiverFirebaseUid: String) {
        val currentUser = AuthUtils.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(
                requireContext(),
                getString(
                    R.string.error,
                    getString(R.string.user_is_not_logged_in)
                ),
                Toast.LENGTH_SHORT
            ).show()
            dismiss()
            return
        }

        // проверка размера файла
        val fileSize = getFileSize(fileUri!!)
        val maxSizeBytes = 5 * 1024 * 1024L // 5 мб

        if (fileSize > maxSizeBytes) {
            showErrorAndRestoreUI(
                getString(
                    R.string.file_size_exceeds_limit
                )
            )
            return
        }

        FileTransferManager.uploadFile(
            context = requireContext(),
            fileUri = fileUri!!,
            fileName = fileName ?: "unknown",
            senderUid = currentUser.uid,
            receiverUid = receiverFirebaseUid,
            onProgress = { progress ->
                loadingText.text = getString(
                    R.string.uploading_a_file
                ) + " $progress%"
            },
            onSuccess = { _ ->
                Toast.makeText(
                    requireContext(),
                    getString(R.string.file_has_been_sent_successfully),
                    Toast.LENGTH_LONG
                ).show()
                onFileSentListener?.onFileSent()

                // закрытие диалога
                view?.postDelayed({
                    if (isAdded && !isDetached) {
                        dismiss()
                    }
                }, 1000)
            },
            onFailure = { e ->
                loadingProgressBar.visibility = View.GONE
                loadingText.visibility = View.GONE
                buttonsLayout.visibility = View.VISIBLE
                recipientIdInput.isEnabled = true

                Toast.makeText(
                    requireContext(),
                    getString(
                        R.string.error,
                        e.message
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    // получение размера файла
    private fun getFileSize(uri: Uri): Long {
        var size = 0L
        try {
            requireContext().contentResolver
                .openFileDescriptor(uri, "r")
                ?.use { descriptor ->
                    size = descriptor.statSize
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return size
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
