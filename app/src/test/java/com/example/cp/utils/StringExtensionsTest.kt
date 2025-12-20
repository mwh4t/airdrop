package com.example.cp.utils

import org.junit.Test
import org.junit.Assert.*

class StringExtensionsTest {

    @Test
    fun `test pdf extension`() {
        val fileName = "document.pdf"
        val extension = fileName.substringAfterLast('.', "")
        assertEquals("pdf", extension)
    }

    @Test
    fun `test jpg extension`() {
        val fileName = "image.jpg"
        val extension = fileName.substringAfterLast('.', "")
        assertEquals("jpg", extension)
    }

    @Test
    fun `test png extension`() {
        val fileName = "photo.png"
        val extension = fileName.substringAfterLast('.', "")
        assertEquals("png", extension)
    }

    @Test
    fun `test file without extension`() {
        val fileName = "filename"
        val extension = fileName.substringAfterLast('.', "")
        assertEquals("", extension)
    }

    @Test
    fun `test multiple dots in filename`() {
        val fileName = "my.file.name.txt"
        val extension = fileName.substringAfterLast('.', "")
        assertEquals("txt", extension)
    }
}
