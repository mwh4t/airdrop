package com.example.cp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ReceivingDialogFragment : DialogFragment() {

    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var loadingText: TextView
    private lateinit var buttonsLayout: LinearLayout
    private lateinit var senderIdInput: TextInputEditText

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
        return inflater.inflate(R.layout.fragment_dialog_receiving, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        senderIdInput = view.findViewById(R.id.senderIdInput)
        val cancelButton = view.findViewById<MaterialButton>(R.id.cancelReceiveButton)
        val receiveButton = view.findViewById<MaterialButton>(R.id.receiveDialogButton)
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
                    "Введите ID отправителя!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
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

        // заглушка
        Toast.makeText(
            requireContext(),
            "Получение файла от пользователя $senderId...",
            Toast.LENGTH_LONG
        ).show()

        // результат
        Handler(Looper.getMainLooper()).postDelayed({
            Toast.makeText(
                requireContext(),
                "Файл успешно получен! (заглушка)",
                Toast.LENGTH_LONG
            ).show()
            dismiss()
        }, 3000)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
