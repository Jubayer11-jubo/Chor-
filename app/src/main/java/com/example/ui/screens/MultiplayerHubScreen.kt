package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppNotification
import com.example.data.model.ChallengeMatch
import com.example.data.model.CompetitionRoom
import com.example.data.model.FriendConnection
import com.example.data.model.GameSession
import com.example.data.model.GameSettings
import com.example.data.model.ReferralRecord
import com.example.data.model.TeamRecord
import com.example.data.model.UserProfile
import com.example.data.model.generateReferralCodeForUid
import com.example.ui.components.ShareInviteDialog
import com.example.ui.components.buildChallengeInviteLink
import com.example.ui.components.buildCompetitionInviteLink
import com.example.ui.components.buildReferralInviteLink
import com.example.ui.components.buildReferralShareMessage
import com.example.ui.components.copyTextToClipboard
import com.example.ui.components.extractCodeFromLinkOrRaw
import com.example.ui.components.shareTextToPlatform
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

@Composable
fun MultiplayerHubScreen(
    currentUserId: String,
    userProfile: UserProfile?,
    settings: GameSettings,
    referrals: List<ReferralRecord>,
    friends: List<FriendConnection>,
    teams: List<TeamRecord>,
    competitions: List<CompetitionRoom>,
    challenges: List<ChallengeMatch>,
    sessions: List<GameSession>,
    notifications: List<AppNotification>,
    initialSectionIndex: Int = 0,
    onApplyReferralCode: (String) -> Unit,
    onAddFriend: (String, String) -> Unit,
    onRemoveFriend: (String) -> Unit,
    onCreateTeam: (String, String) -> Unit,
    onJoinTeam: (String) -> Unit,
    onRemoveTeamMember: (TeamRecord, Int) -> Unit,
    onCreateRoom: (String, String) -> Unit,
    onJoinRoom: (String) -> Unit,
    onStartRoomMatch: (CompetitionRoom) -> Unit,
    onCloseRoom: (CompetitionRoom) -> Unit,
    onCreateChallenge: (Long, String) -> Unit,
    onAcceptChallenge: (ChallengeMatch) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    onBackToHome: () -> Unit
) {
    BackHandler {
        onBackToHome()
    }

    var selectedSection by remember(initialSectionIndex) { mutableIntStateOf(initialSectionIndex) }
    val sections = listOf(
        "🤝 Invite & Friends",
        "⚔️ Friend Room",
        "🛡️ Teams (টিম)",
        "🔥 1v1 & Sessions"
    )
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    var activeShareDialog by remember { mutableStateOf<ShareDialogData?>(null) }

    val myReferralCode = userProfile?.referralCode?.ifBlank {
        generateReferralCodeForUid(currentUserId)
    } ?: generateReferralCodeForUid(currentUserId)
    val myReferralLink = buildReferralInviteLink(settings.productionDomain, myReferralCode)
    val myReferralMessage = buildReferralShareMessage(settings.productionDomain, myReferralCode)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
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
                        text = "⚔️ মাল্টিপ্লেয়ার ও ফ্রেন্ডস হাব",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ChorGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "বন্ধুকে খেলতে ডাকুন • প্রাইভেট রুম • চোর বাহিনী টিম",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChorTextSecondary
                    )
                }
                Button(
                    onClick = {
                        activeShareDialog = ShareDialogData(
                            title = "🤝 বন্ধুকে খেলতে ডাকুন",
                            subtitle = "বন্ধু জয়েন করলেই পাবে +${settings.referralRewardCoins} 🪙 Virtual Coins!",
                            codeLabel = "Referral Code",
                            codeValue = myReferralCode,
                            shareLink = myReferralLink,
                            shareMessage = myReferralMessage
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChorNeonEmerald,
                        contentColor = Color(0xFF041F10)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("hub_invite_friends_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Invite", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Section Selector Tabs
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
                            .testTag("multiplayer_tab_$idx")
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
                    0 -> InviteAndFriendsTab(
                        userProfile = userProfile,
                        settings = settings,
                        myReferralCode = myReferralCode,
                        myReferralLink = myReferralLink,
                        myReferralMessage = myReferralMessage,
                        referrals = referrals,
                        friends = friends,
                        notifications = notifications,
                        numberFormat = numberFormat,
                        onOpenShareSheet = {
                            activeShareDialog = ShareDialogData(
                                title = "🤝 বন্ধুকে খেলতে ডাকুন",
                                subtitle = "বন্ধুকে ইনভাইট লিংক পাঠান এবং একসাথে খেলুন!",
                                codeLabel = "Referral Code",
                                codeValue = myReferralCode,
                                shareLink = myReferralLink,
                                shareMessage = myReferralMessage
                            )
                        },
                        onApplyReferralCode = onApplyReferralCode,
                        onAddFriend = onAddFriend,
                        onRemoveFriend = onRemoveFriend,
                        onMarkNotificationRead = onMarkNotificationRead,
                        onShowMessage = onShowMessage
                    )
                    1 -> FriendCompetitionRoomsTab(
                        currentUserId = currentUserId,
                        userProfile = userProfile,
                        settings = settings,
                        competitions = competitions,
                        onCreateRoom = onCreateRoom,
                        onJoinRoom = onJoinRoom,
                        onStartRoomMatch = onStartRoomMatch,
                        onCloseRoom = onCloseRoom,
                        onShareRoom = { room ->
                            val link = buildCompetitionInviteLink(settings.productionDomain, room.roomCode)
                            val msg = "🎮 CHOR প্রাইভেট কম্পিটিশন: \"${room.title}\"!\n" +
                                "২ মিনিটে কে বেশি Virtual Coins সংগ্রহ করতে পারে দেখি!\n" +
                                "রুম কোড: ${room.roomCode}\n" +
                                "আমার সাথে কম্পিটিশনে খেলতে নিচের লিংকে ঢুকো 👇\n$link"
                            activeShareDialog = ShareDialogData(
                                title = "⚔️ রুম ইনভাইট শেয়ার করুন",
                                subtitle = "Competition: ${room.title}",
                                codeLabel = "Room Code",
                                codeValue = room.roomCode,
                                shareLink = link,
                                shareMessage = msg
                            )
                        }
                    )
                    2 -> TeamsSystemTab(
                        currentUserId = currentUserId,
                        userProfile = userProfile,
                        settings = settings,
                        teams = teams,
                        numberFormat = numberFormat,
                        onCreateTeam = onCreateTeam,
                        onJoinTeam = onJoinTeam,
                        onRemoveTeamMember = onRemoveTeamMember,
                        onShareTeam = { team ->
                            val link = "${settings.productionDomain.trimEnd('/')}/team/${team.teamCode}"
                            val msg = "🛡️ আমাদের টিম \"${team.teamName}\"-এ যোগ দাও!\n" +
                                "Team Code: ${team.teamCode}\n" +
                                "CHOR গেমে টিম কম্পিটিশনে জিততে নিচের লিংকে ঢুকো 👇\n$link"
                            activeShareDialog = ShareDialogData(
                                title = "🛡️ টিম ইনভাইট শেয়ার করুন",
                                subtitle = "Team: ${team.teamName}",
                                codeLabel = "Team Code",
                                codeValue = team.teamCode,
                                shareLink = link,
                                shareMessage = msg
                            )
                        }
                    )
                    3 -> ChallengesAndSessionsTab(
                        currentUserId = currentUserId,
                        userProfile = userProfile,
                        settings = settings,
                        challenges = challenges,
                        sessions = sessions,
                        numberFormat = numberFormat,
                        onCreateChallenge = onCreateChallenge,
                        onAcceptChallenge = onAcceptChallenge,
                        onShareChallenge = { ch ->
                            val link = buildChallengeInviteLink(settings.productionDomain, ch.challengeCode)
                            val msg = "🔥 CHOR 1v1 চ্যালেঞ্জ!\n" +
                                "${ch.creatorName} স্কোর করেছে ${ch.creatorScore} Virtual Coins!\n" +
                                "তুমি কি এই স্কোর ভাঙতে পারবে? চ্যালেঞ্জ কোড: ${ch.challengeCode}\n" +
                                "চ্যালেঞ্জ গ্রহণ করতে নিচের লিংকে ঢুকো 👇\n$link"
                            activeShareDialog = ShareDialogData(
                                title = "🔥 1v1 চ্যালেঞ্জ শেয়ার করুন",
                                subtitle = "Target Score: ${ch.creatorScore} 🪙",
                                codeLabel = "Challenge Code",
                                codeValue = ch.challengeCode,
                                shareLink = link,
                                shareMessage = msg
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        val dialogData = activeShareDialog
        if (dialogData != null) {
            ShareInviteDialog(
                title = dialogData.title,
                subtitle = dialogData.subtitle,
                codeLabel = dialogData.codeLabel,
                codeValue = dialogData.codeValue,
                shareLink = dialogData.shareLink,
                shareMessage = dialogData.shareMessage,
                onCopied = { msg -> onShowMessage(msg) },
                onDismiss = { activeShareDialog = null }
            )
        }
    }
}

private data class ShareDialogData(
    val title: String,
    val subtitle: String,
    val codeLabel: String,
    val codeValue: String,
    val shareLink: String,
    val shareMessage: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InviteAndFriendsTab(
    userProfile: UserProfile?,
    settings: GameSettings,
    myReferralCode: String,
    myReferralLink: String,
    myReferralMessage: String,
    referrals: List<ReferralRecord>,
    friends: List<FriendConnection>,
    notifications: List<AppNotification>,
    numberFormat: NumberFormat,
    onOpenShareSheet: () -> Unit,
    onApplyReferralCode: (String) -> Unit,
    onAddFriend: (String, String) -> Unit,
    onRemoveFriend: (String) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var referralInput by remember { mutableStateOf("") }
    var friendNameInput by remember { mutableStateOf("") }
    var friendCodeInput by remember { mutableStateOf("") }

    // 1. Referral Invite Card ("বন্ধুকে খেলতে ডাকুন")
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, ChorGold.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🤝 বন্ধুকে খেলতে ডাকুন (Invite & Earn)",
                        style = MaterialTheme.typography.titleLarge,
                        color = ChorGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "রেফারেল বোনাস: +${settings.referralRewardCoins} 🪙 Virtual Coins",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChorNeonEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = ChorSurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ChorCardBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Your Referral Code:", color = ChorTextSecondary, fontSize = 11.sp)
                            Text(
                                text = myReferralCode,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = ChorGold,
                                fontSize = 20.sp
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                copyTextToClipboard(context, "Invite Link", myReferralLink)
                                onShowMessage("ইনভাইট লিংক কপি হয়েছে!")
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = ChorGold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Link", color = ChorGold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Invite Link: $myReferralLink",
                        fontFamily = JetBrainsMonoFontFamily,
                        color = ChorNeonCyan,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary "বন্ধুকে খেলতে ডাকুন" Button
            Button(
                onClick = onOpenShareSheet,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChorNeonEmerald,
                    contentColor = Color(0xFF041F10)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("invite_friends_main_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "বন্ধুকে খেলতে ডাকুন (Share Invite)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Social Share Chips (WhatsApp, Facebook, Messenger, Telegram, System Share)
            val socialButtons = listOf(
                "WhatsApp" to "com.whatsapp",
                "Facebook" to "com.facebook.katana",
                "Messenger" to "com.facebook.orca",
                "Telegram" to "org.telegram.messenger",
                "System Share" to null
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                socialButtons.forEach { (label, pkg) ->
                    Surface(
                        color = ChorSurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .border(1.dp, ChorCardBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                copyTextToClipboard(context, "CHOR Invite", myReferralMessage)
                                shareTextToPlatform(context, myReferralMessage, pkg)
                            }
                    ) {
                        Text(
                            text = "📤 $label",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }

    // 2. Referral Tracking & Redeem Invite Link/Code Card
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ChorCardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🎁 আমন্ত্রণ কোড / লিংক যুক্ত করুন (Referral Tracking)",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            if (!userProfile?.referredByCode.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = ChorNeonEmerald.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ChorNeonEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "রেফারেল সংযুক্ত হয়েছে (Code: ${userProfile?.referredByCode}) • বোনাস কয়েন অর্জিত!",
                            color = ChorNeonEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                Text(
                    text = "বন্ধুর ইনভাইট লিংক বা রেফারেল কোড (যেমন: JR12345) পেস্ট করে +${settings.referralRewardCoins} 🪙 বোনাস নাও:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChorTextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = referralInput,
                        onValueChange = { referralInput = it },
                        label = { Text("Paste Invite Link or Code (JR12345)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("referral_code_input")
                    )
                    Button(
                        onClick = {
                            val detected = extractCodeFromLinkOrRaw(referralInput)
                            onApplyReferralCode(detected)
                            referralInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChorGold,
                            contentColor = Color(0xFF1F1200)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("apply_referral_button")
                    ) {
                        Text("Claim", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 3. Friends List & Add Friend Card
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ChorCardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "👥 বন্ধু তালিকা (Friends List • ${friends.size})",
                style = MaterialTheme.typography.titleMedium,
                color = ChorNeonCyan,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = friendNameInput,
                    onValueChange = { friendNameInput = it.take(40) },
                    label = { Text("বন্ধুর নাম (যেমন: Rahim)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = friendCodeInput,
                    onValueChange = { friendCodeInput = it.take(32) },
                    label = { Text("কোড (JR...)") },
                    singleLine = true,
                    modifier = Modifier.weight(0.8f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val code = extractCodeFromLinkOrRaw(friendCodeInput).ifBlank {
                        "friend_${System.currentTimeMillis() % 10000}"
                    }
                    if (friendNameInput.isNotBlank()) {
                        onAddFriend(code, friendNameInput)
                        friendNameInput = ""
                        friendCodeInput = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChorNeonCyan,
                    contentColor = Color(0xFF00262B)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_friend_button")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("বন্ধু তালিকায় যোগ করুন", fontWeight = FontWeight.Bold)
            }

            if (friends.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                friends.forEach { friend ->
                    Surface(
                        color = ChorSurfaceElevated,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (friend.onlineStatus == "ONLINE") ChorNeonEmerald else ChorTextSecondary)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = friend.friendUsername,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Code: ${friend.friendUserId} • ${friend.onlineStatus}",
                                        color = ChorTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            IconButton(onClick = { onRemoveFriend(friend.friendshipId) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Friend", tint = ChorDangerCrimson)
                            }
                        }
                    }
                }
            }
        }
    }

    // 4. In-App Notifications Card
    if (notifications.isNotEmpty()) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ChorCardBorder, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = ChorGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🔔 ইন-অ্যাপ নোটিফিকেশন (${notifications.count { !it.read }} নতুন)",
                        style = MaterialTheme.typography.titleMedium,
                        color = ChorGold,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                notifications.take(6).forEach { notif ->
                    Surface(
                        color = if (!notif.read) ChorSurfaceElevated else ChorSurfaceDark,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(
                                1.dp,
                                if (!notif.read) ChorGold.copy(alpha = 0.5f) else ChorCardBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onMarkNotificationRead(notif.notificationId) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(notif.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(notif.message, color = ChorTextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendCompetitionRoomsTab(
    currentUserId: String,
    userProfile: UserProfile?,
    settings: GameSettings,
    competitions: List<CompetitionRoom>,
    onCreateRoom: (String, String) -> Unit,
    onJoinRoom: (String) -> Unit,
    onStartRoomMatch: (CompetitionRoom) -> Unit,
    onCloseRoom: (CompetitionRoom) -> Unit,
    onShareRoom: (CompetitionRoom) -> Unit
) {
    var roomTitleInput by remember {
        mutableStateOf("${userProfile?.username ?: "Jubayer"} vs Friends")
    }
    var selectedMode by remember { mutableStateOf("CHOR") }
    var joinRoomInput by remember { mutableStateOf("") }

    // 1. Create Private Competition Room Card
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, ChorNeonCyan.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⚔️ বন্ধুদের সাথে খেলুন (Private Competition Room)",
                style = MaterialTheme.typography.titleLarge,
                color = ChorNeonCyan,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "প্রাইভেট রুম তৈরি করলে তুমি হবে Room Manager। বন্ধুদের লিংক শেয়ার করে প্রতিযোগিতা শুরু করো!",
                style = MaterialTheme.typography.bodyMedium,
                color = ChorTextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = roomTitleInput,
                onValueChange = { roomTitleInput = it.take(60) },
                label = { Text("Competition Name (যেমন: Jubayer vs Friends)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("room_title_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "CHOR" to "🥷 চোর ভিলেজ রাউন্ড",
                    "CHANDA" to "🎪 উৎসব চাঁদা তোলা"
                ).forEach { (modeKey, modeLabel) ->
                    val selected = selectedMode == modeKey
                    Surface(
                        color = if (selected) ChorGold else ChorSurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedMode = modeKey }
                    ) {
                        Text(
                            text = modeLabel,
                            color = if (selected) Color(0xFF1F1200) else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { onCreateRoom(roomTitleInput, selectedMode) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChorNeonEmerald,
                    contentColor = Color(0xFF041F10)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("create_room_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Private Room (রুম তৈরি করুন)", fontWeight = FontWeight.Bold)
            }
        }
    }

    // 2. Join Competition by Link or Code
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ChorCardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🔗 প্রাইভেট রুমে যোগ দিন (Join Competition)",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = joinRoomInput,
                    onValueChange = { joinRoomInput = it },
                    label = { Text("Paste Match Link or Room Code (RM...)") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("join_room_input")
                )
                Button(
                    onClick = {
                        val code = extractCodeFromLinkOrRaw(joinRoomInput)
                        if (code.isNotBlank()) {
                            onJoinRoom(code)
                            joinRoomInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChorGold,
                        contentColor = Color(0xFF1F1200)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("join_competition_button")
                ) {
                    Text("Join Competition", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // 3. Active & Completed Private Rooms List
    competitions.forEach { room ->
        val isManager = room.managerId == currentUserId
        val inviteLink = buildCompetitionInviteLink(settings.productionDomain, room.roomCode)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isManager) 1.5.dp else 1.dp,
                    color = if (isManager) ChorGold else ChorCardBorder,
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🏟️ ${room.title}",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChorGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Room Manager: ${room.managerName} • Mode: ${room.gameMode} • Status: ${room.status.uppercase()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ChorTextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Link: $inviteLink",
                            fontFamily = JetBrainsMonoFontFamily,
                            color = ChorNeonCyan,
                            fontSize = 11.sp
                        )
                    }
                    Surface(
                        color = ChorSurfaceElevated,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = room.roomCode,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = ChorGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Live / Final Scoreboard inside Room
                Surface(
                    color = ChorSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "📊 রুম স্কোরবোর্ড ও মেম্বার স্ট্যাটাস (${room.participantIds.size} জন):",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        room.leaderboardSummary.forEachIndexed { idx, line ->
                            Text(
                                text = "${idx + 1}. $line",
                                fontFamily = JetBrainsMonoFontFamily,
                                color = if (idx == 0) ChorGold else ChorTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        if (room.winnerName.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "🏆 WINNER: ${room.winnerName} (${room.winnerScore} Virtual Coins)",
                                color = ChorNeonEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onStartRoomMatch(room) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChorNeonEmerald,
                            contentColor = Color(0xFF041F10)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isManager) "Start Competition" else "Play Match",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { onShareRoom(room) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = ChorGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Invite", color = ChorGold, fontSize = 12.sp)
                    }

                    if (isManager) {
                        IconButton(onClick = { onCloseRoom(room) }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Room", tint = ChorDangerCrimson)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamsSystemTab(
    currentUserId: String,
    userProfile: UserProfile?,
    settings: GameSettings,
    teams: List<TeamRecord>,
    numberFormat: NumberFormat,
    onCreateTeam: (String, String) -> Unit,
    onJoinTeam: (String) -> Unit,
    onRemoveTeamMember: (TeamRecord, Int) -> Unit,
    onShareTeam: (TeamRecord) -> Unit
) {
    var teamNameInput by remember { mutableStateOf("চোর বাহিনী") }
    var selectedTeamAvatar by remember { mutableStateOf("🥷") }
    var joinTeamCodeInput by remember { mutableStateOf("") }
    val teamAvatars = listOf("🥷", "👑", "🐯", "🦊", "⚡", "💎")

    val myTeam = teams.find { it.teamId == userProfile?.teamId || it.memberIds.contains(currentUserId) }

    // 1. My Active Team or Create/Join Team Card
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, ChorGold.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (myTeam != null) {
                val isManager = myTeam.managerId == currentUserId
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = ChorSurfaceElevated,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(50.dp)
                                .border(2.dp, ChorGold, CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = myTeam.teamAvatar, fontSize = 24.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "TEAM: ${myTeam.teamName}",
                                style = MaterialTheme.typography.titleLarge,
                                color = ChorGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Team Manager: ${myTeam.managerName} • Code: ${myTeam.teamCode}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ChorNeonCyan,
                                fontSize = 12.sp
                            )
                        }
                    }
                    OutlinedButton(onClick = { onShareTeam(myTeam) }) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = ChorGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Invite", color = ChorGold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = ChorSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "👥 Members (${myTeam.memberNames.size}/${settings.maxTeamSize}):",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Team Score: 🪙 ${numberFormat.format(myTeam.totalScore)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                color = ChorGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        myTeam.memberNames.forEachIndexed { idx, memberName ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1}. $memberName" + if (idx == 0) " (Team Manager)" else "",
                                    color = if (idx == 0) ChorGold else Color.White,
                                    fontSize = 13.sp
                                )
                                if (isManager && idx > 0) {
                                    Text(
                                        text = "Remove",
                                        color = ChorDangerCrimson,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { onRemoveTeamMember(myTeam, idx) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "🛡️ নতুন টিম তৈরি করুন (Create Team • Max ${settings.maxTeamSize} Players)",
                    style = MaterialTheme.typography.titleLarge,
                    color = ChorGold,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = teamNameInput,
                    onValueChange = { teamNameInput = it.take(40) },
                    label = { Text("Team Name (যেমন: চোর বাহিনী)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("team_name_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    teamAvatars.forEach { emoji ->
                        val selected = selectedTeamAvatar == emoji
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (selected) ChorGold.copy(alpha = 0.25f) else ChorSurfaceElevated)
                                .border(if (selected) 2.dp else 1.dp, if (selected) ChorGold else ChorCardBorder, CircleShape)
                                .clickable { selectedTeamAvatar = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onCreateTeam(teamNameInput, selectedTeamAvatar) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChorNeonEmerald,
                        contentColor = Color(0xFF041F10)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("create_team_button")
                ) {
                    Icon(Icons.Default.Groups, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("টিম তৈরি করুন (Become Team Manager)", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = joinTeamCodeInput,
                        onValueChange = { joinTeamCodeInput = it },
                        label = { Text("Team Code (যেমন: TM1234)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("join_team_input")
                    )
                    Button(
                        onClick = {
                            val code = extractCodeFromLinkOrRaw(joinTeamCodeInput)
                            if (code.isNotBlank()) {
                                onJoinTeam(code)
                                joinTeamCodeInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChorGold,
                            contentColor = Color(0xFF1F1200)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Join Team", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 2. Team Competition Standings ("TEAM COMPETITION")
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ChorCardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆 টিম কম্পিটিশন র‍্যাঙ্কিং (Team Competition)",
                    style = MaterialTheme.typography.titleMedium,
                    color = ChorGold,
                    fontWeight = FontWeight.Bold
                )
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = ChorGold)
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (teams.isEmpty()) {
                Text(
                    text = "এখনো কোনো টিম তৈরি হয়নি! প্রথম টিম তৈরি করে #1 Winner হও!",
                    color = ChorTextSecondary,
                    fontSize = 13.sp
                )
            } else {
                val topTeam = teams.first()
                Surface(
                    color = ChorGold.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ChorGold, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🏆 CURRENT WINNER TEAM",
                                color = ChorGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${topTeam.teamAvatar} ${topTeam.teamName} (Manager: ${topTeam.managerName})",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "🪙 ${numberFormat.format(topTeam.totalScore)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = ChorGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                teams.forEachIndexed { index, team ->
                    Surface(
                        color = ChorSurfaceElevated,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "#${index + 1} ${team.teamAvatar}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = if (index == 0) ChorGold else Color.White
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(team.teamName, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Manager: ${team.managerName} • ${team.memberIds.size} Members • Code: ${team.teamCode}",
                                        color = ChorTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Text(
                                text = "${numberFormat.format(team.totalScore)} Coins",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = ChorGold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengesAndSessionsTab(
    currentUserId: String,
    userProfile: UserProfile?,
    settings: GameSettings,
    challenges: List<ChallengeMatch>,
    sessions: List<GameSession>,
    numberFormat: NumberFormat,
    onCreateChallenge: (Long, String) -> Unit,
    onAcceptChallenge: (ChallengeMatch) -> Unit,
    onShareChallenge: (ChallengeMatch) -> Unit
) {
    val targetScore = (userProfile?.bestScore ?: 500L).coerceAtLeast(100L)

    // 1. 1v1 Shareable Score Challenge Card
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, ChorGold.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🔥 ১v১ স্কোর চ্যালেঞ্জ (1v1 Shareable Challenge)",
                style = MaterialTheme.typography.titleLarge,
                color = ChorGold,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "তোমার সেরা স্কোর (${numberFormat.format(targetScore)} Virtual Coins) দিয়ে চ্যালেঞ্জ লিংক তৈরি করে বন্ধুকে পাঠাও!",
                style = MaterialTheme.typography.bodyMedium,
                color = ChorTextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { onCreateChallenge(targetScore, "CHOR") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChorNeonEmerald,
                    contentColor = Color(0xFF041F10)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("create_1v1_challenge_button")
            ) {
                Icon(Icons.Default.SportsKabaddi, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create 1v1 Challenge Link (${numberFormat.format(targetScore)} 🪙)", fontWeight = FontWeight.Bold)
            }

            if (challenges.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                challenges.take(10).forEach { ch ->
                    Surface(
                        color = ChorSurfaceElevated,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
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
                                    text = "${ch.creatorName}: ${numberFormat.format(ch.creatorScore)} 🪙 (${ch.gameMode})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                if (ch.status == "completed") {
                                    Text(
                                        text = "vs ${ch.accepterName} (${ch.accepterScore} 🪙) • 🏆 Winner: ${ch.winnerName}",
                                        color = ChorNeonEmerald,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "Code: ${ch.challengeCode} • Status: OPEN CHALLENGE",
                                        color = ChorNeonCyan,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = { onShareChallenge(ch) }) {
                                    Text("Share", color = ChorGold, fontSize = 11.sp)
                                }
                                if (ch.status == "open") {
                                    Button(
                                        onClick = { onAcceptChallenge(ch) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ChorGold,
                                            contentColor = Color(0xFF1F1200)
                                        )
                                    ) {
                                        Text("Beat Score", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 2. Game Sessions History from DataManager (/game_sessions)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ChorCardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📜 গেম সেশন রেকর্ড (DataManager • game_sessions)",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (sessions.isEmpty()) {
                Text(
                    text = "এখনো কোনো গেম সেশন রেকর্ড হয়নি। একটি রাউন্ড খেললেই এখানে দেখা যাবে!",
                    color = ChorTextSecondary,
                    fontSize = 12.sp
                )
            } else {
                sessions.take(8).forEach { sess ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${if (sess.gameMode == "CHANDA") "🎪 CHANDA" else "🥷 CHOR"} • 📦 ${sess.itemsCollected} items • 🔥 x${sess.bestCombo}" +
                                if (sess.roomCode.isNotBlank()) " • Room: ${sess.roomCode}" else "",
                            color = ChorTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "+${numberFormat.format(sess.coinsEarned)} 🪙",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = ChorGold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
