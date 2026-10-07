package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.GameSettings
import com.example.ui.theme.ChorCardBorder
import com.example.ui.theme.ChorGold
import com.example.ui.theme.ChorNeonCyan
import com.example.ui.theme.ChorNeonEmerald
import com.example.ui.theme.ChorSurfaceDark
import com.example.ui.theme.ChorSurfaceElevated
import com.example.ui.theme.ChorTextSecondary

@Composable
fun ConfigurableAdBanner(
    settings: GameSettings,
    positionLabel: String,
    modifier: Modifier = Modifier
) {
    if (!settings.adsEnabled) return

    Surface(
        color = Color(0xFF10192D),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, ChorCardBorder.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = ChorGold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "AD • ${settings.adBannerSize}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ChorGold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = "Sponsor Banner",
                    tint = ChorNeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = settings.adBannerCode.ifBlank { "CHOR – চোর আর্কেড স্পনসর ব্যানার ($positionLabel)" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = positionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = ChorTextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun FirstTimeTutorialDialog(
    onDismissOnly: () -> Unit,
    onStartGameClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismissOnly) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, ChorGold, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = ChorGold.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "🎮 কীভাবে খেলবে? (HOW TO PLAY)",
                        style = MaterialTheme.typography.labelLarge,
                        color = ChorGold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "CHOR – চোর",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "২ মিনিট ৩০ সেকেন্ডের মজার কার্টুন চ্যালেঞ্জ!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChorTextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                TutorialStepRow(
                    stepNumber = "১",
                    emoji = "🕹️",
                    title = "চরিত্রটি চালাও",
                    subtitle = "বাম পাশের জয়স্টিক বা স্ক্রিনে টাচ করে গ্রামে ঘুরে বেড়াও।"
                )
                Spacer(modifier = Modifier.height(10.dp))
                TutorialStepRow(
                    stepNumber = "২",
                    emoji = "🥭",
                    title = "জিনিসের কাছে যাও",
                    subtitle = "আম, ডাব, ডিমের ঝুড়ি, গরু ও গুপ্তধনের কাছে যাও।"
                )
                Spacer(modifier = Modifier.height(10.dp))
                TutorialStepRow(
                    stepNumber = "৩",
                    emoji = "🖐️",
                    title = "চুরি/সংগ্রহ করো",
                    subtitle = "‘চুরি করো’ বা গরুর কাছে ‘গরু নিয়ে পালাও’ বাটন চাপো!"
                )
                Spacer(modifier = Modifier.height(10.dp))
                TutorialStepRow(
                    stepNumber = "৪",
                    emoji = "🪙",
                    title = "সময় শেষ হওয়ার আগে যত বেশি Virtual Coins সংগ্রহ করো!",
                    subtitle = "দ্রুত সংগ্রহ করে COMBO x2..x10 বাড়াও এবং গ্রামবাসীর (NPC) নজর এড়িয়ে চলো!"
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onStartGameClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChorNeonEmerald,
                        contentColor = Color(0xFF041F10)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("tutorial_start_game_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "খেলা শুরু করুন",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = onDismissOnly) {
                    Text(
                        text = "পরে খেলবো",
                        color = ChorTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialStepRow(
    stepNumber: String,
    emoji: String,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChorSurfaceElevated, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(ChorGold.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = 18.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ধাপ $stepNumber: ",
                    style = MaterialTheme.typography.labelLarge,
                    color = ChorNeonCyan
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = ChorTextSecondary,
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = ChorNeonEmerald.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp)
        )
    }
}
