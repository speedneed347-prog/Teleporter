package com.example.data.repository

import com.example.data.local.ScriptDao
import com.example.data.model.AppSettings
import com.example.data.model.ScriptEntity
import kotlinx.coroutines.flow.Flow

class ScriptRepository(private val scriptDao: ScriptDao) {

    fun observeAllScripts(): Flow<List<ScriptEntity>> = scriptDao.observeAllScripts()

    fun observeRecentScripts(limit: Int = 6): Flow<List<ScriptEntity>> = scriptDao.observeRecentScripts(limit)

    fun searchScripts(query: String): Flow<List<ScriptEntity>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) scriptDao.observeAllScripts() else scriptDao.searchScripts(trimmed)
    }

    fun observeScriptById(id: Long): Flow<ScriptEntity?> = scriptDao.observeScriptById(id)

    suspend fun getScriptById(id: Long): ScriptEntity? = scriptDao.getScriptById(id)

    suspend fun getMostRecentScript(): ScriptEntity? = scriptDao.getMostRecentScript()

    suspend fun createScript(
        title: String,
        content: String,
        defaults: AppSettings
    ): Long {
        val now = System.currentTimeMillis()
        val cleanTitle = title.trim().ifEmpty { "Untitled Script" }
        val entity = ScriptEntity(
            title = cleanTitle,
            content = content,
            createdAt = now,
            modifiedAt = now,
            lastOpenedAt = now,
            fontSizeSp = defaults.defaultFontSizeSp,
            scrollSpeed = defaults.defaultScrollSpeed,
            lineSpacingMultiplier = defaults.defaultLineSpacing,
            paragraphSpacingDp = defaults.defaultParagraphSpacingDp,
            horizontalMarginDp = defaults.defaultHorizontalMarginDp,
            textAlignment = defaults.defaultAlignment.name,
            textColorHex = defaults.defaultTextColorHex,
            backgroundColorHex = defaults.defaultBackgroundColorHex,
            preferredTheme = defaults.themeMode.name,
            mirrorMode = defaults.defaultMirrorMode.name,
            scrollDirection = defaults.defaultScrollDirection.name,
            readingAreaWidthFraction = defaults.defaultReadingAreaWidthFraction
        )
        return scriptDao.insertScript(entity)
    }

    suspend fun updateScript(script: ScriptEntity) {
        scriptDao.updateScript(script.copy(modifiedAt = System.currentTimeMillis()))
    }

    suspend fun renameScript(id: Long, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isNotEmpty()) {
            scriptDao.renameScript(id, clean, System.currentTimeMillis())
        }
    }

    suspend fun duplicateScript(script: ScriptEntity): Long {
        val now = System.currentTimeMillis()
        val copy = script.copy(
            id = 0L,
            title = "${script.title} (Copy)",
            createdAt = now,
            modifiedAt = now,
            lastOpenedAt = now,
            lastReadingParagraphIndex = 0,
            lastReadingScrollOffsetPx = 0,
            lastReadingProgress = 0f
        )
        return scriptDao.insertScript(copy)
    }

    suspend fun deleteScriptById(id: Long) {
        scriptDao.deleteScriptById(id)
    }

    suspend fun saveReadingPosition(
        id: Long,
        paragraphIndex: Int,
        scrollOffsetPx: Int,
        progress: Float
    ) {
        scriptDao.updateReadingPosition(
            id = id,
            paragraphIndex = paragraphIndex.coerceAtLeast(0),
            scrollOffsetPx = scrollOffsetPx.coerceAtLeast(0),
            progress = progress.coerceIn(0f, 1f),
            openedAt = System.currentTimeMillis()
        )
    }

    suspend fun saveReadingConfig(
        id: Long,
        fontSizeSp: Float,
        scrollSpeed: Int,
        lineSpacingMultiplier: Float,
        paragraphSpacingDp: Int,
        horizontalMarginDp: Int,
        textAlignment: String,
        textColorHex: Long,
        backgroundColorHex: Long,
        mirrorMode: String,
        scrollDirection: String,
        readingAreaWidthFraction: Float
    ) {
        scriptDao.updateReadingConfig(
            id = id,
            fontSizeSp = fontSizeSp.coerceIn(16f, 72f),
            scrollSpeed = scrollSpeed.coerceIn(1, 300),
            lineSpacingMultiplier = lineSpacingMultiplier.coerceIn(1.0f, 2.5f),
            paragraphSpacingDp = paragraphSpacingDp.coerceIn(4, 56),
            horizontalMarginDp = horizontalMarginDp.coerceIn(0, 96),
            textAlignment = textAlignment,
            textColorHex = textColorHex,
            backgroundColorHex = backgroundColorHex,
            mirrorMode = mirrorMode,
            scrollDirection = scrollDirection,
            readingAreaWidthFraction = readingAreaWidthFraction.coerceIn(0.5f, 1.0f),
            openedAt = System.currentTimeMillis()
        )
    }
}
