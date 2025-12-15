package com.example.cp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cp.utils.FileTransferManager
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private var historyList:
    List<FileTransferManager.FileTransfer>
) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView
    .ViewHolder(view) {
        val fileNameText: TextView = view
            .findViewById(R.id.fileNameText)
        val transferInfoText: TextView = view
            .findViewById(R.id.transferInfoText)
        val dateText: TextView = view
            .findViewById(R.id.dateText)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_history,
                parent, false
            )
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val transfer = historyList[position]

        holder.fileNameText.text = transfer.fileName
        holder.transferInfoText.text = holder.itemView.context.getString(
            R.string.transfer_info,
            transfer.senderId,
            transfer.receiverId
        )

        // Форматирование даты
        val timestamp = transfer.uploadedAt
        if (timestamp is com.google.firebase.Timestamp) {
            val date = timestamp.toDate()
            val dateFormat = SimpleDateFormat(
                "dd.MM.yyyy HH:mm",
                Locale.getDefault()
            )
            holder.dateText.text = dateFormat.format(date)
        } else {
            holder.dateText.text = ""
        }
    }

    override fun getItemCount(): Int = historyList.size

    fun updateData(newList: List<FileTransferManager.FileTransfer>) {
        historyList = newList
        notifyDataSetChanged()
    }
}
