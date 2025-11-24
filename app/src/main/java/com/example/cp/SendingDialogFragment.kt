package com.example.cp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class SendingDialogFragment : DialogFragment() {

    private var fileName: String? = null

    companion object {
        private const val ARG_FILE_NAME = "file_name"

        fun newInstance(fileName: String): SendingDialogFragment {
            val fragment = SendingDialogFragment()
            val args = Bundle()
            args.putString(ARG_FILE_NAME, fileName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileName = arguments?.getString(ARG_FILE_NAME)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dialog_sending,
            container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recipientIdInput = view.findViewById<TextInputEditText>(R.id.recipientIdInput)
        val cancelButton = view.findViewById<MaterialButton>(R.id.cancelButton)
        val sendButton = view.findViewById<MaterialButton>(R.id.sendDialogButton)

        cancelButton.setOnClickListener {
            dismiss()
        }

        sendButton.setOnClickListener {
            val recipientId = recipientIdInput.text.toString()

            if (recipientId.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Введите ID получателя!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // TODO: отправка файла
                Toast.makeText(
                    requireContext(),
                    "Отправка файла '$fileName' пользователю $recipientId...",
                    Toast.LENGTH_LONG
                ).show()
                dismiss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
