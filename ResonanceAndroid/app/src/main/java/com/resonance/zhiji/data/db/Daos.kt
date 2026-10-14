package com.resonance.zhiji.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ResonanceDao {

    // --- 卡片与 FTS4 全文检索 ---
    @Query("SELECT * FROM cards ORDER BY createdAt DESC")
    fun observeAllCards(): Flow<List<CardEntity>>

    @Query("SELECT COUNT(*) FROM cards")
    suspend fun getCardCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardInternal(card: CardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardFtsInternal(fts: CardFtsEntity)

    @Query("DELETE FROM cards_fts WHERE cardId = :cardId")
    suspend fun deleteCardFtsInternal(cardId: String)

    @Transaction
    suspend fun upsertCardWithFts(card: CardEntity) {
        insertCardInternal(card)
        deleteCardFtsInternal(card.id)
        insertCardFtsInternal(
            CardFtsEntity(
                cardId = card.id,
                title = card.title ?: "",
                rawContent = card.rawContent,
                tagsText = card.tagsCsv,
                aiSupplementContent = card.aiSupplementContent ?: ""
            )
        )
    }

    @Update
    suspend fun updateCard(card: CardEntity)

    @Transaction
    suspend fun deleteCardById(cardId: String) {
        deleteCardInternal(cardId)
        deleteCardFtsInternal(cardId)
    }

    @Query("DELETE FROM cards WHERE id = :cardId")
    suspend fun deleteCardInternal(cardId: String)

    @Query(
        """
        SELECT cards.* FROM cards
        INNER JOIN cards_fts ON cards.id = cards_fts.cardId
        WHERE cards_fts MATCH :ftsQuery
        ORDER BY cards.createdAt DESC
        LIMIT :limit
        """
    )
    suspend fun searchCardsFts(ftsQuery: String, limit: Int = 20): List<CardEntity>

    @Query(
        """
        SELECT * FROM cards
        WHERE rawContent LIKE '%' || :keyword || '%'
           OR COALESCE(title, '') LIKE '%' || :keyword || '%'
           OR tagsCsv LIKE '%' || :keyword || '%'
        ORDER BY createdAt DESC
        LIMIT :limit
        """
    )
    suspend fun searchCardsByKeyword(keyword: String, limit: Int = 20): List<CardEntity>

    // --- 独立 AI 聊天与精华草稿 ---
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun observeChatSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY sequenceNumber ASC")
    fun observeMessages(sessionId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM card_drafts WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun observePendingDrafts(): Flow<List<CardDraftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatSession(session: ChatSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCardDraft(draft: CardDraftEntity)

    // --- 四层长期记忆中枢 ---
    @Query("SELECT * FROM memory_items ORDER BY updatedAt DESC")
    fun observeAllMemories(): Flow<List<MemoryItemEntity>>

    @Query(
        """
        SELECT * FROM memory_items
        WHERE status = 'ACTIVE'
          AND allowCloudSync = 1
        ORDER BY confidence DESC, updatedAt DESC
        LIMIT :limit
        """
    )
    suspend fun getActiveAuthorizedMemories(limit: Int = 6): List<MemoryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemory(memory: MemoryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemorySource(source: MemorySourceEntity)

    // --- 知识与灵感关联网络 ---
    @Query("SELECT * FROM knowledge_links ORDER BY createdAt DESC")
    fun observeAllKnowledgeLinks(): Flow<List<KnowledgeLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledgeLink(link: KnowledgeLinkEntity)

    // --- 行动闭环：目标、任务、提醒、日志 ---
    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: GoalEntity)

    @Query("SELECT * FROM tasks ORDER BY isDraft DESC, priority DESC, createdAt DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionLog(log: ActionLogEntity)

    // --- 模型 Provider 配置 ---
    @Query("SELECT * FROM provider_profiles ORDER BY isActive DESC, displayName ASC")
    fun observeProviders(): Flow<List<ProviderProfileEntity>>

    @Query("UPDATE provider_profiles SET isActive = CASE WHEN id = :providerId THEN 1 ELSE 0 END")
    suspend fun setActiveProvider(providerId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProvider(profile: ProviderProfileEntity)
}
