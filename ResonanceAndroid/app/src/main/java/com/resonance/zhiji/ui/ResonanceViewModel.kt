package com.resonance.zhiji.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.resonance.zhiji.data.db.ActionLogEntity
import com.resonance.zhiji.data.db.AppDatabase
import com.resonance.zhiji.data.db.CardDraftEntity
import com.resonance.zhiji.data.db.CardEntity
import com.resonance.zhiji.data.db.ChatMessageEntity
import com.resonance.zhiji.data.db.ChatSessionEntity
import com.resonance.zhiji.data.db.GoalEntity
import com.resonance.zhiji.data.db.KnowledgeLinkEntity
import com.resonance.zhiji.data.db.MemoryItemEntity
import com.resonance.zhiji.data.db.ProviderProfileEntity
import com.resonance.zhiji.data.db.ReminderEntity
import com.resonance.zhiji.data.db.TaskEntity
import com.resonance.zhiji.domain.ai.AssembledContextPacket
import com.resonance.zhiji.domain.ai.ContextBuilder
import com.resonance.zhiji.domain.offline.OfflineIntelligenceEngine
import com.resonance.zhiji.service.ReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalCoroutinesApi::class)
class ResonanceViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).resonanceDao()
    private val defaultSessionId = "session-default-v2"

    val cards: StateFlow<List<CardEntity>> = dao.observeAllCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<CardEntity>?>(null)
    val searchResults: StateFlow<List<CardEntity>?> = _searchResults.asStateFlow()

    val messages: StateFlow<List<ChatMessageEntity>> = MutableStateFlow(defaultSessionId)
        .flatMapLatest { sessionId -> dao.observeMessages(sessionId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingDrafts: StateFlow<List<CardDraftEntity>> = dao.observePendingDrafts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryItemEntity>> = dao.observeAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val knowledgeLinks: StateFlow<List<KnowledgeLinkEntity>> = dao.observeAllKnowledgeLinks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<GoalEntity>> = dao.observeGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = dao.observeTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val providers: StateFlow<List<ProviderProfileEntity>> = dao.observeProviders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lastContextPacket = MutableStateFlow<AssembledContextPacket?>(null)
    val lastContextPacket: StateFlow<AssembledContextPacket?> = _lastContextPacket.asStateFlow()

    private val _backupJsonPreview = MutableStateFlow<String?>(null)
    val backupJsonPreview: StateFlow<String?> = _backupJsonPreview.asStateFlow()

    init {
        viewModelScope.launch {
            if (dao.getCardCount() == 0) {
                seedInitialData()
            }
        }
    }

    private suspend fun seedInitialData() {
        val card1 = CardEntity(
            id = "card-fried-rice",
            title = "蛋炒饭晚餐",
            rawContent = "今晚吃了一个炒饭，很香很好吃。",
            contentType = "LIFE",
            tagsCsv = "美食日常,生活记录",
            interventionLevel = "L0"
        )
        val card2 = CardEntity(
            id = "card-habit-design",
            title = "习惯养成与环境设计",
            rawContent = "微习惯的核心不是靠意志力硬撑，而是降低启动阻力，把触发器放在显眼位置。",
            contentType = "KNOWLEDGE",
            tagsCsv = "知识卡片,行为心理学,习惯养成",
            aiSupplementContent = "AI 补充背景：对应福格行为模型 B=MAP（动机、能力、提示），通过减少环境摩擦力提升执行率。",
            interventionLevel = "L1",
            aiSuggestionPrompt = "已关联「环境设计」「注意力管理」知识节点"
        )
        val card3 = CardEntity(
            id = "card-postcard-memory-hub",
            title = "三个月前的灵感：离线个人记忆中枢",
            rawContent = "如果换了不同的大模型，以前聊过几百条的个人经历就全丢了。应该做一个本地 SQLite + 混合检索的独立记忆层。",
            contentType = "INSPIRATION",
            tagsCsv = "产品灵感,端侧架构",
            interventionLevel = "L1",
            aiSuggestionPrompt = "已收入灵感库，可与当前 V2.0 方案建立演进关联"
        )

        dao.upsertCardWithFts(card1)
        dao.upsertCardWithFts(card2)
        dao.upsertCardWithFts(card3)

        dao.insertKnowledgeLink(
            KnowledgeLinkEntity(
                fromEntityType = "CARD",
                fromEntityId = card2.id,
                fromTitle = "习惯养成与环境设计",
                toEntityType = "CARD",
                toEntityId = card3.id,
                toTitle = "离线个人记忆中枢",
                relationType = "SUPPLEMENT",
                reason = "两者均强调降低用户操作阻力（L0 安静记录减少认知负担）",
                confirmedByUser = true
            )
        )

        dao.insertChatSession(
            ChatSessionEntity(
                id = defaultSessionId,
                title = "晚间状态与个人精力复盘"
            )
        )
        dao.insertChatMessage(
            ChatMessageEntity(
                sessionId = defaultSessionId,
                role = "USER",
                content = "最近总觉得想学的新技术太多，下班后却连半小时都静不下心。",
                sequenceNumber = 1,
                tokenCount = 32
            )
        )
        dao.insertChatMessage(
            ChatMessageEntity(
                sessionId = defaultSessionId,
                role = "ASSISTANT",
                content = "这种「想推进很多事却难以启动」的疲惫感很常见。结合你之前的有效经验记录，晚间 21:00 后直接做高难度编码容易受阻。要不要试试只保留一个 15 分钟的阅读小切口？",
                sequenceNumber = 2,
                tokenCount = 78
            )
        )

        dao.upsertMemory(
            MemoryItemEntity(
                category = "LONG_TERM_GOAL",
                content = "职业方向更新：由原本准备考研调整为优先深耕 Android 与端侧 AI 工程落地（旧目标已归档保留）",
                originType = "USER_STATED",
                confidence = 0.98f,
                sourceSummary = "来源：10月生涯规划对话"
            )
        )
        dao.upsertMemory(
            MemoryItemEntity(
                category = "EFFECTIVE_METHOD",
                content = "晚间 21:00 后不适合高强度编码，拆分为 15-25 分钟微任务完成率最高",
                originType = "AI_INFERRED",
                confidence = 0.88f,
                sourceSummary = "来源：过去 3 周行动反馈复盘"
            )
        )

        val goal = GoalEntity(
            id = "goal-resonance-v2",
            title = "完成知己 Resonance 第一阶段本地闭环",
            description = "打通卡片 CRUD、FTS 全文检索、独立聊天、长期记忆与本地强提醒",
            currentStage = "核心开发阶段",
            progressPercent = 75
        )
        dao.upsertGoal(goal)

        dao.upsertTask(
            TaskEntity(
                goalId = goal.id,
                title = "阅读《福格行为模型》第三章并整理 2 张知识卡",
                scheduledTimeLabel = "今晚 20:30",
                estimatedMinutes = 30,
                reminderIntensity = "NORMAL",
                priority = 3,
                isDraft = false
            )
        )
        dao.upsertTask(
            TaskEntity(
                goalId = goal.id,
                title = "明天起床复习 Kotlin Coroutines 与 Room FTS4 索引",
                scheduledTimeLabel = "明天 08:30",
                estimatedMinutes = 25,
                reminderIntensity = "STRONG",
                priority = 2,
                isDraft = true
            )
        )

        dao.upsertProvider(
            ProviderProfileEntity(
                id = "provider-gemini",
                displayName = "Gemini 2.5 Pro (Adapter)",
                providerType = "GEMINI",
                endpointUrl = "https://generativelanguage.googleapis.com",
                modelName = "gemini-2.5-pro",
                maxContextTokens = 128000,
                isActive = true
            )
        )
        dao.upsertProvider(
            ProviderProfileEntity(
                id = "provider-claude",
                displayName = "Claude 3.7 Sonnet (Adapter)",
                providerType = "CLAUDE",
                endpointUrl = "https://api.anthropic.com",
                modelName = "claude-3-7-sonnet",
                maxContextTokens = 200000,
                isActive = false
            )
        )
        dao.upsertProvider(
            ProviderProfileEntity(
                id = "provider-local-slm",
                displayName = "端侧离线小模型 (llama.cpp / Qwen2.5-3B)",
                providerType = "LOCAL_SLM",
                endpointUrl = "local://llama.cpp/qwen2.5-3b-q4",
                modelName = "qwen2.5-3b-instruct-q4_k_m",
                supportsVision = false,
                maxContextTokens = 8192,
                isActive = false
            )
        )
    }

    fun saveQuickCard(rawText: String, customTitle: String? = null) {
        if (rawText.isBlank()) return
        viewModelScope.launch {
            val analysis = OfflineIntelligenceEngine.analyzeCardInput(rawText)
            val derivedTitle = customTitle?.takeIf { it.isNotBlank() }
                ?: rawText.trim().take(18)
            val card = CardEntity(
                title = derivedTitle,
                rawContent = rawText.trim(),
                contentType = analysis.contentType,
                tagsCsv = analysis.recommendedTags.joinToString(","),
                interventionLevel = analysis.interventionLevel,
                aiSuggestionPrompt = analysis.suggestionBanner
            )
            dao.upsertCardWithFts(card)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                _searchResults.value = null
            } else {
                val ftsMatches = runCatching { dao.searchCardsFts("$trimmed*") }.getOrDefault(emptyList())
                val results = ftsMatches.ifEmpty { dao.searchCardsByKeyword(trimmed) }
                _searchResults.value = results
            }
        }
    }

    fun sendChatMessage(userText: String, anchorCardTitle: String? = null) {
        if (userText.isBlank()) return
        viewModelScope.launch {
            val currentMessages = messages.value
            val nextSeq = (currentMessages.maxOfOrNull { it.sequenceNumber } ?: 0) + 1
            val activeProvider = providers.value.firstOrNull { it.isActive }
            val activeMemories = dao.getActiveAuthorizedMemories()
            val relatedCards = dao.searchCardsByKeyword(userText.take(6), limit = 3)

            val packet = ContextBuilder.buildContext(
                userMessage = userText,
                activeProvider = activeProvider,
                candidateMemories = activeMemories,
                candidateCards = relatedCards,
                anchorCardTitle = anchorCardTitle
            )
            _lastContextPacket.value = packet

            dao.insertChatMessage(
                ChatMessageEntity(
                    sessionId = defaultSessionId,
                    role = "USER",
                    content = userText.trim(),
                    sequenceNumber = nextSeq,
                    tokenCount = (userText.length * 0.6f).toInt().coerceAtLeast(8)
                )
            )

            val replyText = buildString {
                if (!anchorCardTitle.isNullOrBlank()) {
                    append("围绕旧卡片《$anchorCardTitle》继续思考：")
                }
                append("我理解你的意思。已通过统一适配层（${packet.providerName}）结合本地记忆库进行分析")
                if (packet.retrievalSkipped) {
                    append("（日常交流已自动跳过冗余历史检索以节省 Token）。")
                } else {
                    append("（本次按需挂载了 ${packet.injectedMemoriesCount} 条长期记忆与 ${packet.injectedCardsCount} 张关联卡片，预计消耗 ${packet.estimatedInputTokens}/${packet.tokenBudgetLimit} Tokens）。")
                }
                append("如果你觉得这段讨论有价值，可以点击下方按钮提炼为卡片草稿。")
            }

            dao.insertChatMessage(
                ChatMessageEntity(
                    sessionId = defaultSessionId,
                    role = "ASSISTANT",
                    content = replyText,
                    sequenceNumber = nextSeq + 1,
                    tokenCount = (replyText.length * 0.6f).toInt()
                )
            )
        }
    }

    fun createDraftFromMessage(message: ChatMessageEntity, mode: String = "STRUCTURED_NOTE") {
        viewModelScope.launch {
            val draft = CardDraftEntity(
                sessionId = message.sessionId,
                sourceMessageIdsJson = "[\"${message.id}\"]",
                proposedTitle = "对话提炼：${message.content.take(14)}...",
                proposedContent = message.content,
                draftMode = mode,
                suggestedTagsCsv = "对话精华,思考沉淀"
            )
            dao.upsertCardDraft(draft)
        }
    }

    fun confirmDraftToFormalCard(draft: CardDraftEntity) {
        viewModelScope.launch {
            dao.upsertCardDraft(draft.copy(status = "SAVED"))
            dao.upsertCardWithFts(
                CardEntity(
                    title = draft.proposedTitle,
                    rawContent = draft.proposedContent,
                    contentType = "KNOWLEDGE",
                    sourceType = "CHAT_EXTRACT",
                    sourceSessionId = draft.sessionId,
                    sourceMessageIdsJson = draft.sourceMessageIdsJson,
                    tagsCsv = draft.suggestedTagsCsv,
                    interventionLevel = "L0"
                )
            )
        }
    }

    fun dismissDraft(draft: CardDraftEntity) {
        viewModelScope.launch {
            dao.upsertCardDraft(draft.copy(status = "DISMISSED"))
        }
    }

    fun createPlanDraftFromCard(card: CardEntity) {
        viewModelScope.launch {
            val analysis = OfflineIntelligenceEngine.analyzeCardInput(card.rawContent)
            dao.upsertTask(
                TaskEntity(
                    sourceCardId = card.id,
                    title = card.title ?: card.rawContent.take(20),
                    notes = "源自卡片：${card.rawContent}",
                    scheduledTimeLabel = analysis.detectedTimeLabel ?: "明天 09:00",
                    estimatedMinutes = 25,
                    reminderIntensity = "STRONG",
                    isDraft = true
                )
            )
        }
    }

    fun confirmTaskDraft(task: TaskEntity) {
        viewModelScope.launch {
            val triggerAt = System.currentTimeMillis() + 60_000L
            val confirmed = task.copy(
                isDraft = false,
                status = "TODO",
                scheduledAt = triggerAt
            )
            dao.upsertTask(confirmed)
            dao.upsertReminder(
                ReminderEntity(
                    taskId = confirmed.id,
                    triggerAt = triggerAt,
                    intensity = confirmed.reminderIntensity
                )
            )
            ReminderScheduler.scheduleTaskReminder(
                context = getApplication(),
                taskId = confirmed.id,
                taskTitle = confirmed.title,
                triggerAtMillis = triggerAt,
                intensity = confirmed.reminderIntensity
            )
        }
    }

    fun markTaskDone(task: TaskEntity) {
        viewModelScope.launch {
            dao.upsertTask(
                task.copy(
                    status = "DONE",
                    completedAt = System.currentTimeMillis()
                )
            )
            dao.insertActionLog(
                ActionLogEntity(
                    taskId = task.id,
                    actionType = "COMPLETED",
                    actualDurationMinutes = task.estimatedMinutes ?: 25
                )
            )
        }
    }

    fun postponeTaskWithFeedback(task: TaskEntity) {
        viewModelScope.launch {
            val newCount = task.postponeCount + 1
            dao.upsertTask(
                task.copy(
                    status = "POSTPONED",
                    postponeCount = newCount
                )
            )
            dao.insertActionLog(
                ActionLogEntity(
                    taskId = task.id,
                    actionType = "POSTPONED",
                    obstacleReason = "累计推迟 $newCount 次，建议拆小任务粒度或调整提醒时段"
                )
            )
        }
    }

    fun toggleMemoryStatus(memory: MemoryItemEntity) {
        viewModelScope.launch {
            val nextStatus = if (memory.status == "ACTIVE") "DISABLED" else "ACTIVE"
            dao.upsertMemory(
                memory.copy(
                    status = nextStatus,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun switchProvider(providerId: String) {
        viewModelScope.launch {
            dao.setActiveProvider(providerId)
        }
    }

    fun exportFullBackupJson() {
        viewModelScope.launch {
            val root = JSONObject().apply {
                put("schemaVersion", "2.0.0")
                put("exportedAt", System.currentTimeMillis())
                put("cardsCount", cards.value.size)
                put("memoriesCount", memories.value.size)
                put("tasksCount", tasks.value.size)
                val cardArray = JSONArray()
                cards.value.forEach { c ->
                    cardArray.put(
                        JSONObject().apply {
                            put("id", c.id)
                            put("title", c.title)
                            put("rawContent", c.rawContent)
                            put("contentType", c.contentType)
                            put("tagsCsv", c.tagsCsv)
                        }
                    )
                }
                put("cards", cardArray)
            }
            _backupJsonPreview.value = root.toString(2)
        }
    }

    fun clearBackupPreview() {
        _backupJsonPreview.value = null
    }
}
