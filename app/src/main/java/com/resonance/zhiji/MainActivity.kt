package com.resonance.zhiji

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonance.zhiji.data.db.CardEntity
import com.resonance.zhiji.data.db.MemoryItemEntity
import com.resonance.zhiji.data.db.TaskEntity
import com.resonance.zhiji.domain.offline.OfflineIntelligenceEngine

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF2E5E4E),
                    secondary = Color(0xFF8B6B4A),
                    background = Color(0xFFF8F6F1),
                    surface = Color(0xFFFFFFFF)
                )
            ) {
                ResonanceMainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResonanceMainApp() {
    var currentTab by remember { mutableIntStateOf(0) }
    var postcardExpanded by remember { mutableStateOf(false) }
    var postcardDismissed by remember { mutableStateOf(false) }
    var chatContextBanner by remember { mutableStateOf<String?>(null) }

    val cards = remember {
        mutableStateListOf(
            CardEntity(
                title = "蛋炒饭晚餐",
                rawContent = "今晚吃了一个炒饭，很香很好吃。",
                contentType = "LIFE",
                interventionLevel = "L0"
            ),
            CardEntity(
                title = "习惯养成与环境设计",
                rawContent = "读书摘录：微习惯的核心不是靠意志力硬撑，而是降低启动阻力，把触发器放在显眼位置。",
                contentType = "KNOWLEDGE",
                aiSupplementContent = "关联概念：福格行为模型 (B=MAP)、注意力管理、环境摩擦力设计。",
                interventionLevel = "L1",
                aiSuggestionPrompt = "已关联「环境设计」「行为心理学」2 张知识卡片"
            ),
            CardEntity(
                title = "三个月前的灵感：离线个人记忆中枢",
                rawContent = "如果换了不同的大模型，以前聊过几百条的个人经历就全丢了。应该做一个本地 SQLite + 混合检索的独立记忆层。",
                contentType = "INSPIRATION",
                interventionLevel = "L1"
            )
        )
    }

    val tasks = remember {
        mutableStateListOf(
            TaskEntity(
                title = "阅读《福格行为模型》第三章并整理 2 张知识卡",
                scheduledAt = System.currentTimeMillis() + 3600_000,
                estimatedMinutes = 40,
                priority = 3,
                isDraft = false
            ),
            TaskEntity(
                title = "【AI 计划草案】明天 08:30 晨间学习 25 分钟",
                scheduledAt = System.currentTimeMillis() + 43200_000,
                estimatedMinutes = 25,
                priority = 2,
                isDraft = true
            )
        )
    }

    val memories = remember {
        mutableStateListOf(
            MemoryItemEntity(
                category = "LONG_TERM_GOAL",
                content = "职业方向更新：由原本准备考研调整为优先积累 Android 与端侧 AI 工程实践经验（保留历史版本可追溯）",
                originType = "USER_STATED",
                confidence = 0.98f
            ),
            MemoryItemEntity(
                category = "EFFECTIVE_METHOD",
                content = "晚间 21:00 后不适合安排高强度编码任务，拆分为 25 分钟小步子完成率最高",
                originType = "AI_INFERRED",
                confidence = 0.86f
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("知己 Resonance", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        Text("记忆独立 · 智能适度 · 离线优先", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                    label = { Text("卡片盒") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Forum, contentDescription = null) },
                    label = { Text("知己聊天") }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                    label = { Text("行动闭环") }
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                    label = { Text("记忆中枢") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (currentTab) {
                0 -> CardsScreen(cards = cards, onCreateDraftTask = { title ->
                    tasks.add(0, TaskEntity(title = "【AI 计划草案】$title", isDraft = true))
                    currentTab = 2
                })
                1 -> ChatScreen(
                    contextBanner = chatContextBanner,
                    onClearBanner = { chatContextBanner = null },
                    onSaveCardFromChat = { title, text ->
                        cards.add(0, CardEntity(title = title, rawContent = text, sourceType = "CHAT_EXTRACT"))
                    }
                )
                2 -> ActionsScreen(tasks = tasks)
                3 -> MemoryHubScreen(memories = memories)
            }

            // 第十章：侧边微露的回忆明信片角标
            if (!postcardDismissed) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clickable { postcardExpanded = true },
                    shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                    color = Color(0xFF8B6B4A),
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.MarkunreadMailbox, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text("旧日\n明信片", color = Color.White, fontSize = 11.sp)
                    }
                }
            }

            if (postcardExpanded) {
                AlertDialog(
                    onDismissRequest = { postcardExpanded = false },
                    title = { Text("寄自 90 天前的回忆明信片") },
                    text = {
                        Column {
                            Text("「三个月前的灵感：离线个人记忆中枢」", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("如果换了不同的大模型，以前聊过几百条的个人经历就全丢了。应该做一个本地 SQLite + 混合检索的独立记忆层。")
                            Spacer(Modifier.height(12.dp))
                            Text("推荐理由：与你近期正在推进的 V2.0 架构高度呼应", fontSize = 12.sp, color = Color.Gray)
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            postcardExpanded = false
                            chatContextBanner = "已挂载明信片上下文：《三个月前的灵感：离线个人记忆中枢》"
                            currentTab = 1
                        }) {
                            Text("继续聊聊")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            postcardExpanded = false
                            postcardDismissed = true
                        }) {
                            Text("收起并忽略本次")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CardsScreen(cards: MutableList<CardEntity>, onCreateDraftTask: (String) -> Unit) {
    var inputText by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("随时记录一句话、灵感或生活瞬间（L0 默认安静保存）...") },
            trailingIcon = {
                IconButton(onClick = {
                    if (inputText.isNotBlank()) {
                        val analysis = OfflineIntelligenceEngine.analyzeCardInput(inputText)
                        cards.add(
                            0,
                            CardEntity(
                                rawContent = inputText,
                                contentType = analysis.contentType,
                                interventionLevel = analysis.interventionLevel,
                                aiSuggestionPrompt = analysis.suggestionBanner
                            )
                        )
                        inputText = ""
                    }
                }) {
                    Icon(Icons.Default.Send, contentDescription = "保存卡片")
                }
            }
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(cards, key = { it.id }) { card ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = card.title ?: "随手卡片",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            AssistChip(
                                onClick = {},
                                label = { Text("${card.contentType} · ${card.interventionLevel}", fontSize = 11.sp) }
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(card.rawContent, fontSize = 14.sp)
                        card.aiSupplementContent?.let { sup ->
                            Spacer(Modifier.height(6.dp))
                            Text(sup, fontSize = 12.sp, color = Color(0xFF2E5E4E))
                        }
                        card.aiSuggestionPrompt?.let { prompt ->
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFEEF4F1), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(prompt, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                TextButton(onClick = { onCreateDraftTask(card.rawContent) }) {
                                    Text("生成计划草案")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(
    contextBanner: String?,
    onClearBanner: () -> Unit,
    onSaveCardFromChat: (String, String) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            "USER" to "最近总觉得想学的新技术太多，下班后却连半小时都静不下心。",
            "ASSISTANT" to "这种「想推进很多事却难以启动」的疲惫感很常见。结合你之前的有效经验记录，晚间 21:00 后直接做高难度编码容易受阻。要不要试试只保留一个 15 分钟的阅读小切口？"
        )
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        contextBanner?.let { banner ->
            Surface(
                color = Color(0xFFEEF4F1),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(banner, fontSize = 12.sp, color = Color(0xFF2E5E4E), modifier = Modifier.weight(1f))
                    TextButton(onClick = onClearBanner) { Text("移除", fontSize = 12.sp) }
                }
            }
        }
        Text("独立自由对话（不强制生成笔记，可按需提取重点为卡片）", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { (role, content) ->
                Surface(
                    color = if (role == "USER") Color(0xFF2E5E4E) else Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = content,
                            color = if (role == "USER") Color.White else Color.Black,
                            fontSize = 14.sp
                        )
                        if (role == "ASSISTANT") {
                            TextButton(
                                onClick = { onSaveCardFromChat("对话精华：15 分钟低阻力启动法", content) },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("提取为正式卡片", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        OutlinedTextField(
            value = messageText,
            onValueChange = { messageText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("与知己自由聊聊...") },
            trailingIcon = {
                IconButton(onClick = {
                    if (messageText.isNotBlank()) {
                        val userMsg = messageText
                        messages.add("USER" to userMsg)
                        messages.add("ASSISTANT" to "我记下了你的想法：「$userMsg」。已通过本地混合检索匹配相关记忆（耗费约 180 Tokens），需要把核心结论保存为待确认卡片草稿吗？")
                        messageText = ""
                    }
                }) {
                    Icon(Icons.Default.Send, contentDescription = "发送")
                }
            }
        )
    }
}

@Composable
fun ActionsScreen(tasks: MutableList<TaskEntity>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(tasks, key = { it.id }) { task ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (task.isDraft) Color(0xFFFFF8E7) else Color.White
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(task.title, fontWeight = FontWeight.Medium)
                        Text(
                            if (task.isDraft) "状态：待确认计划草案（AI 识别意图不等于执行授权）" else "状态：${task.status} · 原生 AlarmManager 强提醒已就绪",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    if (task.isDraft) {
                        Button(onClick = {
                            val idx = tasks.indexOf(task)
                            if (idx >= 0) tasks[idx] = task.copy(isDraft = false, title = task.title.removePrefix("【AI 计划草案】"))
                        }) {
                            Text("确认创建")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryHubScreen(memories: List<MemoryItemEntity>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(memories, key = { it.id }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("${item.category} · 来源：${item.originType}", fontSize = 12.sp, color = Color(0xFF2E5E4E))
                    Spacer(Modifier.height(4.dp))
                    Text(item.content, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("置信度 ${(item.confidence * 100).toInt()}% · 独立于大模型存储，可随时修改或停用", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}
