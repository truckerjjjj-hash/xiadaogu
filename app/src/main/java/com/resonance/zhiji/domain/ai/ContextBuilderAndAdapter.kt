package com.resonance.zhiji.domain.ai

import com.resonance.zhiji.data.db.CardEntity
import com.resonance.zhiji.data.db.MemoryItemEntity
import com.resonance.zhiji.data.db.ProviderProfileEntity

data class AssembledContextPacket(
    val providerName: String,
    val modelName: String,
    val systemInstruction: String,
    val injectedMemories: List<MemoryItemEntity>,
    val injectedCards: List<CardEntity>,
    val estimatedInputTokens: Int,
    val tokenBudgetLimit: Int,
    val retrievalSkipped: Boolean
)

object ContextBuilder {

    fun buildContext(
        userMessage: String,
        activeProvider: ProviderProfileEntity,
        candidateMemories: List<MemoryItemEntity>,
        candidateCards: List<CardEntity>,
        anchorCard: CardEntity? = null
    ): AssembledContextPacket {
        val isCasualGreeting = userMessage.length <= 6 &&
            listOf("你好", "早安", "晚安", "在吗", "嗨", "哈哈").any { userMessage.contains(it) }

        val budget = if (isCasualGreeting) 600 else 2400
        val selectedMemories = if (isCasualGreeting) emptyList() else candidateMemories.take(4)
        val selectedCards = buildList {
            if (anchorCard != null) add(anchorCard)
            if (!isCasualGreeting) addAll(candidateCards.filter { it.id != anchorCard?.id }.take(3))
        }

        val sysPrompt = buildString {
            appendLine("你是「知己 Resonance」，用户的长期思考伙伴与个人知识助手。")
            appendLine("原则：1. 自然真诚，不假装人类情感；2. 引用记忆时标明来源；3. 涉及任务/卡片写入仅生成草案，由用户确认。")
            if (selectedMemories.isNotEmpty()) {
                appendLine("【授权长期记忆】")
                selectedMemories.forEach { m ->
                    appendLine("- [${m.category}/${m.originType}] ${m.content}")
                }
            }
            if (selectedCards.isNotEmpty()) {
                appendLine("【相关历史卡片】")
                selectedCards.forEach { c ->
                    appendLine("- 卡片(${c.title ?: "无标题"}): ${c.rawContent.take(120)}")
                }
            }
        }

        val estimatedTokens = (sysPrompt.length + userMessage.length) / 2
        return AssembledContextPacket(
            providerName = activeProvider.displayName,
            modelName = activeProvider.modelName,
            systemInstruction = sysPrompt,
            injectedMemories = selectedMemories,
            injectedCards = selectedCards,
            estimatedInputTokens = estimatedTokens,
            tokenBudgetLimit = budget,
            retrievalSkipped = isCasualGreeting
        )
    }

    val controlledTools = listOf(
        "search_memories",
        "search_cards",
        "get_card_source",
        "get_related_cards",
        "create_card_draft",
        "suggest_tags",
        "get_goals",
        "create_plan_draft",
        "get_tasks",
        "create_task_draft",
        "update_task_draft",
        "generate_review"
    )
}
