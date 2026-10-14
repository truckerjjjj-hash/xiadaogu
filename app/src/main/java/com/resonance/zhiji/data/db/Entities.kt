package com.resonance.zhiji.data.db

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val providerProfileId: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val summaryText: String? = null,
    val allowMemoryExtraction: Boolean = true
)

@Entity(tableName = "chat_messages", indices = [Index("sessionId"), Index("createdAt")])
data class ChatMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val role: String, // USER, ASSISTANT, SYSTEM
    val content: String,
    val sequenceNumber: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val tokenCount: Int = 0,
    val referencedCardIdsJson: String = "[]"
)

@Entity(tableName = "cards", indices = [Index("contentType"), Index("createdAt"), Index("allowPostcardReview")])
data class CardEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val rawContent: String,
    val title: String? = null,
    val contentType: String = "LIFE", // LIFE, KNOWLEDGE, INSPIRATION, DIARY, VOICE, CUSTOM
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sourceType: String = "DIRECT_INPUT", // DIRECT_INPUT, CHAT_EXTRACT, POSTCARD_FOLLOWUP
    val sourceSessionId: String? = null,
    val sourceMessageIdsJson: String = "[]",
    val aiSupplementContent: String? = null,
    val interventionLevel: String = "L0", // L0, L1, L2, L3
    val aiSuggestionPrompt: String? = null,
    val allowPostcardReview: Boolean = true,
    val allowAiAccess: Boolean = true,
    val isSensitive: Boolean = false,
    val importanceScore: Int = 3,
    val lastReviewedAt: Long? = null
)

@Fts4(contentEntity = CardEntity::class)
@Entity(tableName = "cards_fts")
data class CardFtsEntity(
    val title: String?,
    val rawContent: String,
    val aiSupplementContent: String?
)

@Entity(tableName = "card_drafts", indices = [Index("sessionId")])
data class CardDraftEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val sourceMessageIdsJson: String,
    val proposedTitle: String,
    val proposedContent: String,
    val draftMode: String, // RAW_EXCERPT, KEY_POINTS, STRUCTURED_NOTE, BRIEF_SUMMARY
    val suggestedTagsJson: String = "[]",
    val status: String = "PENDING", // PENDING, SAVED, DISMISSED
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "attachments", indices = [Index("ownerType", "ownerId")])
data class AttachmentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ownerType: String, // CARD, CHAT_MESSAGE
    val ownerId: String,
    val mimeType: String,
    val localUri: String,
    val caption: String? = null,
    val sizeBytes: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "GENERAL",
    val createdByAi: Boolean = false
)

@Entity(tableName = "card_tag_relations", primaryKeys = ["cardId", "tagId"], indices = [Index("tagId")])
data class CardTagRelationEntity(
    val cardId: String,
    val tagId: String,
    val confidence: Float = 1.0f,
    val confirmedByUser: Boolean = true
)

@Entity(tableName = "memory_items", indices = [Index("category"), Index("status")])
data class MemoryItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val category: String, // PREFERENCE, LONG_TERM_GOAL, EXPERIENCE, KNOWLEDGE_TOPIC, EFFECTIVE_METHOD, EXPLICIT_FACT
    val content: String,
    val status: String = "ACTIVE", // ACTIVE, SUPERSEDED, DISABLED
    val confidence: Float = 0.9f,
    val originType: String = "USER_STATED", // USER_STATED, AI_INFERRED
    val sensitivityLevel: String = "NORMAL", // NORMAL, PRIVATE, RESTRICTED
    val allowCloudSync: Boolean = true,
    val previousVersionId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "memory_sources", indices = [Index("memoryId")])
data class MemorySourceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val memoryId: String,
    val sourceType: String, // CARD, CHAT_MESSAGE, ACTION_LOG
    val sourceId: String,
    val excerpt: String,
    val recordedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "knowledge_links", indices = [Index("fromEntityId"), Index("toEntityId")])
data class KnowledgeLinkEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val fromEntityType: String, // CARD, CHAT_SESSION, GOAL, TASK, MEMORY
    val fromEntityId: String,
    val toEntityType: String,
    val toEntityId: String,
    val relationType: String, // SIMILAR, SUPPLEMENT, TOPIC_RELATED, TEMPORAL, SUPPORT, CONTRADICTION
    val reason: String,
    val confirmedByUser: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val currentStage: String = "规划中",
    val progressPercent: Int = 0,
    val targetDate: Long? = null,
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks", indices = [Index("goalId"), Index("status"), Index("scheduledAt")])
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val goalId: String? = null,
    val sourceCardId: String? = null,
    val title: String,
    val notes: String = "",
    val isDraft: Boolean = false, // 必须经用户确认后才转为正式任务
    val repeatRule: String = "ONCE",
    val priority: Int = 2,
    val scheduledAt: Long? = null,
    val dueAt: Long? = null,
    val estimatedMinutes: Int? = null,
    val status: String = "TODO",
    val postponeCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(tableName = "reminders", indices = [Index("taskId")])
data class ReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val triggerAt: Long,
    val advanceMinutes: Int = 15,
    val intensity: String = "NORMAL", // LIGHT, NORMAL, STRONG
    val followUpIntervalMinutes: Int = 0,
    val maxNotifications: Int = 3,
    val sentCount: Int = 0,
    val dndRespect: Boolean = true,
    val isEnabled: Boolean = true
)

@Entity(tableName = "action_logs", indices = [Index("taskId")])
data class ActionLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val actionType: String, // COMPLETED, POSTPONED, CANCELLED, OBSTACLE_NOTE
    val obstacleReason: String? = null,
    val actualDurationMinutes: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "postcard_recommendations", indices = [Index("cardId")])
data class PostcardRecommendationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val cardId: String,
    val recommendReason: String,
    val score: Float,
    val state: String = "PEEKING", // PEEKING, EXPANDED, DISMISSED, SNOOZED, CHATTED
    val shownAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "provider_profiles")
data class ProviderProfileEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val providerType: String, // OPENAI_COMPAT, CLAUDE, GEMINI, LOCAL_SLM
    val endpointUrl: String,
    val modelName: String,
    val supportsToolCalling: Boolean = true,
    val supportsStructuredOutput: Boolean = true,
    val supportsVision: Boolean = true,
    val supportsStreaming: Boolean = true,
    val maxContextTokens: Int = 32000,
    val isActive: Boolean = false
)

@Entity(tableName = "model_packages")
data class ModelPackageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val engineType: String, // LLAMA_CPP, LITERT_LM
    val sizeBytes: Long,
    val localPath: String?,
    val status: String = "NOT_INSTALLED",
    val tokensPerSecondEstimate: Float = 0f
)

@Entity(tableName = "consent_records")
data class ConsentRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val scope: String, // CLOUD_MEMORY_UPLOAD, CLOUD_IMAGE_UPLOAD, AUTO_TAGGING, POSTCARD_REVIEW
    val granted: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)
