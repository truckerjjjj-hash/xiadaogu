package com.resonance.zhiji.domain.offline

data class OfflineAnalysisResult(
    val contentType: String,          // LIFE, KNOWLEDGE, INSPIRATION, DIARY
    val recommendedTags: List<String>,
    val interventionLevel: String,    // L0, L1, L2, L3
    val suggestionBanner: String?,    // L1 轻量入口文案
    val detectedTimeLabel: String?,   // 确定性解析出的时间标签
    val isNegatedOrWishOnly: Boolean  // 第七章 7.3：识别否定或单纯愿望，防止误建任务
)

object OfflineIntelligenceEngine {

    private val negationMarkers = listOf("不想", "不去", "不用", "不要", "算了", "取消", "没打算", "并非")
    private val wishOnlyMarkers = listOf("要是能", "如果能", "幻想", "做梦", "希望有一天")
    private val actionMarkers = listOf("明天", "下周", "今晚", "起床", "一定要", "计划", "待办", "准备", "复习", "完成", "打卡")
    private val inspirationMarkers = listOf("灵感", "创意", "点子", "如果做一个", "产品构想", "架构", "设计方案")
    private val knowledgeMarkers = listOf("概念", "原理", "摘录", "读书笔记", "心理学", "算法", "定律", "研究发现")

    fun analyzeCardInput(rawText: String): OfflineAnalysisResult {
        val text = rawText.trim()
        val hasNegation = negationMarkers.any { text.contains(it) }
        val isWishOnly = wishOnlyMarkers.any { text.contains(it) }

        val timeLabel = when {
            text.contains("明天早上") || text.contains("明天起床") -> "明天 08:30"
            text.contains("明天") -> "明天 10:00"
            text.contains("今晚") || text.contains("今天晚上") -> "今晚 20:00"
            text.contains("周末") -> "本周六 10:00"
            text.contains("下周") -> "下周一 09:30"
            else -> null
        }

        val tags = mutableListOf<String>()
        if (text.contains("炒饭") || text.contains("吃") || text.contains("晚餐") || text.contains("咖啡")) tags.add("美食日常")
        if (text.contains("学习") || text.contains("阅读") || text.contains("考")) tags.add("成长学习")
        if (inspirationMarkers.any { text.contains(it) }) tags.add("产品灵感")
        if (knowledgeMarkers.any { text.contains(it) }) tags.add("知识卡片")
        if (timeLabel != null && !hasNegation) tags.add("待规划")
        if (tags.isEmpty()) tags.add("随手记")

        val contentType = when {
            inspirationMarkers.any { text.contains(it) } -> "INSPIRATION"
            knowledgeMarkers.any { text.contains(it) } -> "KNOWLEDGE"
            else -> "LIFE"
        }

        // L0-L3 介入判定规则（第三章 3.4 & 第七章 7.3）
        if (hasNegation || isWishOnly) {
            return OfflineAnalysisResult(
                contentType = contentType,
                recommendedTags = tags,
                interventionLevel = "L0",
                suggestionBanner = null,
                detectedTimeLabel = null,
                isNegatedOrWishOnly = true
            )
        }

        return when {
            actionMarkers.any { text.contains(it) } -> OfflineAnalysisResult(
                contentType = contentType,
                recommendedTags = tags,
                interventionLevel = "L1",
                suggestionBanner = if (timeLabel != null) "检测到行动意图（$timeLabel），要不要帮你安排到明天？" else "要不要一起把这个想法拆解成可执行的小步骤？",
                detectedTimeLabel = timeLabel,
                isNegatedOrWishOnly = false
            )
            contentType == "INSPIRATION" -> OfflineAnalysisResult(
                contentType = contentType,
                recommendedTags = tags,
                interventionLevel = "L1",
                suggestionBanner = "已收入灵感库。需要时可关联历史灵感或探讨可行性路线。",
                detectedTimeLabel = null,
                isNegatedOrWishOnly = false
            )
            contentType == "KNOWLEDGE" -> OfflineAnalysisResult(
                contentType = contentType,
                recommendedTags = tags,
                interventionLevel = "L1",
                suggestionBanner = "需要为你补充概念背景或连接已有知识卡片吗？",
                detectedTimeLabel = null,
                isNegatedOrWishOnly = false
            )
            else -> OfflineAnalysisResult(
                contentType = "LIFE",
                recommendedTags = tags,
                interventionLevel = "L0",
                suggestionBanner = null,
                detectedTimeLabel = null,
                isNegatedOrWishOnly = false
            )
        }
    }
}
