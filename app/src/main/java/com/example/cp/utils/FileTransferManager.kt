package com.example.cp.utils

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.example.cp.utils.FirestoreConstants.Collections
import com.example.cp.utils.FirestoreConstants.Fields
import com.example.cp.utils.FirestoreConstants.StoragePaths
import com.example.cp.utils.FirestoreConstants.TransferStatus
import java.util.UUID

object FileTransferManager {

    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // данные о передаче файла
    data class FileTransfer(
        val id: String = "",
        val fileName: String = "",
        val fileSize: Long = 0,
        val fileType: String = "",
        val senderId: String = "",
        val receiverId: String = "",
        val senderUid: String = "",
        val receiverUid: String = "",
        val storageUrl: String = "",
        val uploadedAt: Any = FieldValue.serverTimestamp(),
        val status: String = TransferStatus.PENDING
    )

    // загрузка файла и создание записи
    fun uploadFile(
        context: Context,
        fileUri: Uri,
        fileName: String,
        senderUid: String,
        receiverUid: String,
        onProgress: ((Int) -> Unit)? = null,
        onSuccess: (FileTransfer) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            // получение информации о файле
            val fileSize = getFileSize(context, fileUri)
            val fileType = context.contentResolver.getType(fileUri)
                ?: "application/octet-stream"

            // генерация ID для передачи
            val transferId = UUID.randomUUID().toString()

            // загрузка числовых ID отправителя и получателя
            loadUserNumericIds(
                senderUid = senderUid,
                receiverUid = receiverUid,
                onSuccess = { senderId, receiverId ->
                    // путь в Storage
                    val storageRef = storage.reference
                        .child(StoragePaths.TRANSFERS)
                        .child(transferId)
                        .child(fileName)

                    // загрузка файла
                    val uploadTask = storageRef.putFile(fileUri)

                    uploadTask.addOnProgressListener { taskSnapshot ->
                        val progress = (100.0 * taskSnapshot.bytesTransferred /
                                taskSnapshot.totalByteCount).toInt()
                        onProgress?.invoke(progress)
                    }.addOnSuccessListener {
                        // получение URL загруженного файла
                        storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            // создание записи в Firestore
                            val fileTransfer = FileTransfer(
                                id = transferId,
                                fileName = fileName,
                                fileSize = fileSize,
                                fileType = fileType,
                                senderId = senderId,
                                receiverId = receiverId,
                                senderUid = senderUid,
                                receiverUid = receiverUid,
                                storageUrl = downloadUri.toString(),
                                status = TransferStatus.PENDING
                            )

                            saveTransferToFirestore(
                                fileTransfer,
                                onSuccess = { onSuccess(fileTransfer) },
                                onFailure = onFailure
                            )
                        }.addOnFailureListener { e ->
                            onFailure(e)
                        }
                    }.addOnFailureListener { e ->
                        onFailure(e)
                    }
                },
                onFailure = onFailure
            )
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // загрузка ID пользователей
    private fun loadUserNumericIds(
        senderUid: String,
        receiverUid: String,
        onSuccess: (String, String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val senderTask = firestore.collection(Collections.USERS)
            .document(senderUid)
            .get()

        val receiverTask = firestore.collection(Collections.USERS)
            .document(receiverUid)
            .get()

        Tasks.whenAllSuccess<DocumentSnapshot>(senderTask, receiverTask)
            .addOnSuccessListener { documents ->
                val senderId = documents[0].getString(Fields.ID) ?: "unknown"
                val receiverId = documents[1].getString(Fields.ID) ?: "unknown"
                onSuccess(senderId, receiverId)
            }
            .addOnFailureListener(onFailure)
    }

    // сохранение записи о передаче
    private fun saveTransferToFirestore(
        transfer: FileTransfer,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val batch = firestore.batch()

        // files
        val fileRef = firestore.collection(Collections.FILES)
            .document(transfer.id)
        val fileData = hashMapOf(
            Fields.FILE_NAME to transfer.fileName,
            Fields.FILE_SIZE to transfer.fileSize,
            Fields.FILE_TYPE to transfer.fileType,
            Fields.SENDER_ID to transfer.senderId,
            Fields.RECEIVER_ID to transfer.receiverId,
            Fields.SENDER_UID to transfer.senderUid,
            Fields.RECEIVER_UID to transfer.receiverUid,
            Fields.STORAGE_URL to transfer.storageUrl,
            Fields.UPLOADED_AT to FieldValue.serverTimestamp(),
            Fields.STATUS to transfer.status
        )
        batch.set(fileRef, fileData)

        // transfers
        val transferRef = firestore.collection(Collections.TRANSFERS)
            .document(transfer.id)
        val transferData = hashMapOf(
            Fields.FILE_ID to transfer.id,
            Fields.SENDER_ID to transfer.senderId,
            Fields.RECEIVER_ID to transfer.receiverId,
            Fields.SENDER_UID to transfer.senderUid,
            Fields.RECEIVER_UID to transfer.receiverUid,
            Fields.STATUS to transfer.status,
            Fields.TIMESTAMP to FieldValue.serverTimestamp()
        )
        batch.set(transferRef, transferData)

        // атомарная запись обоих документов
        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onFailure)
    }

    // получение размера файла
    private fun getFileSize(context: Context, uri: Uri): Long {
        var size = 0L
        try {
            context.contentResolver.openFileDescriptor(uri, "r")
                ?.use { descriptor ->
                    size = descriptor.statSize
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return size
    }

    // форматирование размера файла
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}
