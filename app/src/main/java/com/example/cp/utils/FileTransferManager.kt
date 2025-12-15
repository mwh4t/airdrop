package com.example.cp.utils

import android.content.Context
import android.net.Uri
import com.example.cp.R
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

            // загрузка ID отправителя и получателя
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

    // загрузка ID
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

        // атомарная запись документов
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

    // получение файла по ID отправителя
    fun receiveFile(
        context: Context,
        receiverUid: String,
        senderId: String,
        onProgress: ((String) -> Unit)? = null,
        onSuccess: (FileTransfer) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        onProgress?.invoke(context.getString(R.string.file_search))

        // получение UID по ID
        firestore.collection(Collections.USERS)
            .whereEqualTo(Fields.ID, senderId)
            .get()
            .addOnSuccessListener { userSnapshot ->
                if (userSnapshot.isEmpty) {
                    onFailure(
                        Exception(
                            context.getString(
                                R.string.user_has_not_been_found
                            )
                        )
                    )
                    return@addOnSuccessListener
                }

                val senderUid = userSnapshot.documents[0].getString(Fields.UID)
                if (senderUid == null) {
                    onFailure(
                        Exception(
                            context.getErrorMessage(
                                context.getString(
                                    R.string.senders_data_has_not_been_received
                                )
                            )
                        )
                    )
                    return@addOnSuccessListener
                }

                // поиск pending файла
                onProgress?.invoke(
                    context.getString(
                        R.string.waiting_for_a_file
                    )
                )

                firestore.collection(Collections.FILES)
                    .whereEqualTo(Fields.SENDER_UID, senderUid)
                    .whereEqualTo(Fields.RECEIVER_UID, receiverUid)
                    .whereEqualTo(Fields.STATUS, TransferStatus.PENDING)
                    .get()
                    .addOnSuccessListener { filesSnapshot ->
                        if (filesSnapshot.isEmpty) {
                            onFailure(
                                Exception(
                                    context.getString(
                                        R.string.file_was_not_found
                                    )
                                )
                            )
                            return@addOnSuccessListener
                        }

                        val fileDoc = filesSnapshot.documents[0]
                        val fileId = fileDoc.id
                        val fileName = fileDoc.getString(Fields.FILE_NAME) ?: "file"
                        val storageUrl = fileDoc.getString(Fields.STORAGE_URL)

                        if (storageUrl == null) {
                            onFailure(
                                Exception(
                                    context.getErrorMessage(
                                        context.getString(
                                            R.string.link_to_the_file_was_not_received
                                        )
                                    )
                                )
                            )
                            return@addOnSuccessListener
                        }

                        // скачивание
                        onProgress?.invoke(
                            context.getString(
                                R.string.uploading_a_file
                            )
                        )
                        downloadFile(
                            context = context,
                            storageUrl = storageUrl,
                            fileName = fileName,
                            onDownloadProgress = { progress ->
                                onProgress?.invoke("Загрузка: $progress%")
                            },
                            onSuccess = {
                                // обновление статуса на received
                                updateFileStatus(
                                    fileId = fileId,
                                    status = TransferStatus.RECEIVED,
                                    onSuccess = {
                                        // создание объекта для возврата
                                        val fileTransfer = FileTransfer(
                                            id = fileId,
                                            fileName = fileDoc.getString(Fields.FILE_NAME) ?: "",
                                            fileSize = fileDoc.getLong(Fields.FILE_SIZE) ?: 0,
                                            fileType = fileDoc.getString(Fields.FILE_TYPE) ?: "",
                                            senderId = fileDoc.getString(Fields.SENDER_ID) ?: "",
                                            receiverId = fileDoc.getString(Fields.RECEIVER_ID)
                                                ?: "",
                                            senderUid = fileDoc.getString(Fields.SENDER_UID) ?: "",
                                            receiverUid = fileDoc.getString(Fields.RECEIVER_UID)
                                                ?: "",
                                            storageUrl = storageUrl,
                                            status = TransferStatus.RECEIVED
                                        )
                                        onSuccess(fileTransfer)
                                    },
                                    onFailure = onFailure
                                )
                            },
                            onFailure = onFailure
                        )
                    }
                    .addOnFailureListener(onFailure)
            }
            .addOnFailureListener(onFailure)
    }

    // скачивание файла из Storage
    private fun downloadFile(
        context: Context,
        storageUrl: String,
        fileName: String,
        onDownloadProgress: ((Int) -> Unit)? = null,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val storageRef = storage.getReferenceFromUrl(
                storageUrl
            )

            // создание временного файла
            val tempFile = java.io.File.createTempFile(
                "download_",
                ".tmp", context.cacheDir
            )

            storageRef.getFile(tempFile)
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred /
                            taskSnapshot.totalByteCount).toInt()
                    onDownloadProgress?.invoke(progress)
                }
                .addOnSuccessListener {
                    // копирование файла в Downloads
                    saveToDownloads(
                        context, tempFile,
                        fileName, onSuccess, onFailure
                    )
                }
                .addOnFailureListener { e ->
                    tempFile.delete()
                    onFailure(e)
                }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // сохранение файла в Downloads
    private fun saveToDownloads(
        context: Context,
        sourceFile: java.io.File,
        fileName: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // MediaStore
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(
                        android.provider.MediaStore.MediaColumns.DISPLAY_NAME,
                        fileName
                    )
                    put(
                        android.provider.MediaStore.MediaColumns.MIME_TYPE,
                        fileName.getMimeType()
                    )
                    put(
                        android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    )
                }

                val uri = resolver.insert(
                    android.provider.MediaStore.Downloads
                        .EXTERNAL_CONTENT_URI,
                    contentValues
                )

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        sourceFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    sourceFile.delete()

                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.file_is_saved),
                        android.widget.Toast.LENGTH_LONG
                    ).show()

                    // открытие
                    openDownloadedFile(context, uri, fileName)
                    onSuccess()
                } else {
                    sourceFile.delete()
                    onFailure(
                        Exception(
                            context.getString(
                                R.string.file_could_not_be_created
                            )
                        )
                    )
                }
            } else {
                // прямой доступ
                val downloadsDir = android.os.Environment
                    .getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    )
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }

                val destinationFile = java.io.File(
                    downloadsDir, fileName
                )
                sourceFile.copyTo(
                    destinationFile,
                    overwrite = true
                )
                sourceFile.delete()

                // уведомление системы о новом файле
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE
                )
                intent.data = android.net.Uri.fromFile(destinationFile)
                context.sendBroadcast(intent)

                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.file_is_saved),
                    android.widget.Toast.LENGTH_LONG
                ).show()

                // открытие через FileProvider
                android.os.Handler(android.os.Looper.getMainLooper())
                    .postDelayed({
                        openFileFromPath(context, destinationFile)
                    }, 3000)

                onSuccess()
            }
        } catch (e: Exception) {
            sourceFile.delete()
            onFailure(e)
        }
    }

    // открытие из MediaStore
    private fun openDownloadedFile(
        context: Context, uri: android.net.Uri,
        fileName: String
    ) {
        try {
            if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.Q
            ) {
                val intent = android.content.Intent(
                    android.content
                        .Intent.ACTION_VIEW
                ).apply {
                    setDataAndType(
                        android.provider.MediaStore.Downloads
                            .EXTERNAL_CONTENT_URI,
                        "resource/folder"
                    )
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }

                try {
                    context.startActivity(intent)
                    return
                } catch (e: android.content.ActivityNotFoundException) {
                }
            }

            openFilesApp(context)
        } catch (e: Exception) {
            e.printStackTrace()
            showDownloadedMessage(context)
        }
    }

    // открытие приложения Файлы
    private fun openFilesApp(context: Context) {
        try {
            val filesIntent = android.content.Intent(
                android.content
                    .Intent.ACTION_GET_CONTENT
            ).apply {
                type = "*/*"
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(filesIntent)
        } catch (e: Exception) {
            showDownloadedMessage(context)
        }
    }

    // открытие файла по пути
    private fun openFileFromPath(context: Context, file: java.io.File) {
        try {
            val downloadsDir = android.os.Environment
                .getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                downloadsDir
            )

            val intent = android.content.Intent(
                android.content
                    .Intent.ACTION_VIEW
            ).apply {
                setDataAndType(uri, "resource/folder")
                flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }

            try {
                context.startActivity(intent)
            } catch (e: android.content.ActivityNotFoundException) {
                openFilesApp(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showDownloadedMessage(context)
        }
    }

    // сообщение о сохранении файла
    private fun showDownloadedMessage(context: Context) {
        android.widget.Toast.makeText(
            context,
            context.getString(R.string.file_is_saved),
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    // обновление статуса файла
    private fun updateFileStatus(
        fileId: String,
        status: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val batch = firestore.batch()

        // обновление в коллекции files
        val fileRef = firestore.collection(
            Collections.FILES
        )
            .document(fileId)
        batch.update(fileRef, Fields.STATUS, status)

        // обновление в коллекции transfers
        val transferRef = firestore.collection(
            Collections.TRANSFERS
        )
            .document(fileId)
        batch.update(
            transferRef,
            Fields.STATUS, status
        )

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onFailure)
    }

    // получение истории передач
    fun getHistory(
        userUid: String,
        onSuccess: (List<FileTransfer>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection(Collections.FILES)
            .whereEqualTo(Fields.SENDER_UID, userUid)
            .get()
            .addOnSuccessListener { sentSnapshot ->
                firestore.collection(Collections.FILES)
                    .whereEqualTo(Fields.RECEIVER_UID, userUid)
                    .get()
                    .addOnSuccessListener { receivedSnapshot ->
                        val historyList = mutableListOf<FileTransfer>()

                        // отправленные файлы
                        for (doc in sentSnapshot.documents) {
                            val transfer = doc.toObject(
                                FileTransfer::class.java
                            )
                            if (transfer != null) {
                                historyList.add(
                                    transfer.copy(id = doc.id)
                                )
                            }
                        }

                        // полученные файлы
                        for (doc in receivedSnapshot.documents) {
                            val transfer = doc.toObject(
                                FileTransfer::class.java
                            )
                            if (transfer != null) {
                                historyList.add(
                                    transfer.copy(id = doc.id)
                                )
                            }
                        }

                        // сортировка по дате
                        historyList.sortByDescending {
                            when (val timestamp = it.uploadedAt) {
                                is com.google.firebase.Timestamp ->
                                    timestamp.seconds

                                else -> 0L
                            }
                        }

                        onSuccess(historyList)
                    }
                    .addOnFailureListener(onFailure)
            }
            .addOnFailureListener(onFailure)
    }
}
