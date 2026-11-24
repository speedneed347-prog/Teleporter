package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = 0L,
    val lastReadingParagraphIndex: Int = 0,
    val lastReadingScrollOffsetPx: Int = 0,
    val lastReadingProgress: Float = 0f,
    val fontSizeSp: Float = 36f,
    val scrollSpeed: Int = 65,
    val lineSpacingMultiplier: Float = 1.45f,
    val paragraphSpacingDp: Int = 18,
    val horizontalMarginDp: Int = 20,
    val textAlignment: String = PrompterAlignment.LEFT.name,
    val textColorHex: Long = 0xFFFFFFFFL,
    val backgroundColorHex: Long = 0xFF000000L,
    val preferredTheme: String = AppThemeMode.DARK.name,
    val mirrorMode: String = MirrorMode.NORMAL.name,
    val scrollDirection: String = ScrollDirection.UP.name,
    val readingAreaWidthFraction: Float = 1.0f
) {
    fun wordCount(): Int {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return 0
        return trimmed.split(Regex("\\s+")).count { it.isNotBlank() }
    }

    fun estimatedDurationSeconds(wordsPerMinute: Int = 135): Int {
        val words = wordCount()
        if (words == 0) return 0
        return ((words * 60f) / wordsPerMinute.coerceAtLeast(60)).toInt().coerceAtLeast(1)
    }

    fun detectedScriptTag(): String {
        val hasBengali = content.any { it in '\u0980'..'\u09FF' }
        val hasHindi = content.any { it in '\u0900'..'\u097F' }
        val hasLatin = content.any { it in 'A'..'Z' || it in 'a'..'z' }
        return buildList {
            if (hasBengali) add("বাংলা")
            if (hasHindi) add("हिन्दी")
            if (hasLatin) add("EN")
        }.ifEmpty { listOf("TXT") }.joinToString(" · ")
    }
}
