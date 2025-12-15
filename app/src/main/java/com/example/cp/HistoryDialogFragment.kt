package com.example.cp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cp.utils.AuthUtils
import com.example.cp.utils.FileTransferManager
import com.google.android.material.button.MaterialButton

class HistoryDialogFragment : DialogFragment() {

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var emptyHistoryText: TextView
    private lateinit var historyAdapter: HistoryAdapter

    companion object {
        fun newInstance(): HistoryDialogFragment {
            return HistoryDialogFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_dialog_history,
            container, false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        historyRecyclerView = view.findViewById(
            R.id.historyRecyclerView
        )
        emptyHistoryText = view.findViewById(
            R.id.emptyHistoryText
        )
        val closeButton = view.findViewById<MaterialButton>(
            R.id.closeButton
        )

        // настройка RecyclerView
        historyAdapter = HistoryAdapter(emptyList())
        historyRecyclerView.layoutManager = LinearLayoutManager(
            requireContext()
        )
        historyRecyclerView.adapter = historyAdapter

        closeButton.setOnClickListener {
            dismiss()
        }

        loadHistory()
    }

    private fun loadHistory() {
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

        FileTransferManager.getHistory(
            userUid = currentUser.uid,
            onSuccess = { historyList ->
                if (historyList.isEmpty()) {
                    historyRecyclerView.visibility = View.GONE
                    emptyHistoryText.visibility = View.VISIBLE
                } else {
                    historyRecyclerView.visibility = View.VISIBLE
                    emptyHistoryText.visibility = View.GONE
                    historyAdapter.updateData(historyList)
                }
            },
            onFailure = { exception ->
                Toast.makeText(
                    requireContext(),
                    getString(
                        R.string.error,
                        exception.message
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}
