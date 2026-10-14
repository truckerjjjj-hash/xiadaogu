// === 文件: app/src/main/java/com/resonance/zhiji/data/db/Daos.kt ===
package com.resonance.zhiji.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ResonanceDao {
    @Query("SELECT * FROM cards ORDER BY createdAt DESC")
    fun observeAllCards(): Flow<List<CardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity)

    @Update
    suspend fun updateCard(card: CardEntity)

    @Query("""
        SELECT cards.* FROM cards
        JOIN cards_fts ON cards.rowid = cards_fts.rowid
        WHERE cards_fts MATCH :ftsQuery
        ORDER BY cards.createdAt DESC
        LIMIT :limit
    """)
    suspend fun searchCardsFts(ftsQuery: String, limit: Int = 10): List<CardEntity>

    @Query("""
        SELECT * FROM cards
        WHERE allowAiAccess = 1
          AND (rawContent LIKE '%' || :keyword || '%' OR COALESCE(title, '') LIKE '%' || :keyword || '%')
        ORDER BY createdAt DESC
        LIMIT :limit
    """)
    suspend fun searchAuthorizedCardsByKeyword(keyword: String, limit: Int = 8): List<CardEntity>

    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun observeChatSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY sequenceNumber ASC")
    fun observeMessages(sessionId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatSession(session: ChatSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardDraft(draft: CardDraftEntity)

    @Query("SELECT * FROM memory_items WHERE status = 'ACTIVE' ORDER BY updatedAt DESC")
    fun observeActiveMemories(): Flow<List<MemoryItemEntity>>

    @Query("""
        SELECT * FROM memory_items
        WHERE status = 'ACTIVE'
          AND allowCloudSync = 1
          AND content LIKE '%' || :query || '%'
        ORDER BY confidence DESC, updatedAt DESC
        LIMIT :limit
    """)
    suspend fun retrieveRelevantMemories(query: String, limit: Int = 6): List<MemoryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemory(memory: MemoryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemorySource(source: MemorySourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledgeLink(link: KnowledgeLinkEntity)

    @Query("SELECT * FROM tasks ORDER BY isDraft DESC, priority DESC, createdAt DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(reminder: ReminderEntity)

    @Query("""
        SELECT * FROM cards
        WHERE allowPostcardReview = 1 AND isSensitive = 0
        ORDER BY COALESCE(lastReviewedAt, 0) ASC, importanceScore DESC
        LIMIT 5
    """)
    suspend fun getPostcardCandidates(): List<CardEntity>

    @Query("SELECT * FROM provider_profiles ORDER BY isActive DESC")
    fun observeProviders(): Flow<List<ProviderProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProvider(profile: ProviderProfileEntity)
}
