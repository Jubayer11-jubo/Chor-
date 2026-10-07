package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsKabaddi
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameSettings
import com.example.ui.components.ConfigurableAdBanner
import com.example.ui.components.shareTextToPlatform
import com.example.ui.theme.ChorCardBorder
import com.example.ui.theme.ChorGold
import com.example.ui.theme.ChorMidnightBg
import com.example.ui.theme.ChorNeonCyan
import com.example.ui.theme.ChorNeonEmerald
import com.example.ui.theme.ChorSurfaceDark
import com.example.ui.theme.ChorSurfaceElevated
import com.example.ui.theme.ChorTextSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.RoundSummary
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ResultOverlay(
    summary: RoundSummary,
    settings: GameSettings,
    onPlayAgain: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onClaimRewardedBonus: () -> Unit,
    onCreateChallengeFromScore: (Long) -> Unit = {},
    onReturnHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    val animatedFinalCoins by animateIntAsState(
        targetValue = summary.coinsEarned.toInt(),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "final_coins_count"
    )
    val mins = summary.durationSeconds / 60
    val secs = summary.durationSeconds % 60
    val timeFormatted = String.format(Locale.US, "%d:%02d", mins, secs)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChorMidnightBg.copy(alpha = 0.9f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .border(2.5.dp, ChorGold, RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = ChorGold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (summary.gameMode == "CHANDA") {
                            "🎪 উৎসব চাঁদা তোলা শেষ! • TIME'S UP!"
                        } else {
                            "🎉 TIME'S UP! • সময় শেষ!"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = ChorGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                }

                if (summary.competitionTitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "⚔️ ${summary.competitionTitle}",
                        color = ChorNeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "তুমি আজ সংগ্রহ করেছো:",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ChorTextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "🪙 ${numberFormat.format(animatedFinalCoins)} Coins",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = ChorGold,
                    fontSize = 34.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Virtual Coins",
                    style = MaterialTheme.typography.labelMedium,
                    color = ChorNeonCyan
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Round Metrics Breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ResultMetricBox(
                        label = "📦 Items",
                        value = "${summary.itemsCollected}",
                        modifier = Modifier.weight(1f)
                    )
                    ResultMetricBox(
                        label = "🔥 Best Combo",
                        value = "x${summary.bestCombo}",
                        modifier = Modifier.weight(1f)
                    )
                    ResultMetricBox(
                        label = "⏱️ Time",
                        value = timeFormatted,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Optional Rewarded Ad Bonus
                if (settings.adsEnabled && settings.rewardedAdEnabled && !summary.rewardedBonusClaimed) {
                    Surface(
                        color = ChorSurfaceElevated,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ChorNeonCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Watch an ad to receive a small virtual coin bonus?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onClaimRewardedBonus,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ChorNeonCyan,
                                    contentColor = Color(0xFF00262B)
                                ),
                                modifier = Modifier.testTag("claim_rewarded_ad_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CardGiftcard,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "বিজ্ঞাপন দেখুন (+${settings.rewardedCoinBonus} 🪙 Virtual Coins)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Result-Screen Ad Slot
                if (settings.adsEnabled && settings.resultAdEnabled) {
                    ConfigurableAdBanner(
                        settings = settings,
                        positionLabel = "RESULT AD"
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 1v1 Challenge & Social Share Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onCreateChallengeFromScore(summary.coinsEarned) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("result_create_challenge_button")
                    ) {
                        Icon(Icons.Default.SportsKabaddi, contentDescription = null, tint = ChorGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("1v1 Challenge", color = ChorGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val shareMsg = "🎮 আমি CHOR গেমে ${summary.coinsEarned} Virtual Coins সংগ্রহ করেছি!\n" +
                                "তুমি কি আমার স্কোর ভাঙতে পারবে? এখনই খেলতে ঢুকো 👇\n" +
                                settings.productionDomain
                            shareTextToPlatform(context, shareMsg, null)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("result_share_score_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = ChorNeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share Score", color = ChorNeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary Action: PLAY AGAIN
                Button(
                    onClick = onPlayAgain,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChorNeonEmerald,
                        contentColor = Color(0xFF041F10)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("play_again_button")
                ) {
                    Icon(Icons.Default.Replay, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PLAY AGAIN • আবার খেলুন",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Action: LEADERBOARD
                OutlinedButton(
                    onClick = onOpenLeaderboard,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("result_leaderboard_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = ChorGold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LEADERBOARD • লিডারবোর্ড",
                        style = MaterialTheme.typography.titleMedium,
                        color = ChorGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Return to Home Action: HOME
                OutlinedButton(
                    onClick = onReturnHome,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("result_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = ChorNeonCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HOME • হোম স্ক্রিন",
                        style = MaterialTheme.typography.titleMedium,
                        color = ChorNeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultMetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ChorSurfaceElevated,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, ChorCardBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = ChorTextSecondary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp
            )
        }
    }
}
