package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CollectibleItemConfig
import com.example.data.model.GameSettings
import com.example.data.model.ScoreEntry
import com.example.data.model.UserProfile
import com.example.data.model.currentDayKey
import com.example.data.model.generateReferralCodeForUid
import com.example.ui.auth.GoogleSignInButton
import com.example.ui.components.ConfigurableAdBanner
import com.example.ui.components.ShareInviteDialog
import com.example.ui.components.buildReferralInviteLink
import com.example.ui.components.buildReferralShareMessage
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    userProfile: UserProfile?,
    validScores: List<ScoreEntry>,
    settings: GameSettings,
    activeItems: List<CollectibleItemConfig>,
    soundEnabled: Boolean,
    musicEnabled: Boolean,
    onStartGame: () -> Unit,
    onStartChandaBonusMode: () -> Unit,
    onOpenFriendCompetition: () -> Unit,
    onOpenInviteHub: () -> Unit,
    onOpenTutorial: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleMusic: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onShowMessage: (String) -> Unit,
    isAuthenticated: Boolean = true,
    onOpenSignIn: () -> Unit = {}
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)
    var continueAsPlayerDismissed by remember { mutableStateOf(false) }
    val todayKey = currentDayKey()
    val todayBestScore = validScores
        .filter { it.periodDay == todayKey }
        .maxOfOrNull { it.coinsEarned }
        ?: validScores.maxOfOrNull { it.coinsEarned }
        ?: userProfile?.bestScore
        ?: 0L

    val personalBest = userProfile?.bestScore ?: 0L
    val totalCoins = userProfile?.totalCoins ?: 0L
    val totalRounds = userProfile?.totalRounds ?: 0L
    val highestCombo = userProfile?.highestCombo ?: 1L

    val durationMinutes = settings.roundDurationSeconds / 60
    val durationRemSec = settings.roundDurationSeconds % 60
    val durationLabel = if (durationRemSec > 0) {
        "$durationMinutes মিনিট $durationRemSec সেকেন্ডে"
    } else {
        "$durationMinutes মিনিটে"
    }

    var showInviteDialog by remember { mutableStateOf(false) }
    val myReferralCode = userProfile?.referralCode?.ifBlank {
        generateReferralCodeForUid(userProfile.userId.ifBlank { "JR12345" })
    } ?: "JR12345"

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Configurable Ad Banner
            if (settings.adsEnabled && settings.topBannerEnabled) {
                ConfigurableAdBanner(
                    settings = settings,
                    positionLabel = "TOP BANNER"
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Top Action Row (Player Greeting + Audio / Tutorial / Admin Controls)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.img_thief_avatar_1791341028042),
                        contentDescription = "Player Avatar",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(2.dp, ChorGold, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = userProfile?.username ?: "চোর প্লেয়ার",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            if (!userProfile?.teamName.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = ChorNeonCyan.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "🛡️ ${userProfile?.teamName}",
                                        color = ChorNeonCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "🪙 ${numberFormat.format(totalCoins)} Virtual Coins • Code: $myReferralCode",
                            style = MaterialTheme.typography.labelMedium,
                            color = ChorGold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier.testTag("toggle_sound_button")
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (soundEnabled) "Sound ON" else "Sound OFF",
                            tint = if (soundEnabled) ChorNeonEmerald else ChorTextSecondary
                        )
                    }
                    IconButton(
                        onClick = onToggleMusic,
                        modifier = Modifier.testTag("toggle_music_button")
                    ) {
                        Icon(
                            imageVector = if (musicEnabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
                            contentDescription = if (musicEnabled) "Music ON" else "Music OFF",
                            tint = if (musicEnabled) ChorNeonCyan else ChorTextSecondary
                        )
                    }
                    IconButton(
                        onClick = onOpenTutorial,
                        modifier = Modifier.testTag("open_tutorial_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "খেলার নিয়ম (Tutorial)",
                            tint = ChorGold
                        )
                    }
                    IconButton(
                        onClick = onOpenAdminPanel,
                        modifier = Modifier.testTag("open_admin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Panel",
                            tint = ChorTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sign In / Continue Banner when not authenticated with Google
            if (!isAuthenticated && !continueAsPlayerDismissed) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, ChorNeonCyan.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔐 Sign In / Continue • ক্লাউড সিঙ্ক",
                                style = MaterialTheme.typography.titleMedium,
                                color = ChorNeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedButton(
                                onClick = { continueAsPlayerDismissed = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("home_continue_button")
                            ) {
                                Text(
                                    text = "Continue • খেলা চালিয়ে যান",
                                    color = ChorGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "লিডারবোর্ড, বন্ধুদের সাথে প্রতিযোগিতা ও রেফারেল কয়েন সংরক্ষণ করতে Google দিয়ে সাইন ইন করুন অথবা সরাসরি খেলা শুরু করুন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ChorTextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        GoogleSignInButton(
                            onAuthSuccess = {
                                onShowMessage("✅ সফলভাবে Google অ্যাকাউন্টে সাইন ইন হয়েছে!")
                            },
                            onAuthError = { err ->
                                onShowMessage(err)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else if (!isAuthenticated) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onOpenSignIn,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("home_open_signin_button")
                    ) {
                        Text(
                            text = "🔐 Sign In with Google",
                            color = ChorNeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Main Hero Start Card (Game Start Screen)
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, ChorGold.copy(alpha = 0.65f), RoundedCornerShape(26.dp))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(195.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_banner_1791341013479),
                            contentDescription = "CHOR Village World",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            ChorSurfaceDark.copy(alpha = 0.6f),
                                            ChorSurfaceDark
                                        )
                                    )
                                )
                        )

                        // Floating Mode Badge
                        Surface(
                            color = Color(0xCC0A0F1D),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(14.dp)
                                .border(1.dp, ChorNeonEmerald, RoundedCornerShape(50))
                        ) {
                            Text(
                                text = "🟢 কার্টুন ভিলেজ আর্কেড • ${String.format(Locale.US, "%02d:%02d", durationMinutes, durationRemSec)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = ChorNeonEmerald,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "CHOR • চোর",
                                style = MaterialTheme.typography.displayMedium,
                                color = ChorGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "“$durationLabel কত কয়েন করতে পারো?”",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "সময় শেষ হওয়ার আগে যত বেশি সম্ভব জিনিস সংগ্রহ করো এবং বেশি Virtual Coins অর্জন করো।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ChorTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Large Primary Start Game Button ("খেলা শুরু করুন")
                        Button(
                            onClick = onStartGame,
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ChorNeonEmerald,
                                contentColor = Color(0xFF041F10)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .testTag("start_game_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start Game",
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "খেলা শুরু করুন",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Multiplayer & Social Action Buttons ("বন্ধুদের সাথে খেলুন" + "বন্ধুকে খেলতে ডাকুন")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onOpenFriendCompetition,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ChorGold,
                                    contentColor = Color(0xFF1F1200)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("play_with_friends_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "বন্ধুদের সাথে খেলুন",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = { showInviteDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ChorNeonCyan,
                                    contentColor = Color(0xFF00262B)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("invite_friend_home_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "বন্ধুকে খেলতে ডাকুন",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Festival Bonus Round Button ("🎪 উৎসব মেলা চাঁদা তোলা রাউন্ড")
                        OutlinedButton(
                            onClick = onStartChandaBonusMode,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("start_chanda_mode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Celebration,
                                contentDescription = null,
                                tint = ChorGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🎪 উৎসব চাঁদা তোলা বোনাস রাউন্ড (${settings.chandaDurationSeconds}s)",
                                color = ChorGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Today's High Score Banner ("আজকের সেরা: X Coins")
                        Surface(
                            color = ChorSurfaceElevated,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ChorGold.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Today's Best",
                                    tint = ChorGold,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "আজকের সেরা: ${numberFormat.format(todayBestScore)} Coins",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ChorGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Player Quick Stats Grid (Personal Best, Total Coins, Total Rounds, Best Combo)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMiniCard(
                    title = "Personal Best",
                    value = "🪙 ${numberFormat.format(personalBest)}",
                    icon = Icons.Default.Star,
                    accent = ChorGold,
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    title = "Total Coins",
                    value = "🪙 ${numberFormat.format(totalCoins)}",
                    icon = Icons.Default.EmojiEvents,
                    accent = ChorNeonEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMiniCard(
                    title = "Total Rounds",
                    value = "${numberFormat.format(totalRounds)} রাউন্ড",
                    icon = Icons.Default.SportsEsports,
                    accent = ChorNeonCyan,
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    title = "Highest Combo",
                    value = "🔥 x$highestCombo",
                    icon = Icons.Default.LocalFireDepartment,
                    accent = Color(0xFFFF7043),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Collectible Village Valuables Showcase
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
                            text = "🎒 গ্রামের সংগ্রহযোগ্য জিনিস ও কয়েন মান",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Virtual Coins",
                            style = MaterialTheme.typography.labelMedium,
                            color = ChorNeonCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeItems.sortedBy { it.coinValue }.forEach { item ->
                            Surface(
                                color = ChorSurfaceElevated,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.border(
                                    width = 1.dp,
                                    color = if (item.specialEffect != "NONE") ChorGold.copy(alpha = 0.5f) else ChorCardBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = item.emoji, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.nameBn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "+${item.coinValue} 🪙",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = ChorGold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Configurable Ad Banner
            if (settings.adsEnabled && settings.bottomBannerEnabled) {
                Spacer(modifier = Modifier.height(14.dp))
                ConfigurableAdBanner(
                    settings = settings,
                    positionLabel = "BOTTOM BANNER"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "CHOR — চোর • Developer: JR Jubayer Hasan • Virtual Coins Only",
                style = MaterialTheme.typography.bodyMedium,
                color = ChorTextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showInviteDialog) {
            ShareInviteDialog(
                title = "🤝 বন্ধুকে খেলতে ডাকুন",
                subtitle = "বন্ধুকে ইনভাইট লিংক পাঠান এবং জিতে নিন +${settings.referralRewardCoins} 🪙 বোনাস!",
                codeLabel = "Referral Code",
                codeValue = myReferralCode,
                shareLink = buildReferralInviteLink(settings.productionDomain, myReferralCode),
                shareMessage = buildReferralShareMessage(settings.productionDomain, myReferralCode),
                onCopied = { msg -> onShowMessage(msg) },
                onDismiss = { showInviteDialog = false }
            )
        }
    }
}

@Composable
private fun StatMiniCard(
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
        modifier = modifier.border(1.dp, ChorCardBorder, RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accent.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChorTextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
