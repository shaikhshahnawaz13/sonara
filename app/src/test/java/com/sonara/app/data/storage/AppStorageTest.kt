package com.sonara.app.data.storage

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppStorageTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun file(dir: File, name: String, bytes: Int): File =
        File(dir, name).also { it.parentFile.mkdirs(); it.writeBytes(ByteArray(bytes)) }

    @Test
    fun sizeCountsNestedFiles() {
        val cache = temp.newFolder("cache")
        file(cache, "a.bin", 100)
        file(cache, "sub/b.bin", 250)
        assertEquals(350L, CacheCleaner.sizeOf(cache))
    }

    @Test
    fun missingDirectoryIsZero() {
        assertEquals(0L, CacheCleaner.sizeOf(File(temp.root, "nope")))
        assertEquals(ClearResult(0, 0, 0), CacheCleaner.clearContents(File(temp.root, "nope")))
    }

    @Test
    fun clearRemovesContentsButKeepsTheDirectory() {
        val cache = temp.newFolder("cache")
        file(cache, "a.bin", 100)
        file(cache, "sub/deeper/b.bin", 50)
        val result = CacheCleaner.clearContents(cache)
        assertTrue(cache.isDirectory)
        assertEquals(0, cache.listFiles()!!.size)
        assertEquals(150L, result.freedBytes)
        assertEquals(0, result.failed)
    }

    @Test
    fun clearNeverTouchesSiblingDirectories() {
        val cache = temp.newFolder("cache")
        val databases = temp.newFolder("databases")
        val music = temp.newFolder("music")
        file(cache, "x.tmp", 10)
        val db = file(databases, "sonara.db", 500)
        val song = file(music, "song.mp3", 900)
        CacheCleaner.clearContents(cache)
        assertTrue(db.exists())
        assertEquals(500L, db.length())
        assertTrue(song.exists())
    }

    @Test
    fun aSymbolicLinkIsRemovedButItsTargetIsNeverFollowed() {
        val cache = temp.newFolder("cache")
        val outside = temp.newFolder("outside")
        val precious = file(outside, "precious.bin", 77)
        val link = File(cache, "link")
        try {
            Files.createSymbolicLink(link.toPath(), outside.toPath())
        } catch (e: Exception) {
            return // symbolic links are unavailable on this file system; nothing to verify
        }
        assertEquals(0L, CacheCleaner.sizeOf(cache))
        CacheCleaner.clearContents(cache)
        assertFalse(Files.isSymbolicLink(link.toPath()))
        assertTrue(precious.exists())
        assertEquals(77L, precious.length())
    }

    @Test
    fun byteFormat() {
        assertEquals("0 B", ByteFormat.format(0))
        assertEquals("512 B", ByteFormat.format(512))
        assertEquals("1.0 KB", ByteFormat.format(1024))
        assertEquals("1.5 KB", ByteFormat.format(1536))
        assertEquals("12 MB", ByteFormat.format(12L * 1024 * 1024))
        assertEquals("1.2 GB", ByteFormat.format((1.2 * 1024 * 1024 * 1024).toLong()))
        assertEquals("0 B", ByteFormat.format(-5))
    }
}
