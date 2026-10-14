package com.resonance.zhiji.domain.ai

import com.resonance.zhiji.data.db.CardEntity
import com.resonance.zhiji.data.db.MemoryItemEntity
import com.resonance.zhiji.data.db.ProviderProfileEntity

/**
 * 第六章：Token 优化系统与统一 Context Builder
 * 第十一章：统一 Provider Adapter 与 12 项内部受控工具声明
 */
data class AssembledContextPacket(
    val providerName: String,
    val modelName: String,
    val systemInstruction: String,
    val injectedMemoriesCount: Int,
    val injectedCardsCount: Int,
    val estimatedInputTokens: Int,
    val tokenBudgetLimit: Int,
    val retrievalSkipped: Boolean
)

object ContextBuilder {

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

    fun buildContext(
        userMessage: String,
        activeProvider: ProviderProfileEntity?,
        candidateMemories: List<MemoryItemEntity>,
        candidateCards: List<CardEntity>,
        anchorCardTitle: String? = null
    ): AssembledContextPacket {
        val trimmed = userMessage.trim()
        val isCasualGreeting = trimmed.length <= 6 &&
            listOf("你好", "早安", "晚安", "在吗", "嗨", "哈哈", "谢谢").any { trimmed.contains(it) }

        val budget = if (isCasualGreeting) 512 else 2048
        val selectedMemories = if (isCasualGreeting) emptyList() else candidateMemories.take(4)
        val selectedCards = if (isCasualGreeting) emptyList() else candidateCards.take(3)

        val providerName = activeProvider?.displayName ?: "离线规则引擎"
        val modelName = activeProvider?.modelName ?: "local-deterministic-v2"

        val sysPrompt = buildString {
            appendLine("你是「知己 Resonance」，用户的长期思考伙伴与个人知识助手。")
            appendLine("当前模型接口：$providerName ($modelName)")
            if (!anchorCardTitle.isNullOrBlank()) {
                appendLine("当前对话锚定旧卡片：《$anchorCardTitle》")
            }
            if (selectedMemories.isNotEmpty()) {
                appendLine("【授权长期记忆】")
                selectedMemories.forEach { m ->
                    appendLine("- [${m.category}/${m.originType}] ${m.content}")
                }
            }
            if (selectedCards.isNotEmpty()) {
                appendLine("【相关历史卡片】")
                selectedCards.forEach { c ->
                    appendLine("- 《${c.title ?: "随手记"}》: ${c.rawContent.take(100)}")
                }
            }
        }

        val estimatedTokens = ((sysPrompt.length + trimmed.length) * 0.65f).toInt().coerceAtLeast(24)
        return AssembledContextPacket(
            providerName = providerName,
            modelName = modelName,
            systemInstruction = sysPrompt,
            injectedMemoriesCount = selectedMemories.size,
            injectedCardsCount = selectedCards.size,
            estimatedInputTokens = estimatedTokens,
            tokenBudgetLimit = budget,
            retrievalSkipped = isCasualGreeting
        )
    }
}
