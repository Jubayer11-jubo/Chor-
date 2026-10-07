package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FriendConnection
import com.example.data.model.ScoreEntry
import com.example.data.model.TeamRecord
import com.example.data.model.UserProfile
import com.example.data.model.currentDayKey
import com.example.data.model.currentWeekKey
import com.example.ui.theme.ChorCardBorder
import com.example.ui.theme.ChorGold
import com.example.ui.theme.ChorNeonCyan
import com.example.ui.theme.ChorNeonEmerald
import com.example.ui.theme.ChorSurfaceDark
import com.example.ui.theme.ChorSurfaceElevated
import com.example.ui.theme.ChorTextSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RankingScreen(
    currentUserId: String,
    userProfile: UserProfile?,
    validScores: List<ScoreEntry>,
    friendsList: List<FriendConnection> = emptyList(),
    activeTeams: List<TeamRecord> = emptyList(),
    onStartGame: () -> Unit,
    onOpenMultiplayerHub: (Int) -> Unit = {},
    onBackToHome: () -> Unit
) {
    BackHandler {
        onBackToHome()
    }

    // Main category: 0 = Global, 1 = Friends, 2 = Teams
    var selectedCategory by remember { mutableIntStateOf(0) }
    val categoryTabs = listOf(
        "🌍 গ্লোবাল (Global)",
        "👥 বন্ধুদের র‍্যাঙ্কিং (Friends)",
        "🛡️ টিম র‍্যাঙ্কিং (Teams)"
    )

    var selectedPeriodIndex by remember { mutableIntStateOf(0) } // 0 = Daily, 1 = Weekly, 2 = All-Time
    val periodTabs = listOf("Daily • আজকের", "Weekly • সাপ্তাহিক", "All-Time • সর্বকালের")
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    val dayKey = currentDayKey()
    val weekKey = currentWeekKey()

    val friendUserIds = remember(friendsList, currentUserId) {
        (friendsList.map { it.friendUserId } + currentUserId).toSet()
    }

    // Filter by period and aggregate best score per user
    val filteredScores = remember(validScores, selectedPeriodIndex, selectedCategory, dayKey, weekKey, friendUserIds) {
        val periodMatches = when (selectedPeriodIndex) {
            0 -> validScores.filter { it.periodDay == dayKey }
            1 -> validScores.filter { it.periodWeek == weekKey }
            else -> validScores
        }
        val categoryMatches = if (selectedCategory == 1) {
            periodMatches.filter { friendUserIds.contains(it.userId) }
        } else {
            periodMatches
        }
        categoryMatches
            .groupBy { it.userId }
            .mapNotNull { (_, list) -> list.maxByOrNull { it.coinsEarned } }
            .sortedByDescending { it.coinsEarned }
    }

    val myRankIndex = filteredScores.indexOfFirst { it.userId == currentUserId }
    val myRankDisplay = if (myRankIndex >= 0) "#${myRankIndex + 1}" else "Unranked"
    val myBestInPeriod = if (myRankIndex >= 0) {
        filteredScores[myRankIndex].coinsEarned
    } else {
        userProfile?.bestScore ?: 0L
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🏆 লিডারবোর্ড (Leaderboard)",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ChorGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Global • Friends • Team Competition Standings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChorTextSecondary
                    )
                }
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Trophy",
                    tint = ChorGold,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Selector: Global | Friends | Teams
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categoryTabs.forEachIndexed { idx, label ->
                    val selected = selectedCategory == idx
                    Surface(
                        color = if (selected) ChorNeonCyan else ChorSurfaceDark,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .border(1.dp, if (selected) ChorNeonCyan else ChorCardBorder, RoundedCornerShape(14.dp))
                            .clickable { selectedCategory = idx }
                            .testTag("ranking_category_$idx")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Color(0xFF00262B) else Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedCategory == 2) {
                // TEAM LEADERBOARD VIEW
                TeamLeaderboardContent(
                    activeTeams = activeTeams,
                    myTeamId = userProfile?.teamId ?: "",
                    numberFormat = numberFormat,
                    onManageTeams = { onOpenMultiplayerHub(2) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Period Selector (Daily / Weekly / All-Time)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ChorSurfaceDark, RoundedCornerShape(16.dp))
                        .border(1.dp, ChorCardBorder, RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    periodTabs.forEachIndexed { idx, title ->
                        val selected = selectedPeriodIndex == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) ChorGold else Color.Transparent)
                                .clickable { selectedPeriodIndex = idx }
                                .padding(vertical = 10.dp)
                                .testTag("ranking_tab_$idx"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) Color(0xFF0A0F1D) else ChorTextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Top 3 Podium Cards (🥇 Rank 1, 🥈 Rank 2, 🥉 Rank 3)
                if (filteredScores.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val rank1 = filteredScores.getOrNull(0)
                        val rank2 = filteredScores.getOrNull(1)
                        val rank3 = filteredScores.getOrNull(2)

                        PodiumCard(
                            medal = "🥈 Rank 2",
                            entry = rank2,
                            accent = Color(0xFFCFD8DC),
                            numberFormat = numberFormat,
                            modifier = Modifier.weight(1f)
                        )
                        PodiumCard(
                            medal = "🥇 Rank 1",
                            entry = rank1,
                            accent = ChorGold,
                            numberFormat = numberFormat,
                            isFirst = true,
                            modifier = Modifier.weight(1.1f)
                        )
                        PodiumCard(
                            medal = "🥉 Rank 3",
                            entry = rank3,
                            accent = Color(0xFFFFAB91),
                            numberFormat = numberFormat,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Current User Rank Banner ("Your Rank / Your Score")
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ChorSurfaceElevated),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, ChorNeonCyan, RoundedCornerShape(18.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (selectedCategory == 1) "Friend Rank • বন্ধুদের মধ্যে অবস্থান" else "Your Rank • তোমার অবস্থান",
                                style = MaterialTheme.typography.labelMedium,
                                color = ChorNeonCyan
                            )
                            Text(
                                text = "$myRankDisplay • ${userProfile?.username ?: "চোর প্লেয়ার"}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Your Score",
                                style = MaterialTheme.typography.labelMedium,
                                color = ChorTextSecondary
                            )
                            Text(
                                text = "🪙 ${numberFormat.format(myBestInPeriod)} Coins",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = ChorGold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Leaderboard List or Empty State
                if (filteredScores.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(1.dp, ChorCardBorder, RoundedCornerShape(22.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = "🥇", fontSize = 44.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedCategory == 1) "বন্ধুদের এই পিরিয়ডে এখনো কোনো স্কোর নেই!" else "এই পিরিয়ডে এখনো কোনো স্কোর নেই!",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "এখনই একটি রাউন্ড খেলে Rank #1 দখল করো অথবা বন্ধুদের আমন্ত্রণ জানাও!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ChorTextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onStartGame,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ChorNeonEmerald,
                                        contentColor = Color(0xFF041F10)
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("খেলা শুরু করুন", fontWeight = FontWeight.Bold)
                                }
                                if (selectedCategory == 1) {
                                    OutlinedButton(
                                        onClick = { onOpenMultiplayerHub(0) },
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("🤝 বন্ধু ডাকুন", color = ChorGold, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(filteredScores, key = { _, item -> item.scoreId }) { index, entry ->
                            val rank = index + 1
                            val isMe = entry.userId == currentUserId
                            val medalPrefix = when (rank) {
                                1 -> "🥇 #1"
                                2 -> "🥈 #2"
                                3 -> "🥉 #3"
                                else -> "#$rank"
                            }
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMe) ChorSurfaceElevated else ChorSurfaceDark
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isMe) 1.5.dp else 1.dp,
                                        color = if (isMe) ChorGold else ChorCardBorder,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = if (rank <= 3) ChorGold.copy(alpha = 0.2f) else ChorSurfaceElevated,
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = medalPrefix,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = if (rank <= 3) ChorGold else Color.White,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = entry.username + if (isMe) " (তুমি)" else "",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "📦 ${entry.itemsCollected} items • 🔥 x${entry.bestCombo} combo",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = ChorTextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${numberFormat.format(entry.coinsEarned)} Coins",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = ChorGold,
                                        fontSize = 15.sp
                                    )
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
private fun TeamLeaderboardContent(
    activeTeams: List<TeamRecord>,
    myTeamId: String,
    numberFormat: NumberFormat,
    onManageTeams: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedTeams = remember(activeTeams) {
        activeTeams.sortedByDescending { it.totalScore }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (sortedTeams.isEmpty()) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, ChorCardBorder, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "🛡️", fontSize = 44.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "এখনো কোনো টিম তৈরি হয়নি!",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "নিজের 'চোর বাহিনী' টিম তৈরি করো এবং বন্ধুদের নিয়ে টিম কম্পিটিশনে অংশ নাও!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChorTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onManageTeams,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChorGold,
                            contentColor = Color(0xFF1F1200)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("টিম তৈরি বা জয়েন করুন", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Winner Spotlight Banner for #1 Team
            val topTeam = sortedTeams.first()
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ChorSurfaceElevated),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, ChorGold, RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🏆 LEADING TEAM • চ্যাম্পিয়ন টিম",
                            style = MaterialTheme.typography.labelMedium,
                            color = ChorGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${topTeam.teamAvatar} ${topTeam.teamName}",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manager: ${topTeam.managerName} • Members: ${topTeam.memberNames.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ChorTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "🪙 ${numberFormat.format(topTeam.totalScore)}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = ChorGold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(sortedTeams, key = { _, t -> t.teamId }) { index, team ->
                    val rank = index + 1
                    val isMyTeam = team.teamId == myTeamId
                    val medalPrefix = when (rank) {
                        1 -> "🥇 #1"
                        2 -> "🥈 #2"
                        3 -> "🥉 #3"
                        else -> "#$rank"
                    }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMyTeam) ChorSurfaceElevated else ChorSurfaceDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isMyTeam) 1.5.dp else 1.dp,
                                color = if (isMyTeam) ChorNeonEmerald else ChorCardBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = if (rank <= 3) ChorGold.copy(alpha = 0.2f) else ChorSurfaceElevated,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = medalPrefix,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = if (rank <= 3) ChorGold else Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${team.teamAvatar} ${team.teamName}" + if (isMyTeam) " (তোমার টিম)" else "",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Manager: ${team.managerName} • ${team.memberNames.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ChorTextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text(
                                text = "${numberFormat.format(team.totalScore)} Virtual Coins",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = ChorGold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumCard(
    medal: String,
    entry: ScoreEntry?,
    accent: Color,
    numberFormat: NumberFormat,
    isFirst: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
        modifier = modifier
            .height(if (isFirst) 128.dp else 112.dp)
            .border(if (isFirst) 2.dp else 1.dp, accent, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = medal,
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                fontWeight = FontWeight.Bold,
                fontSize = if (isFirst) 14.sp else 12.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = entry?.username ?: "---",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (entry != null) "${numberFormat.format(entry.coinsEarned)} Coins" else "0 Coins",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = ChorGold,
                fontSize = 12.sp
            )
        }
    }
}
