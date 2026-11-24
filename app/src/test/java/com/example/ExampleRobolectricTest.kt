package com.example

import android.content.Context
import android.content.pm.PackageManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.TeleprompterDatabase
import com.example.data.model.AppSettings
import com.example.data.repository.DocumentImporter
import com.example.data.repository.ScriptRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: TeleprompterDatabase
    private lateinit var repository: ScriptRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, TeleprompterDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ScriptRepository(database.scriptDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `app name matches Smart Teleprompter Pro`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Smart Teleprompter Pro", appName)
    }

    @Test
    fun `manifest does not declare internet permission`() {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requested = packageInfo.requestedPermissions?.toList().orEmpty()
        assertFalse(requested.contains(android.Manifest.permission.INTERNET))
    }

    @Test
    fun `room script library CRUD, rename, duplicate, search, and position persistence`() = runTest {
        val bengaliText = "স্বাগতম দর্শকবৃন্দ। আজকের অনুষ্ঠানে আমরা বাংলা, हिन्दी এবং English স্ক্রিপ্ট পরীক্ষা করছি।"
        val id = repository.createScript("বাংলা নিউজ", bengaliText, AppSettings())
        assertTrue(id > 0L)

        val fetched = repository.getScriptById(id)
        assertNotNull(fetched)
        assertEquals("বাংলা নিউজ", fetched!!.title)
        assertEquals(bengaliText, fetched.content)
        assertTrue(fetched.detectedScriptTag().contains("বাংলা"))
        assertTrue(fetched.detectedScriptTag().contains("हिन्दी"))
        assertTrue(fetched.detectedScriptTag().contains("EN"))

        // Rename
        repository.renameScript(id, "বাংলা লাইভ শো")
        assertEquals("বাংলা লাইভ শো", repository.getScriptById(id)?.title)

        // Reading position
        repository.saveReadingPosition(id, paragraphIndex = 4, scrollOffsetPx = 128, progress = 0.65f)
        val afterPos = repository.getScriptById(id)!!
        assertEquals(4, afterPos.lastReadingParagraphIndex)
        assertEquals(128, afterPos.lastReadingScrollOffsetPx)
        assertEquals(0.65f, afterPos.lastReadingProgress, 0.001f)

        // Duplicate
        val copyId = repository.duplicateScript(afterPos)
        assertTrue(copyId != id)
        assertEquals("বাংলা লাইভ শো (Copy)", repository.getScriptById(copyId)?.title)

        // Search
        val searchMatches = repository.searchScripts("অনুষ্ঠানে").first()
        assertEquals(2, searchMatches.size)

        // Delete
        repository.deleteScriptById(id)
        assertNull(repository.getScriptById(id))
    }

    @Test
    fun `offline docx xml parser preserves Bengali Hindi and paragraph breaks`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                <w:p><w:r><w:t>নমস্কার বন্ধুরা</w:t></w:r></w:p>
                <w:p><w:r><w:t>नमस्ते भारत and Hello World</w:t></w:r></w:p>
              </w:body>
            </w:document>
        """.trimIndent()
        val importer = DocumentImporter(context)
        val parsed = importer.parseWordDocumentXml(xml.byteInputStream())
        assertEquals("নমস্কার বন্ধুরা\n\nनमस्ते भारत and Hello World", parsed)
    }
}
