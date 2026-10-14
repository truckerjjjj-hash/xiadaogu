package com.resonance.zhiji

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MarkunreadMailbox
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resonance.zhiji.data.db.CardDraftEntity
import com.resonance.zhiji.data.db.CardEntity
import com.resonance.zhiji.data.db.ChatMessageEntity
import com.resonance.zhiji.data.db.GoalEntity
import com.resonance.zhiji.data.db.KnowledgeLinkEntity
import com.resonance.zhiji.data.db.MemoryItemEntity
import com.resonance.zhiji.data.db.ProviderProfileEntity
import com.resonance.zhiji.data.db.TaskEntity
import com.resonance.zhiji.domain.ai.AssembledContextPacket
import com.resonance.zhiji.ui.ResonanceViewModel

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
                ResonanceMainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResonanceMainScreen(vm: ResonanceViewModel = viewModel()) {
    var currentTab by remember { mutableIntStateOf(0) }
    var postcardExpanded by remember { mutableStateOf(false) }
    var postcardDismissed by remember { mutableStateOf(false) }
    var anchorCardTitle by remember { mutableStateOf<String?>(null) }

    val cards by vm.cards.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val searchResults by vm.searchResults.collectAsState()
    val messages by vm.messages.collectAsState()
    val pendingDrafts by vm.pendingDrafts.collectAsState()
    val memories by vm.memories.collectAsState()
    val knowledgeLinks by vm.knowledgeLinks.collectAsState()
    val goals by vm.goals.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val providers by vm.providers.collectAsState()
    val lastContextPacket by vm.lastContextPacket.collectAsState()
    val backupJsonPreview by vm.backupJsonPreview.collectAsState()

    val activeProvider = providers.firstOrNull { it.isActive }
    val displayedCards = searchResults ?: cards

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("知己 Resonance V2.0", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "当前引擎：${activeProvider?.displayName ?: "离线规则引擎"} · 记忆独立",
                            fontSize = 11.sp,
                            color = Color(0xFF5C6B64)
                        )
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
                    icon = { Icon(Icons.Default.AccountTree, contentDescription = null) },
                    label = { Text("知识灵感") }
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                    label = { Text("行动闭环") }
                )
                NavigationBarItem(
                    selected = currentTab == 4,
                    onClick = { currentTab = 4 },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                    label = { Text("记忆中枢") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F6F1))
                .padding(padding)
        ) {
            when (currentTab) {
                0 -> CardsTabScreen(
                    cards = displayedCards,
                    searchQuery = searchQuery,
                    onSearchChange = vm::updateSearchQuery,
                    onSaveCard = vm::saveQuickCard,
                    onCreatePlanDraft = { card ->
                        vm.createPlanDraftFromCard(card)
                        currentTab = 3
                    },
                    onContinueChatWithCard = { card ->
                        anchorCardTitle = card.title ?: card.rawContent.take(16)
                        currentTab = 1
                    }
                )
                1 -> ChatTabScreen(
                    messages = messages,
                    pendingDrafts = pendingDrafts,
                    anchorCardTitle = anchorCardTitle,
                    lastContextPacket = lastContextPacket,
                    onClearAnchor = { anchorCardTitle = null },
                    onSendMessage = { text -> vm.sendChatMessage(text, anchorCardTitle) },
                    onExtractDraft = { msg -> vm.createDraftFromMessage(msg) },
                    onConfirmDraft = vm::confirmDraftToFormal
