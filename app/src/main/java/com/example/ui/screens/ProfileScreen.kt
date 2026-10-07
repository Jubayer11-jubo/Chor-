package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AchievementDef
import com.example.data.model.DefaultGameCatalog
import com.example.data.model.GameSession
import com.example.data.model.GameSettings
import com.example.data.model.ScoreEntry
import com.example.data.model.UserProfile
import com.example.ui.components.ShareInviteDialog
import com.example.ui.components.buildReferralShareMessage
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

@Composable
fun ProfileScreen(
    currentUserId: String,
    userProfile: UserProfile?,
    validScores: List<ScoreEntry>,
    userScores: List<ScoreEntry>,
    userSessions: List<GameSession> = emptyList(),
    settings: GameSettings = GameSettings(),
    soundEnabled: Boolean,
    musicEnabled: Boolean,
    onUpdateProfile: (String, String) -> Unit,
    onToggleSound: () -> Unit,
    onToggleMusic: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onSignOut: () -> Unit,
    onBackToHome: () -> Unit
) {
    BackHandler {
        onBackToHome()
    }

    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    var editingProfile by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var usernameInput by remember(userProfile?.username) {
        mutableStateOf(userProfile?.username ?: "চোর প্লেয়ার")
    }
    var selectedAvatarId by remember(userProfile?.avatarId) {
        mutableStateOf(userProfile?.avatarId ?: "avatar_hoodie")
    }

    val avatarOptions = listOf(
        "avatar_hoodie" to "🥷",
        "avatar_crown" to "👑",
        "avatar_fox" to "🦊",
        "avatar_tiger" to "🐯"
    )

    val referralCode = userProfile?.referralCode?.ifBlank { "JR12345" } ?: "JR12345"
    val domain = settings.productionDomain.trimEnd('/')
    val inviteLink = "$domain/invite/$referralCode"

    // Compute current All-Time Rank
    val rankedUsers = remember(validScores) {
        validScores
            .groupBy { it.userId }
            .mapNotNull { (_, list) -> list.maxByOrNull { it.coinsEarned } }
            .sortedByDescending { it.coinsEarned }
    }
    val rankIndex = rankedUsers.indexOfFirst { it.userId == currentUserId }
    val currentRankLabel = if (rankIndex >= 0) "#${rankIndex + 1}" else "Unranked"

    if (showInviteDialog) {
        ShareInviteDialog(
            title = "🤝 বন্ধুকে খেলতে ডাকুন (Invite Friends)",
            subtitle = "প্রতিটি সফল রেফারেলে বোনাস +${settings.referralRewardCoins} Virtual Coins!",
            shareLink = inviteLink,
            shareMessage = buildReferralShareMessage(
                domain = domain,
                referralCode = referralCode
            ),
            codeLabel = "রেফারেল কোড (Referral Code)",
            codeValue = referralCode,
            onCopied = {},
            onDismiss = { showInviteDialog = false }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 1. User Profile Header Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, ChorGold.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Image(
                            painter = painterResource(id = R.drawable.img_thief_avatar_1791341028042),
                            contentDescription = "Player Avatar",
                            modifier = Modifier
                                .size(86.dp)
                                .clip(CircleShape)
                                .border(3.dp, ChorGold, CircleShape)
                        )
                        val badgeEmoji = avatarOptions.find { it.first == (userProfile?.avatarId ?: "avatar_hoodie") }?.second ?: "🥷"
                        Surface(
                            color = ChorSurfaceElevated,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(30.dp)
                                .border(1.5.dp, ChorNeonCyan, CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = badgeEmoji, fontSize = 15.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = userProfile?.username ?: "চোর প্লেয়ার",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Current Rank: $currentRankLabel • Referral Code: $referralCode",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChorNeonCyan
                    )
                    if (!userProfile?.teamName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🛡️ টিম: ${userProfile?.teamName}",
                            style = MaterialTheme.typography.labelLarge,
                            color = ChorNeonEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!editingProfile) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { editingProfile = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("edit_profile_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = ChorGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("প্রোফাইল এডিট", color = ChorGold)
                            }
                            Button(
                                onClick = { showInviteDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ChorGold,
                                    contentColor = Color(0xFF1F1200)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("profile_invite_friend_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Invite Friends",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("বন্ধুকে ডাকুন", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ChorSurfaceElevated, RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            OutlinedTextField(
                                value = usernameInput,
                                onValueChange = { usernameInput = it.take(40) },
                                label = { Text("Username (খেলোয়াড়ের নাম)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_username_input")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                avatarOptions.forEach { (id, emoji) ->
                                    val selected = selectedAvatarId == id
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(if (selected) ChorGold.copy(alpha = 0.25f) else ChorSurfaceDark)
                                            .border(
                                                width = if (selected) 2.dp else 1.dp,
                                                color = if (selected) ChorGold else ChorCardBorder,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedAvatarId = id },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 22.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        onUpdateProfile(usernameInput, selectedAvatarId)
                                        editingProfile = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ChorNeonEmerald,
                                        contentColor = Color(0xFF041F10)
                                    ),
                                    modifier = Modifier.testTag("save_profile_button")
                                ) {
                                    Text("সেভ করুন", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Lifetime Statistics Grid
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ChorCardBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📊 খেলোয়াড়ের পরিসংখ্যান (Lifetime Stats)",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatTile("Total Coins", "🪙 ${numberFormat.format(userProfile?.totalCoins ?: 0L)}", ChorGold, Modifier.weight(1f))
                        ProfileStatTile("Total Games", "🎮 ${numberFormat.format(userProfile?.totalRounds ?: 0L)}", ChorNeonCyan, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatTile("Best Score", "🏆 ${numberFormat.format(userProfile?.bestScore ?: 0L)}", ChorNeonEmerald, Modifier.weight(1f))
                        ProfileStatTile("Highest Combo", "🔥 x${userProfile?.highestCombo ?: 1L}", Color(0xFFFF7043), Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatTile("🤝 Referrals", "${userProfile?.referralCount ?: 0L} জন", ChorGold, Modifier.weight(1f))
                        ProfileStatTile("🎪 Chanda Best", "🪙 ${numberFormat.format(userProfile?.chandaBestScore ?: 0L)}", ChorNeonCyan, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatTile("🥭 আম সংগ্রহ", "${userProfile?.mangoCount ?: 0L} টি", ChorGold, Modifier.weight(1f))
                        ProfileStatTile("🥥 ডাব সংগ্রহ", "${userProfile?.coconutCount ?: 0L} টি", ChorNeonEmerald, Modifier.weight(1f))
                        ProfileStatTile("🐄 গরু সংগ্রহ", "${userProfile?.cowCount ?: 0L} টি", ChorNeonCyan, Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Achievements Section
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ChorCardBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val unlockedSet = userProfile?.unlockedAchievements?.toSet() ?: emptySet()
                    Text(
                        text = "🏅 অ্যাচিভমেন্টস (Achievements ${unlockedSet.size}/${DefaultGameCatalog.achievements.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = ChorGold,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    DefaultGameCatalog.achievements.forEach { ach ->
                        val isUnlocked = unlockedSet.contains(ach.id)
                        val currentProgressVal = when (ach.id) {
                            "mango_master" -> (userProfile?.mangoCount ?: 0L).toInt()
                            "coconut_king" -> (userProfile?.coconutCount ?: 0L).toInt()
                            "coin_hunter" -> (userProfile?.totalCoins ?: 0L).toInt()
                            "combo_master" -> (userProfile?.highestCombo ?: 1L).toInt()
                            "chor_boss" -> (userProfile?.bestScore ?: 0L).toInt()
                            "speed_thief" -> if (isUnlocked) 20 else 0
                            "referral_star" -> (userProfile?.referralCount ?: 0L).toInt()
                            "team_champion" -> if (isUnlocked || !userProfile?.teamId.isNullOrBlank()) 1 else 0
                            "chanda_collector" -> (userProfile?.chandaBestScore ?: 0L).toInt()
                            else -> 0
                        }
                        AchievementRowItem(
                            achievement = ach,
                            isUnlocked = isUnlocked,
                            currentValue = currentProgressVal
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Sound & Music Settings + Admin Panel + Developer Attribution
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ChorCardBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔊 সাউন্ড ও সেটিংস (Audio & Controls)",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = null,
                                tint = ChorNeonEmerald
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("🔊 Sound Effects (সাউন্ড ইফেক্ট)", color = Color.White)
                        }
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { onToggleSound() }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (musicEnabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
                                contentDescription = null,
                                tint = ChorNeonCyan
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("🎵 Background Music (মিউজিক)", color = Color.White)
                        }
                        Switch(
                            checked = musicEnabled,
                            onCheckedChange = { onToggleMusic() }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Admin Panel Button
                    OutlinedButton(
                        onClick = onOpenAdminPanel,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("profile_admin_panel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Panel",
                            tint = ChorGold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin Panel (অ্যাডমিন কন্ট্রোল প্যানেল)",
                            color = ChorGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sign Out Button
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            tint = ChorDangerCrimson
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sign Out • লগ আউট করুন",
                            color = ChorDangerCrimson,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Developer: JR Jubayer Hasan • CHOR — চোর",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChorTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            if (userSessions.isNotEmpty() || userScores.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ChorCardBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📜 গেম সেশন হিস্ট্রি (Recent Game Sessions)",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (userSessions.isNotEmpty()) {
                            userSessions.take(6).forEach { session ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val modeTag = if (session.gameMode == "CHANDA") "🎪 উৎসব চাঁদা" else "🥷 চোর রাউন্ড"
                                    Text(
                                        text = "$modeTag • 📦 ${session.itemsCollected} items • 🔥 x${session.bestCombo}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ChorTextSecondary,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "+${numberFormat.format(session.coinsEarned)} 🪙",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = ChorGold
                                    )
                                }
                            }
                        } else {
                            userScores.take(5).forEach { score ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📦 ${score.itemsCollected} items • 🔥 x${score.bestCombo} (${score.mostCollectedItem})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ChorTextSecondary
                                    )
                                    Text(
                                        text = "+${numberFormat.format(score.coinsEarned)} 🪙",
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

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ProfileStatTile(
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
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = ChorTextSecondary,
                fontSize = 11.sp
            )
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
private fun AchievementRowItem(
    achievement: AchievementDef,
    isUnlocked: Boolean,
    currentValue: Int
) {
    val progress = (currentValue.toFloat() / achievement.targetValue.toFloat()).coerceIn(0f, 1f)
    Surface(
        color = if (isUnlocked) ChorSurfaceElevated else ChorSurfaceDark,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isUnlocked) ChorNeonEmerald else ChorCardBorder,
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isUnlocked) ChorGold.copy(alpha = 0.2f) else ChorSurfaceElevated,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = achievement.emoji, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.titleBn,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isUnlocked) ChorGold else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = achievement.descriptionBn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChorTextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (isUnlocked) 1f else progress },
                    color = if (isUnlocked) ChorNeonEmerald else ChorGold,
                    trackColor = ChorCardBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = if (isUnlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
                contentDescription = if (isUnlocked) "Unlocked" else "Locked",
                tint = if (isUnlocked) ChorNeonEmerald else ChorTextSecondary
            )
        }
    }
}
