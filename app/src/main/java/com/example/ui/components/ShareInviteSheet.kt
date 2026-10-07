package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.ChorCardBorder
import com.example.ui.theme.ChorGold
import com.example.ui.theme.ChorNeonCyan
import com.example.ui.theme.ChorNeonEmerald
import com.example.ui.theme.ChorSurfaceDark
import com.example.ui.theme.ChorSurfaceElevated
import com.example.ui.theme.ChorTextSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily

fun extractCodeFromLinkOrRaw(rawInput: String): String {
    val trimmed = rawInput.trim()
    if (trimmed.isEmpty()) return ""
    val afterSlash = if (trimmed.contains("/")) {
        trimmed.trimEnd('/').substringAfterLast("/")
    } else {
        trimmed
    }
    return afterSlash.substringBefore("?").trim()
}

fun buildReferralInviteLink(domain: String, referralCode: String): String {
    val cleanDomain = domain.trim().removeSuffix("/").ifBlank { "https://chor-game.com" }
    val cleanCode = referralCode.trim().ifBlank { "JR12345" }
    return "$cleanDomain/invite/$cleanCode"
}

fun buildCompetitionInviteLink(domain: String, roomCode: String): String {
    val cleanDomain = domain.trim().removeSuffix("/").ifBlank { "https://chor-game.com" }
    val cleanCode = roomCode.trim().ifBlank { "ABC123" }
    return "$cleanDomain/match/$cleanCode"
}

fun buildChallengeInviteLink(domain: String, challengeCode: String): String {
    val cleanDomain = domain.trim().removeSuffix("/").ifBlank { "https://chor-game.com" }
    val cleanCode = challengeCode.trim().ifBlank { "CHAL01" }
    return "$cleanDomain/challenge/$cleanCode"
}

fun buildReferralShareMessage(domain: String, referralCode: String): String {
    val link = buildReferralInviteLink(domain, referralCode)
    return "🎮 CHOR গেমে আসো!\n" +
        "২ মিনিটে কে বেশি Virtual Coins সংগ্রহ করতে পারে দেখি!\n" +
        "আমার সাথে কম্পিটিশনে খেলতে নিচের লিংকে ঢুকো 👇\n" +
        "$link\n" +
        "(Referral Code: $referralCode)"
}

fun copyTextToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
}

fun shareTextToPlatform(
    context: Context,
    message: String,
    targetPackage: String? = null
) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
        if (!targetPackage.isNullOrBlank()) {
            setPackage(targetPackage)
        }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        if (!targetPackage.isNullOrBlank()) {
            context.startActivity(sendIntent)
        } else {
            val chooser = Intent.createChooser(sendIntent, "বন্ধুকে আমন্ত্রণ জানান").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    } catch (_: Exception) {
        // Fallback to system share sheet if specific social app isn't installed on emulator/device
        val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        val chooser = Intent.createChooser(fallbackIntent, "বন্ধুকে আমন্ত্রণ জানান").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShareInviteDialog(
    title: String = "🤝 বন্ধুকে খেলতে ডাকুন",
    subtitle: String = "তোমার বন্ধুদের আমন্ত্রণ জানাও এবং একসাথে কম্পিটিশনে খেলো!",
    codeLabel: String,
    codeValue: String,
    shareLink: String,
    shareMessage: String,
    onCopied: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ChorSurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, ChorGold, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = ChorGold,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChorTextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Code & Link Box
                Surface(
                    color = ChorSurfaceElevated,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ChorCardBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = codeLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ChorNeonCyan
                                )
                                Text(
                                    text = codeValue,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = ChorGold,
                                    fontSize = 20.sp
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    copyTextToClipboard(context, codeLabel, codeValue)
                                    onCopied("কোড কপি হয়েছে: $codeValue")
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("copy_code_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = ChorGold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Code", color = ChorGold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Invite Link:",
                            style = MaterialTheme.typography.labelMedium,
                            color = ChorTextSecondary
                        )
                        Text(
                            text = shareLink,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Preview of Auto-Generated Message
                Surface(
                    color = ChorSurfaceElevated.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = shareMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChorTextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Copy Link & Share Link Primary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            copyTextToClipboard(context, "Invite Link", shareLink)
                            onCopied("ইনভাইট লিংক কপি হয়েছে!")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChorGold,
                            contentColor = Color(0xFF1F1200)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_invite_link_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Link", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            shareTextToPlatform(context, shareMessage, null)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChorNeonEmerald,
                            contentColor = Color(0xFF041F10)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_invite_link_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Link", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "সোশ্যাল মিডিয়ায় শেয়ার করুন:",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                val platforms = listOf(
                     Triple("🟢 WhatsApp", "com.whatsapp", Color(0xFF25D366)),
                    Triple("🔵 Facebook", "com.facebook.katana", Color(0xFF1877F2)),
                    Triple("💬 Messenger", "com.facebook.orca", Color(0xFF00B2FF)),
                    Triple("✈️ Telegram", "org.telegram.messenger", Color(0xFF26A5E4)),
                    Triple("📲 System Share", null, ChorNeonCyan)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    platforms.forEach { (label, pkg, accent) ->
                        Surface(
                            color = ChorSurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                                .clickable {
                                    copyTextToClipboard(context, "CHOR Invite", shareMessage)
                                    shareTextToPlatform(context, shareMessage, pkg)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(accent)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
