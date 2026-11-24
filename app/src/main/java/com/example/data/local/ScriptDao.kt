package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ScriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {

    @Query("SELECT * FROM scripts ORDER BY modifiedAt DESC")
    fun observeAllScripts(): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts ORDER BY CASE WHEN lastOpenedAt > 0 THEN lastOpenedAt ELSE modifiedAt END DESC LIMIT :limit")
    fun observeRecentScripts(limit: Int = 6): Flow<List<ScriptEntity>>

    @Query(
        """
        SELECT * FROM scripts
        WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'
        ORDER BY modifiedAt DESC
        """
    )
    fun searchScripts(query: String): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts WHERE id = :id LIMIT 1")
    fun observeScriptById(id: Long): Flow<ScriptEntity?>

    @Query("SELECT * FROM scripts WHERE id = :id LIMIT 1")
    suspend fun getScriptById(id: Long): ScriptEntity?

    @Query("SELECT * FROM scripts ORDER BY CASE WHEN lastOpenedAt > 0 THEN lastOpenedAt ELSE modifiedAt END DESC LIMIT 1")
    suspend fun getMostRecentScript(): ScriptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: ScriptEntity): Long

    @Update
    suspend fun updateScript(script: ScriptEntity)

    @Delete
    suspend fun deleteScript(script: ScriptEntity)

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun deleteScriptById(id: Long)

    @Query("UPDATE scripts SET title = :newTitle, modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun renameScript(id: Long, newTitle: String, modifiedAt: Long)

    @Query(
        """
        UPDATE scripts
        SET lastReadingParagraphIndex = :paragraphIndex,
            lastReadingScrollOffsetPx = :scrollOffsetPx,
            lastReadingProgress = :progress,
            lastOpenedAt = :openedAt
        WHERE id = :id
        """
    )
    suspend fun updateReadingPosition(
        id: Long,
        paragraphIndex: Int,
        scrollOffsetPx: Int,
        progress: Float,
        openedAt: Long
    )

    @Query(
        """
        UPDATE scripts
        SET fontSizeSp = :fontSizeSp,
            scrollSpeed = :scrollSpeed,
            lineSpacingMultiplier = :lineSpacingMultiplier,
            paragraphSpacingDp = :paragraphSpacingDp,
            horizontalMarginDp = :horizontalMarginDp,
            textAlignment = :textAlignment,
            textColorHex = :textColorHex,
            backgroundColorHex = :backgroundColorHex,
            mirrorMode = :mirrorMode,
            scrollDirection = :scrollDirection,
            readingAreaWidthFraction = :readingAreaWidthFraction,
            lastOpenedAt = :openedAt
        WHERE id = :id
        """
    )
    suspend fun updateReadingConfig(
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
        readingAreaWidthFraction: Float,
        openedAt: Long
    )
}
