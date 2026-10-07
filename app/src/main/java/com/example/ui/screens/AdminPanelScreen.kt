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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CollectibleItemConfig
import com.example.data.model.CompetitionRoom
import com.example.data.model.GameSettings
import com.example.data.model.ReferralRecord
import com.example.data.model.ScoreEntry
import com.example.data.model.TeamRecord
import com.example.data.model.UserProfile
import com.example.data.model.currentDayKey
import com.example.data.model.currentWeekKey
import com.example.ui.theme.ChorAlertAmber
import com.example.ui.theme.ChorCardBorder
import com.example.ui.theme.ChorDangerCrimson
import com.example.ui.theme.ChorGold
import com.example.ui.theme.ChorNeonCyan
import com.example.ui.theme.ChorNeonEmerald
import com.example.ui.theme.ChorSurfaceDark
import com.example.ui.theme.ChorSurfaceElevated
import com.example.ui.theme.ChorTextSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun AdminPanelScreen(
    isAdmin: Boolean,
    settings: GameSettings,
    items: List<CollectibleItemConfig>,
    allUsers: List<UserProfile>,
    allScores: List<ScoreEntry>,
    validScores: List<ScoreEntry>,
    allReferrals: List<ReferralRecord> = emptyList(),
    activeTeams: List<TeamRecord> = emptyList(),
    competitions: List<CompetitionRoom> = emptyList(),
    onSaveSettings: (GameSettings) -> Unit,
    onSaveItem: (CollectibleItemConfig) -> Unit,
    onSeedDefaultItems: () -> Unit,
    onToggleUserSuspend: (UserProfile) -> Unit,
    onDeleteScore: (String) -> Unit,
    onApproveFlaggedScore: (String) -> Unit,
    onResetDailyLeaderboard: (String) -> Unit,
    onResetWeeklyLeaderboard: (String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler {
        onClose()
    }

    var selectedSection by remember { mutableIntStateOf(0) }
    val sections = listOf(
        "📊 Dashboard",
        "⚙️ Game & Domain",
        "🤝 Referrals & Teams",
        "🥭 Items",
        "🏆 Leaderboard",
        "📢 Ads & Users"
    )
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_admin_panel_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Column {
                        Text(
                            text = "🛡️ CHOR Admin Panel",
                            style = MaterialTheme.typography.headlineMedium,
                            color = ChorGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isAdmin) "Verified Admin Access • Developer: JR Jubayer Hasan" else "Read-Only Preview (Admin Account Required for Writes)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isAdmin) ChorNeonEmerald else ChorAlertAmber,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Section Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sections.forEachIndexed { idx, label ->
                    val selected = selectedSection == idx
                    Surface(
                        color = if (selected) ChorGold else ChorSurfaceDark,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .border(1.dp, if (selected) ChorGold else ChorCardBorder, RoundedCornerShape(14.dp))
                            .clickable { selectedSection = idx }
                            .testTag("admin_tab_$idx")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Color(0xFF0A0F1D) else Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedSection) {
                    0 -> AdminDashboardSection(
                        settings = settings,
                        allUsers = allUsers,
                        scores = allScores.ifEmpty { validScores },
                        allReferrals = allReferrals,
                        activeTeams = activeTeams,
                        competitions = competitions,
                        numberFormat = numberFormat
                    )
                    1 -> AdminGameSettingsSection(
                        settings = settings,
                        onSaveSettings = onSaveSettings
                    )
                    2 -> AdminReferralsAndTeamsSection(
                        allReferrals = allReferrals,
                        allUsers = allUsers,
                        activeTeams = activeTeams,
                        competitions = competitions,
                        numberFormat = numberFormat
                    )
                    3 -> AdminItemsSection(
                        items = items,
                        onSaveItem = onSaveItem,
                        onSeedDefaultItems = onSeedDefaultItems
                    )
                    4 -> AdminLeaderboardSection(
                        scores = allScores.ifEmpty { validScores },
                        numberFormat = numberFormat,
                        onDeleteScore = onDeleteScore,
                        onApproveFlaggedScore = onApproveFlaggedScore,
                        onResetDaily = { onResetDailyLeaderboard(currentDayKey()) },
                        onResetWeekly = { onResetWeeklyLeaderboard(currentWeekKey()) }
                    )
                    5 -> AdminAdsAndUsersSection(
                        settings = settings,
                        allUsers = allUsers,
                        allScores = allScores.ifEmpty { validScores },
                        onSaveSettings = onSaveSettings,
                        onToggleUserSuspend = onToggleUserSuspend
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun AdminDashboardSection(
    settings: GameSettings,
    allUsers: List<UserProfile>,
    scores: List<ScoreEntry>,
    allReferrals: List<ReferralRecord>,
    activeTeams: List<TeamRecord>,
    competitions: List<CompetitionRoom>,
    numberFormat: NumberFormat
) {
    val todayKey = currentDayKey()
    val totalUsersCount = maxOf(allUsers.size, scores.map { it.userId }.distinct().size)
    val totalRoundsPlayed = maxOf(allUsers.sumOf { it.totalRounds }, scores.size.toLong())
    val totalVirtualCoins = maxOf(allUsers.sumOf { it.totalCoins }, scores.sumOf { it.coinsEarned })
    val highestScore = scores.maxOfOrNull { it.coinsEarned } ?: 0L
    val mostCollectedItem = scores.groupingBy { it.mostCollectedItem }.eachCount()
        .maxByOrNull { it.value }?.key ?: "🥭 আম"
    val dailyActivePlayers = scores.filter { it.periodDay == todayKey }.map { it.userId }.distinct().size
    val flaggedCount = scores.count { it.flaggedSuspicious || it.status == "flagged" }
    val totalReferrals = maxOf(allReferrals.size.toLong(), allUsers.sumOf { it.referralCount })

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ChorCardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "📈 লাইভ অ্যাডমিন ড্যাশবোর্ড (System Overview)",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminMetricCard("Total Users", "$totalUsersCount", ChorNeonCyan, Modifier.weight(1f))
                AdminMetricCard("Total Rounds", numberFormat.format(totalRoundsPlayed), ChorNeonEmerald, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminMetricCard("Total Virtual Coins", "🪙 ${numberFormat.format(totalVirtualCoins)}", ChorGold, Modifier.weight(1f))
                AdminMetricCard("Highest Score", "🏆 ${numberFormat.format(highestScore)}", ChorGold, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminMetricCard("Total Referrals", "🤝 $totalReferrals", ChorGold, Modifier.weight(1f))
                AdminMetricCard("Active Teams / Rooms", "🛡️ ${activeTeams.size} / 🏟️ ${competitions.size}", ChorNeonCyan, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminMetricCard("Most Collected Item", mostCollectedItem, ChorNeonEmerald, Modifier.weight(1f))
                AdminMetricCard("Daily Active Players", "$dailyActivePlayers", ChorNeonCyan, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminMetricCard(
                    "Ad Status",
                    if (settings.adsEnabled) "ENABLED (${settings.adBannerSize})" else "DISABLED",
                    if (settings.adsEnabled) ChorNeonEmerald else ChorDangerCrimson,
                    Modifier.weight(1f)
                )
                AdminMetricCard(
                    "Anti-Cheat Flags",
                    "$flaggedCount Suspicious",
                    if (flaggedCount > 0) ChorAlertAmber else ChorNeonEmerald,
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AdminMetricCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ChorSurfaceElevated,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, ChorCardBorder, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = ChorTextSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = accent,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun AdminGameSettingsSection(
    settings: GameSettings,
    onSaveSettings: (GameSettings) -> Unit
) {
    var roundDuration by remember(settings) { mutableLongStateOf(settings.roundDurationSeconds) }
    var maxCombo by remember(settings) { mutableLongStateOf(settings.maxComboMultiplier) }
    var npcDetection by remember(settings) { mutableLongStateOf(settings.npcDetectionRadius) }
    var specialFreq by remember(settings) { mutableLongStateOf(settings.specialItemFrequency) }
    var spawnRate by remember(settings) { mutableLongStateOf(settings.itemSpawnRate) }
    var speedMult by remember(settings) { mutableLongStateOf(settings.playerSpeedMultiplier) }
    var difficulty by remember(settings) { mutableStateOf(settings.difficulty) }
    var productionDomain by remember(settings) { mutableStateOf(settings.productionDomain) }
    var downloadAppUrl by remember(settings) { mutableStateOf(settings.downloadAppUrl) }
    var referralBonus by remember(settings) { mutableStateOf(settings.referralRewardCoins.toString()) }
    var maxTeamSize by remember(settings) { mutableLongStateOf(settings.maxTeamSize) }
    var chandaDuration by remember(settings) { mutableLongStateOf(settings.chandaDurationSeconds) }
    var chandaReward by remember(settings) { mutableLongStateOf(settings.chandaBaseReward) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ChorCardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "⚙️ Game, Referral & Team Settings",
                style = MaterialTheme.typography.titleLarge,
                color = ChorGold,
                fontWeight = FontWeight.Bold
            )

            // Production Domain & Download URL
            OutlinedTextField(
                value = productionDomain,
                onValueChange = { productionDomain = it.take(120) },
                label = { Text("Production Invite Domain (e.g., https://YOUR-DOMAIN.com)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_invite_domain_input")
            )

            OutlinedTextField(
                value = downloadAppUrl,
                onValueChange = { downloadAppUrl = it.take(200) },
                label = { Text("Download App Link (e.g., https://YOUR-DOMAIN.com/download)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_download_url_input")
            )

            OutlinedTextField(
                value = referralBonus,
                onValueChange = { referralBonus = it.filter { c -> c.isDigit() } },
                label = { Text("Referral Reward Virtual Coins (🪙)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Team Size Configuration (Default 2-5 players)
            Text("Max Team Size: $maxTeamSize players (Default: 2–5)", color = ChorNeonCyan, fontWeight = FontWeight.Bold)
            Slider(
                value = maxTeamSize.toFloat(),
                onValueChange = { maxTeamSize = it.roundToLong() },
                valueRange = 2f..10f
            )

            // Festival Chanda Bonus Mode settings
            Text("🎪 Chanda Bonus Duration: ${chandaDuration}s • Base Reward: +${chandaReward} 🪙", color = ChorGold, fontWeight = FontWeight.Bold)
            Slider(
                value = chandaDuration.toFloat(),
                onValueChange = { chandaDuration = it.roundToLong() },
                valueRange = 30f..300f
            )

            // Round Duration (Default 150s = 2m 30s)
            Text("Round Duration: ${roundDuration}s (${roundDuration / 60}m ${roundDuration % 60}s)", color = Color.White)
            Slider(
                value = roundDuration.toFloat(),
                onValueChange = { roundDuration = it.roundToLong() },
                valueRange = 60f..300f
            )

            // Max Combo Multiplier
            Text("Max Combo Multiplier: x$maxCombo", color = Color.White)
            Slider(
                value = maxCombo.toFloat(),
                onValueChange = { maxCombo = it.roundToLong() },
                valueRange = 2f..10f
            )

            // NPC Detection Radius %
            Text("NPC Detection Level: $npcDetection%", color = Color.White)
            Slider(
                value = npcDetection.toFloat(),
                onValueChange = { npcDetection = it.roundToLong() },
                valueRange = 50f..200f
            )

            // Special Item Frequency %
            Text("Special Item Frequency: $specialFreq%", color = Color.White)
            Slider(
                value = specialFreq.toFloat(),
                onValueChange = { specialFreq = it.roundToLong() },
                valueRange = 10f..100f
            )

            // Item Spawn Rate %
            Text("Item Spawn Rate: $spawnRate%", color = Color.White)
            Slider(
                value = spawnRate.toFloat(),
                onValueChange = { spawnRate = it.roundToLong() },
                valueRange = 50f..200f
            )

            // Player Movement Speed %
            Text("Movement Speed: $speedMult%", color = Color.White)
            Slider(
                value = speedMult.toFloat(),
                onValueChange = { speedMult = it.roundToLong() },
                valueRange = 50f..200f
            )

            // Difficulty Selector
            Text("Difficulty (কঠিনতার মাত্রা):", color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("EASY", "NORMAL", "HARD").forEach { diff ->
                    val selected = difficulty == diff
                    Surface(
                        color = if (selected) ChorNeonEmerald else ChorSurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { difficulty = diff }
                    ) {
                        Text(
                            text = diff,
                            color = if (selected) Color(0xFF041F10) else Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Button(
                onClick = {
                    onSaveSettings(
                        settings.copy(
                            roundDurationSeconds = roundDuration,
                            maxComboMultiplier = maxCombo,
                            npcDetectionRadius = npcDetection,
                            specialItemFrequency = specialFreq,
                            itemSpawnRate = spawnRate,
                            playerSpeedMultiplier = speedMult,
                            difficulty = difficulty,
                            productionDomain = productionDomain.ifBlank { "https://chor-game.com" },
                            downloadAppUrl = downloadAppUrl.ifBlank { "https://chor-game.com/download" },
                            referralRewardCoins = (referralBonus.toLongOrNull() ?: 100L).coerceIn(10L, 1000L),
                            maxTeamSize = maxTeamSize.coerceIn(2L, 10L),
                            chandaDurationSeconds = chandaDuration.coerceIn(30L, 300L),
                            chandaBaseReward = chandaReward.coerceIn(10L, 500L)
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChorNeonEmerald,
                    contentColor = Color(0xFF041F10)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_game_settings_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("গেম ও ডোমেইন সেটিংস সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AdminReferralsAndTeamsSection(
    allReferrals: List<ReferralRecord>,
    allUsers: List<UserProfile>,
    activeTeams: List<TeamRecord>,
    competitions: List<CompetitionRoom>,
    numberFormat: NumberFormat
) {
    val totalReferrals = maxOf(allReferrals.size.toLong(), allUsers.sumOf { it.referralCount })
    val successfulReferrals = allReferrals.count { it.status == "completed" }

    // Referral Leaderboard from referral records + user profiles
    val topReferrers = remember(allUsers, allReferrals) {
        val countsByCode = allReferrals.groupingBy { it.referrerCode }.eachCount()
        allUsers
            .map { user ->
                val count = maxOf(user.referralCount.toInt(), countsByCode[user.referralCode] ?: 0)
                Triple(user.username, user.referralCode, count)
            }
            .filter { it.third > 0 }
            .sortedByDescending { it.third }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ChorCardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "🤝 Referral Tracking & Analytics",
                style = MaterialTheme.typography.titleMedium,
                color = ChorGold,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminMetricCard("Total Referrals", "$totalReferrals", ChorGold, Modifier.weight(1f))
                AdminMetricCard("Successful Referrals", "${maxOf(successfulReferrals.toLong(), totalReferrals)}", ChorNeonEmerald, Modifier.weight(1f))
            }

            Text(
                text = "🏆 Referral Leaderboard",
                style = MaterialTheme.typography.labelLarge,
                color = ChorNeonCyan,
                fontWeight = FontWeight.Bold
            )

            if (topReferrers.isEmpty()) {
                Text("এখনো কোনো রেফারেল রেকর্ড নেই।", color = ChorTextSecondary, fontSize = 12.sp)
            } else {
                topReferrers.take(10).forEachIndexed { idx, (name, code, count) ->
                    Surface(
                        color = ChorSurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${idx + 1} $name ($code)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "$count Referrals",
                                fontFamily = JetBrainsMonoFontFamily,
                                color = ChorGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            if (allReferrals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "📋 Recent Referral Activity",
                    style = MaterialTheme.typography.labelLarge,
                    color = ChorGold,
                    fontWeight = FontWeight.Bold
                )
                allReferrals.take(15).forEach { ref ->
                    Surface(
                        color = ChorSurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Invited: ${ref.referredUsername} (Code: ${ref.referrerCode})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Status: ${ref.status.uppercase()}",
                                    color = ChorNeonEmerald,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "+${ref.rewardCoins} 🪙",
                                fontFamily = JetBrainsMonoFontFamily,
                                color = ChorGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Active Teams & Competitions Card
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ChorCardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "🛡️ Teams (${activeTeams.size}) & Private Rooms (${competitions.size})",
                style = MaterialTheme.typography.titleMedium,
                color = ChorNeonCyan,
                fontWeight = FontWeight.Bold
            )

            activeTeams.take(10).forEach { team ->
                Surface(
                    color = ChorSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${team.teamAvatar} ${team.teamName} (Manager: ${team.managerName})",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Members (${team.memberNames.size}): ${team.memberNames.joinToString(", ")}",
                                color = ChorTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "${numberFormat.format(team.totalScore)} 🪙",
                            fontFamily = JetBrainsMonoFontFamily,
                            color = ChorGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminItemsSection(
    items: List<CollectibleItemConfig>,
    onSaveItem: (CollectibleItemConfig) -> Unit,
    onSeedDefaultItems: () -> Unit
) {
    var editingItem by remember { mutableStateOf<CollectibleItemConfig?>(null) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ChorCardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🥭 Item Management (কয়েন মান ও স্পন)",
                    style = MaterialTheme.typography.titleMedium,
                    color = ChorGold,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = onSeedDefaultItems) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sync Defaults", fontSize = 12.sp)
                }
            }

            if (editingItem != null) {
                val current = editingItem!!
                var nameBn by remember(current) { mutableStateOf(current.nameBn) }
                var emoji by remember(current) { mutableStateOf(current.emoji) }
                var coinVal by remember(current) { mutableStateOf(current.coinValue.toString()) }
                var respawnSec by remember(current) { mutableStateOf(current.respawnTimeSeconds.toString()) }

                Surface(
                    color = ChorSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ChorGold, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("এডিট আইটেম: ${current.emoji} ${current.nameBn}", color = ChorGold, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = emoji,
                                onValueChange = { emoji = it.take(8) },
                                label = { Text("Icon") },
                                modifier = Modifier.weight(0.35f)
                            )
                            OutlinedTextField(
                                value = nameBn,
                                onValueChange = { nameBn = it.take(40) },
                                label = { Text("নাম (বাংলা)") },
                                modifier = Modifier.weight(0.65f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = coinVal,
                                onValueChange = { coinVal = it.filter { c -> c.isDigit() } },
                                label = { Text("Coin Value (🪙)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = respawnSec,
                                onValueChange = { respawnSec = it.filter { c -> c.isDigit() } },
                                label = { Text("Respawn (sec)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    onSaveItem(
                                        current.copy(
                                            nameBn = nameBn,
                                            emoji = emoji,
                                            coinValue = (coinVal.toLongOrNull() ?: current.coinValue).coerceIn(5L, 1000L),
                                            respawnTimeSeconds = (respawnSec.toLongOrNull() ?: current.respawnTimeSeconds).coerceIn(3L, 120L)
                                        )
                                    )
                                    editingItem = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ChorNeonEmerald, contentColor = Color(0xFF041F10))
                            ) {
                                Text("আপডেট করুন", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            items.sortedBy { it.coinValue }.forEach { item ->
                Surface(
                    color = ChorSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingItem = item }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = item.emoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("${item.nameBn} (${item.nameEn})", color = Color.White, fontWeight = FontWeight.Bold)
                                Text(
                                    "Respawn: ${item.respawnTimeSeconds}s • Effect: ${item.specialEffect}",
                                    color = ChorTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Text(
                            text = "+${item.coinValue} 🪙",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = ChorGold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminLeaderboardSection(
    scores: List<ScoreEntry>,
    numberFormat: NumberFormat,
    onDeleteScore: (String) -> Unit,
    onApproveFlaggedScore: (String) -> Unit,
    onResetDaily: () -> Unit,
    onResetWeekly: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ChorCardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "🏆 Leaderboard & Anti-Cheat Management",
                style = MaterialTheme.typography.titleMedium,
                color = ChorGold,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onResetDaily,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset Daily", color = ChorAlertAmber, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onResetWeekly,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset Weekly", color = ChorDangerCrimson, fontSize = 12.sp)
                }
            }

            scores.take(25).forEach { entry ->
                val isFlagged = entry.flaggedSuspicious || entry.status == "flagged"
                Surface(
                    color = ChorSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isFlagged) ChorDangerCrimson else ChorCardBorder,
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${entry.username} • ${numberFormat.format(entry.coinsEarned)} 🪙",
                                color = if (isFlagged) ChorDangerCrimson else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Items: ${entry.itemsCollected} • Combo: x${entry.bestCombo} • Status: ${entry.status.uppercase()}",
                                color = ChorTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Row {
                            if (isFlagged) {
                                IconButton(onClick = { onApproveFlaggedScore(entry.scoreId) }) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Approve", tint = ChorNeonEmerald)
                                }
                            }
                            IconButton(onClick = { onDeleteScore(entry.scoreId) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Score", tint = ChorDangerCrimson)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAdsAndUsersSection(
    settings: GameSettings,
    allUsers: List<UserProfile>,
    allScores: List<ScoreEntry>,
    onSaveSettings: (GameSettings) -> Unit,
    onToggleUserSuspend: (UserProfile) -> Unit
) {
    var adsEnabled by remember(settings) { mutableStateOf(settings.adsEnabled) }
    var topBanner by remember(settings) { mutableStateOf(settings.topBannerEnabled) }
    var bottomBanner by remember(settings) { mutableStateOf(settings.bottomBannerEnabled) }
    var resultAd by remember(settings) { mutableStateOf(settings.resultAdEnabled) }
    var rewardedAd by remember(settings) { mutableStateOf(settings.rewardedAdEnabled) }
    var rewardedBonus by remember(settings) { mutableStateOf(settings.rewardedCoinBonus.toString()) }
    var adCode by remember(settings) { mutableStateOf(settings.adBannerCode) }
    var adSize by remember(settings) { mutableStateOf(settings.adBannerSize) }
    var userSearchQuery by remember { mutableStateOf("") }

    // Advertisement Management Card
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ChorCardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "📢 Advertisement Management (Ad Settings)",
                style = MaterialTheme.typography.titleMedium,
                color = ChorNeonCyan,
                fontWeight = FontWeight.Bold
            )

            AdminSwitchRow("Master Enable Ads", adsEnabled) { adsEnabled = it }
            AdminSwitchRow("1. Top Banner Ad", topBanner) { topBanner = it }
            AdminSwitchRow("2. Bottom Banner Ad", bottomBanner) { bottomBanner = it }
            AdminSwitchRow("3. Result-Screen Ad", resultAd) { resultAd = it }
            AdminSwitchRow("4. Optional Rewarded Ad", rewardedAd) { rewardedAd = it }

            OutlinedTextField(
                value = adCode,
                onValueChange = { adCode = it.take(500) },
                label = { Text("Ad HTML / JavaScript / Promo Code") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = adSize,
                    onValueChange = { adSize = it.take(32) },
                    label = { Text("Banner Size (e.g., 320x50)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = rewardedBonus,
                    onValueChange = { rewardedBonus = it.filter { c -> c.isDigit() } },
                    label = { Text("Rewarded Bonus (🪙)") },
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = {
                    onSaveSettings(
                        settings.copy(
                            adsEnabled = adsEnabled,
                            topBannerEnabled = topBanner,
                            bottomBannerEnabled = bottomBanner,
                            resultAdEnabled = resultAd,
                            rewardedAdEnabled = rewardedAd,
                            rewardedCoinBonus = (rewardedBonus.toLongOrNull() ?: 50L).coerceIn(10L, 500L),
                            adBannerCode = adCode,
                            adBannerSize = adSize
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ChorNeonCyan, contentColor = Color(0xFF00262B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("অ্যাড সেটিংস সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
            }
        }
    }

    // User Management Card
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ChorCardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "👥 User Management (খেলোয়াড় ব্যবস্থাপনা)",
                style = MaterialTheme.typography.titleMedium,
                color = ChorGold,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = userSearchQuery,
                onValueChange = { userSearchQuery = it },
                label = { Text("Search users by username or referral code...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            val filteredUsers = allUsers.filter {
                userSearchQuery.isBlank() ||
                    it.username.contains(userSearchQuery, ignoreCase = true) ||
                    it.referralCode.contains(userSearchQuery, ignoreCase = true)
            }

            filteredUsers.forEach { user ->
                val userRoundCount = allScores.count { it.userId == user.userId }
                Surface(
                    color = ChorSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${user.username} (${user.referralCode})" + if (user.suspended) " [SUSPENDED]" else "",
                                color = if (user.suspended) ChorDangerCrimson else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Coins: ${user.totalCoins} 🪙 • Best: ${user.bestScore} • Referrals: ${user.referralCount} • Rounds: ${maxOf(user.totalRounds, userRoundCount.toLong())}",
                                color = ChorTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = { onToggleUserSuspend(user) }) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "Suspend or Unsuspend User",
                                tint = if (user.suspended) ChorNeonEmerald else ChorDangerCrimson
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
