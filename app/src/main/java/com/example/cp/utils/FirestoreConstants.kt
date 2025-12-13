package com.example.cp.utils

object FirestoreConstants {
    // коллекции Firestore
    object Collections {
        const val USERS = "users"
        const val FILES = "files"
        const val TRANSFERS = "transfers"
    }

    // поля документов
    object Fields {
        const val UID = "uid"
        const val ID = "id"
        const val EMAIL = "email"
        const val DISPLAY_NAME = "displayName"
        const val AUTH_PROVIDER = "authProvider"
        const val CREATED_AT = "createdAt"

        const val FILE_NAME = "fileName"
        const val FILE_SIZE = "fileSize"
        const val FILE_TYPE = "fileType"
        const val SENDER_ID = "senderId"
        const val RECEIVER_ID = "receiverId"
        const val SENDER_UID = "senderUid"
        const val RECEIVER_UID = "receiverUid"
        const val STORAGE_URL = "storageUrl"
        const val UPLOADED_AT = "uploadedAt"
        const val STATUS = "status"

        const val FILE_ID = "fileId"
        const val TIMESTAMP = "timestamp"
    }

    // пути Storage
    object StoragePaths {
        const val TRANSFERS = "transfers"
    }

    // статусы передачи файлов
    object TransferStatus {
        const val PENDING = "pending"
        const val RECEIVED = "received"
        const val EXPIRED = "expired"
    }
}
