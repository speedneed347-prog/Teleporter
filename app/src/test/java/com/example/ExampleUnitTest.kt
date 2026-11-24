package com.example

import com.example.data.model.ScriptEntity
import com.example.ui.components.ScriptChunker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `teleprompter speed mapping is strictly monotonic across 1 to 300`() {
        var previous = 0f
        for (speed in 1..300) {
            val current = ScriptChunker.speedToDpPerSecond(speed)
            assertTrue("Speed $speed should exceed previous $previous", current > previous)
            previous = current
        }
        assertEquals(ScriptChunker.speedToDpPerSecond(1), ScriptChunker.speedToDpPerSecond(-50), 0.001f)
        assertEquals(ScriptChunker.speedToDpPerSecond(300), ScriptChunker.speedToDpPerSecond(999), 0.001f)
    }

    @Test
    fun `script chunker splits very long documents cleanly without losing words`() {
        val longParagraph = (1..200).joinToString(" ") { "word$it" }
        val fullText = "প্রথম অনুচ্ছেদ বাংলায়।\n\n$longParagraph\n\nतीसरा अनुच्छेद हिंदी में।"
        val chunks = ScriptChunker.splitIntoParagraphChunks(fullText)

        assertTrue("Expected long paragraph to be split into multiple memory-safe chunks", chunks.size >= 4)
        assertEquals("প্রথম অনুচ্ছেদ বাংলায়।", chunks.first())
        assertEquals("तीसरा अनुच्छेद हिंदी में।", chunks.last())
        assertTrue(chunks.joinToString(" ").contains("word200"))
    }

    @Test
    fun `script entity computes word count and estimated duration`() {
        val script = ScriptEntity(
            title = "Test",
            content = (1..135).joinToString(" ") { "hello" }
        )
        assertEquals(135, script.wordCount())
        assertEquals(60, script.estimatedDurationSeconds(wordsPerMinute = 135))
    }
}
