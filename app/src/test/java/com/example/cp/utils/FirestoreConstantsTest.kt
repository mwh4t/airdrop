package com.example.cp.utils

import org.junit.Test
import org. junit.Assert.*

class FirestoreConstantsTest {

    @Test
    fun `test users collection name`() {
        assertEquals("users", FirestoreConstants. Collections.USERS)
    }

    @Test
    fun `test files collection name`() {
        assertEquals("files", FirestoreConstants. Collections.FILES)
    }

    @Test
    fun `test transfers collection name`() {
        assertEquals("transfers", FirestoreConstants. Collections.TRANSFERS)
    }

    @Test
    fun `test pending status`() {
        assertEquals("pending", FirestoreConstants.TransferStatus.PENDING)
    }

    @Test
    fun `test received status`() {
        assertEquals("received", FirestoreConstants.TransferStatus.RECEIVED)
    }
}
