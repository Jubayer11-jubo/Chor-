package com.example.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CollectibleItemConfig
import com.example.data.model.CompetitionRoom
import com.example.data.model.GameSettings
import com.example.game.AlertState
import com.example.game.ChorVillageGameEngine
import com.example.game.PlayerAnimState
import com.example.game.RandomEventType
import com.example.game.StructureType
import com.example.game.WorldCollectible
import com.example.sound.GameSoundManager
import com.example.ui.theme.ChorAlertAmber
import com.example.ui.theme.ChorCardBorder
import com.example.ui.theme.ChorDangerCrimson
import com.example.ui.theme.ChorGold
import com.example.ui.theme.ChorMidnightBg
import com.example.ui.theme.ChorNeonCyan
import com.example.ui.theme.ChorNeonEmerald
import com.example.ui.theme.ChorSafeGreen
import com.example.ui.theme.ChorSurfaceDark
import com.example.ui.theme.ChorSurfaceElevated
import com.example.ui.theme.ChorTextSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.RoundSummary
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun GameplayScreen(
    settings: GameSettings,
    activeItems: List<CollectibleItemConfig>,
    personalBest: Long,
    soundEnabled: Boolean,
    roundSummary: RoundSummary?,
    soundManager: GameSoundManager,
    gameMode: String = "CLASSIC",
    playerUsername: String = "চোর প্লেয়ার",
    activeCompetition: CompetitionRoom? = null,
    onSyncCompetitionScore: (Long) -> Unit = {},
    onSendChallengeFromResult: () -> Unit = {},
    onInviteFriendsFromResult: () -> Unit = {},
    onToggleSound: () -> Unit,
    onRoundComplete: (
        coinsEarned: Long,
        itemsCollected: Int,
        bestCombo: Int,
        durationSeconds: Int,
        mostCollectedItem: String,
        mangoes: Int,
        coconuts: Int,
        cows: Int,
        speedThief: Boolean
    ) -> Unit,
    onPlayAgain: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onClaimRewardedBonus: () -> Unit,
    onExitToHome: () -> Unit
) {
    BackHandler {
        onExitToHome()
    }

    val engine = remember { ChorVillageGameEngine() }
    var roundSeed by remember { mutableIntStateOf(0) }
    var frameTick by remember { mutableIntStateOf(0) }

    // UI reactive snapshots of engine state
    var remainingSeconds by remember { mutableFloatStateOf(settings.roundDurationSeconds.toFloat()) }
    var liveCoins by remember { mutableLongStateOf(0L) }
    var lastCoinGain by remember { mutableLongStateOf(0L) }
    var liveItems by remember { mutableIntStateOf(0) }
    var liveCombo by remember { mutableIntStateOf(1) }
    var comboProgress by remember { mutableFloatStateOf(0f) }
    var alertState by remember { mutableStateOf(AlertState.SAFE) }
    var alertWarning by remember { mutableStateOf<String?>(null) }
    var activeEvent by remember { mutableStateOf<RandomEventType?>(null) }
    var nearbyInteractable by remember { mutableStateOf<WorldCollectible?>(null) }
    var sprintStamina by remember { mutableFloatStateOf(1f) }
    var specialCharge by remember { mutableFloatStateOf(0.6f) }
    var sneakyTimeLeft by remember { mutableFloatStateOf(0f) }
    var speedBoostLeft by remember { mutableFloatStateOf(0f) }
    var isRoundEnded by remember { mutableStateOf(false) }

    // Joystick input vector (-1..1)
    var joyX by remember { mutableFloatStateOf(0f) }
    var joyY by remember { mutableFloatStateOf(0f) }

    // Smooth animated coin counter for the Live Earnings HUD
    val animatedCoins by animateIntAsState(
        targetValue = liveCoins.toInt(),
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "live_coin_counter"
    )
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    // Initialize or reset round when roundSeed changes or when roundSummary becomes null on Play Again
    LaunchedEffect(roundSeed, roundSummary == null) {
        if (roundSummary == null) {
            engine.initializeRound(settings, activeItems)
            remainingSeconds = engine.totalSecondsRemaining
            liveCoins = 0L
            lastCoinGain = 0L
            liveItems = 0
            liveCombo = 1
            isRoundEnded = false
        }
    }

    // 60 FPS Game Loop
    LaunchedEffect(roundSeed, isRoundEnded, roundSummary) {
        if (isRoundEnded || roundSummary != null) return@LaunchedEffect
        var lastFrameNanos = 0L
        while (!isRoundEnded) {
            withFrameNanos { nowNanos ->
                val dt = if (lastFrameNanos == 0L) 0.016f else ((nowNanos - lastFrameNanos) / 1_000_000_000f)
                lastFrameNanos = nowNanos

                engine.stepFrame(
                    dtSec = dt,
                    joystickX = joyX,
                    joystickY = joyY,
                    settings = settings,
                    onSoundFootstep = { soundManager.playFootstep() },
                    onSoundCollect = { combo -> soundManager.playCollectSuccess(combo) },
                    onSoundCombo = { combo -> soundManager.playComboUp(combo) },
                    onSoundAlert = { soundManager.playAlertWarning() },
                    onSoundCaught = { soundManager.playCaughtPenalty() },
                    onSoundTick = { urgent -> soundManager.playCountdownTick(urgent) },
                    onRoundFinished = {
                        isRoundEnded = true
                        onRoundComplete(
                            engine.coinsEarned,
                            engine.itemsCollected,
                            engine.bestCombo,
                            engine.roundDurationSeconds,
                            engine.mostCollectedItemName(),
                            engine.itemCountsByType["আম"] ?: 0,
                            engine.itemCountsByType["ডাব"] ?: 0,
                            engine.itemCountsByType["গরু"] ?: 0,
                            engine.reachedSpeedThiefInRound
                        )
                    }
                )

                remainingSeconds = engine.totalSecondsRemaining
                if (engine.coinsEarned > liveCoins) {
                    lastCoinGain = engine.coinsEarned - liveCoins
                }
                liveCoins = engine.coinsEarned
                liveItems = engine.itemsCollected
                liveCombo = engine.currentCombo
                comboProgress = (engine.comboTimerSec / engine.maxComboWindowSec).coerceIn(0f, 1f)
                alertState = engine.overallAlertState
                alertWarning = engine.activeWarningMessage
                activeEvent = engine.activeEventBanner
                nearbyInteractable = engine.findNearestInteractable(165f)
                sprintStamina = engine.sprintStamina
                specialCharge = engine.specialCharge
                sneakyTimeLeft = engine.sneakyModeTimerSec
                speedBoostLeft = engine.speedBoostTimerSec
                frameTick++
            }
        }
    }

    val emojiPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 46f
        }
    }
    val labelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.WHITE
        }
    }
    val popupPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF173B23))
    ) {
        // 1. 2.5D Isometric Cartoon Village World Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("village_game_canvas")
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { tapOffset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = tapOffset.x - cx
                            val dy = tapOffset.y - cy
                            val dist = hypot(dx, dy).coerceAtLeast(1f)
                            if (dist > 28f) {
                                joyX = (dx / dist)
                                joyY = (dy / dist)
                                tryAwaitRelease()
                                joyX = 0f
                                joyY = 0f
                            }
                        }
                    )
                }
        ) {
            // Read frameTick so Canvas redraws smoothly every frame
            val tick = frameTick
            if (tick >= 0) {
                drawVillageWorld(
                    engine = engine,
                    nearbyTarget = nearbyInteractable,
                    emojiPaint = emojiPaint,
                    labelPaint = labelPaint,
                    popupPaint = popupPaint
                )
            }
        }

        // 2. Top Live Earnings HUD & Status Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Primary HUD Bar: TIME | COINS | ITEMS | BEST | EXIT
            val wholeSec = remainingSeconds.toInt().coerceAtLeast(0)
            val mins = wholeSec / 60
            val secs = wholeSec % 60
            val isLast30 = wholeSec in 1..30
            val timerText = String.format(Locale.US, "%02d:%02d", mins, secs)

            Surface(
                color = ChorMidnightBg.copy(alpha = 0.9f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = if (isLast30) ChorDangerCrimson else ChorGold.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TIME
                    HudMetricBox(
                        title = "TIME",
                        value = timerText,
                        valueColor = if (isLast30) ChorDangerCrimson else Color.White,
                        isLarge = true
                    )

                    // LIVE COINS
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "COINS",
                            style = MaterialTheme.typography.labelMedium,
                            color = ChorTextSecondary,
                            fontSize = 10.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🪙 ${numberFormat.format(animatedCoins)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = ChorGold,
                                fontSize = 19.sp
                            )
                            if (lastCoinGain > 0L) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+$lastCoinGain",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = ChorNeonEmerald,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // ITEMS
                    HudMetricBox(
                        title = "ITEMS",
                        value = "$liveItems",
                        valueColor = ChorNeonCyan
                    )

                    // BEST
                    HudMetricBox(
                        title = "BEST",
                        value = numberFormat.format(maxOf(personalBest, liveCoins)),
                        valueColor = ChorNeonEmerald
                    )

                    // Sound & Exit buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleSound,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = "Toggle Sound",
                                tint = if (soundEnabled) ChorNeonEmerald else ChorTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onExitToHome,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("exit_gameplay_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit to Home",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Secondary HUD Row: Alert Status Meter + Combo Multiplier + Active Buffs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Alert Status Pill (🟢 SAFE / 🟡 ALERT / 🔴 CAUGHT!)
                val alertBg = when (alertState) {
                    AlertState.SAFE -> ChorSafeGreen.copy(alpha = 0.2f)
                    AlertState.ALERT -> ChorAlertAmber.copy(alpha = 0.28f)
                    AlertState.CAUGHT -> ChorDangerCrimson.copy(alpha = 0.35f)
                }
                val alertBorder = when (alertState) {
                    AlertState.SAFE -> ChorSafeGreen
                    AlertState.ALERT -> ChorAlertAmber
                    AlertState.CAUGHT -> ChorDangerCrimson
                }
                Surface(
                    color = ChorMidnightBg.copy(alpha = 0.86f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.border(1.5.dp, alertBorder, RoundedCornerShape(50))
                ) {
                    Row(
                        modifier = Modifier
                            .background(alertBg)
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${alertState.badgeEmoji} ${alertState.labelEn} (${alertState.labelBn})",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }

                // Active Buffs (Sneaky Mode / Speed Boost)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (sneakyTimeLeft > 0f) {
                        Surface(
                            color = ChorMidnightBg.copy(alpha = 0.88f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.border(1.dp, ChorNeonCyan, RoundedCornerShape(50))
                        ) {
                            Text(
                                text = "🛡️ Sneaky ${sneakyTimeLeft.roundToInt()}s",
                                style = MaterialTheme.typography.labelLarge,
                                color = ChorNeonCyan,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (speedBoostLeft > 0f) {
                        Surface(
                            color = ChorMidnightBg.copy(alpha = 0.88f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.border(1.dp, ChorGold, RoundedCornerShape(50))
                        ) {
                            Text(
                                text = "🔥 Speed ${speedBoostLeft.roundToInt()}s",
                                style = MaterialTheme.typography.labelLarge,
                                color = ChorGold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Combo Badge (COMBO x2..x10)
                Surface(
                    color = ChorMidnightBg.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.border(
                        width = 1.5.dp,
                        color = if (liveCombo > 1) ChorGold else ChorCardBorder,
                        shape = RoundedCornerShape(14.dp)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🔥 COMBO x$liveCombo",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (liveCombo > 1) ChorGold else ChorTextSecondary,
                            fontSize = 13.sp
                        )
                        if (liveCombo > 1) {
                            LinearProgressIndicator(
                                progress = { comboProgress },
                                color = ChorGold,
                                trackColor = ChorSurfaceElevated,
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }

            // Festival Chanda Bonus Mode Banner
            if (gameMode == "CHANDA") {
                Surface(
                    color = Color(0xFF4A148C).copy(alpha = 0.92f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .border(1.5.dp, ChorGold, RoundedCornerShape(50))
                ) {
                    Text(
                        text = "🎪 উৎসব চাঁদা তোলা বোনাস রাউন্ড • NPC উৎসব স্টল থেকে ফান্ড সংগ্রহ করো!",
                        style = MaterialTheme.typography.labelLarge,
                        color = ChorGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }
            }

            // Live Competition Scoreboard (Multiplayer Room / Friend Match)
            if (activeCompetition != null) {
                val liveStandings = remember(activeCompetition.leaderboardSummary, liveCoins, playerUsername) {
                    val parsed = activeCompetition.leaderboardSummary.map { rawLine ->
                        val namePart = rawLine.substringBefore(":").trim()
                        val scorePart = rawLine.substringAfter(":", "0").filter { it.isDigit() }.toLongOrNull() ?: 0L
                        namePart to scorePart
                    }.toMutableList()
                    val existingIdx = parsed.indexOfFirst { it.first.equals(playerUsername, ignoreCase = true) }
                    if (existingIdx >= 0) {
                        val prev = parsed[existingIdx]
                        parsed[existingIdx] = prev.first to maxOf(prev.second, liveCoins)
                    } else {
                        parsed.add(playerUsername to liveCoins)
                    }
                    parsed.sortedByDescending { it.second }.take(4)
                }
                Surface(
                    color = ChorMidnightBg.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .align(Alignment.End)
                        .border(1.dp, ChorNeonCyan, RoundedCornerShape(14.dp))
                        .testTag("live_competition_scoreboard")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(
                            text = "🏆 LIVE: ${activeCompetition.title}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ChorNeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        liveStandings.forEachIndexed { idx, (name, score) ->
                            val medal = when (idx) {
                                0 -> "🥇"
                                1 -> "🥈"
                                2 -> "🥉"
                                else -> "#${idx + 1}"
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$medal $name",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (name == playerUsername) ChorGold else Color.White,
                                    fontWeight = if (name == playerUsername) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "${numberFormat.format(score)} 🪙",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = ChorGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Last 30 Seconds Warning Banner ("⚠️ শেষ ৩০ সেকেন্ড!")
            AnimatedVisibility(
                visible = wholeSec in 1..30,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = ChorDangerCrimson.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(
                        text = if (wholeSec <= 10) "⚠️ শেষ $wholeSec সেকেন্ড! দ্রুত চুরি করো!" else "⚠️ শেষ ৩০ সেকেন্ড!",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // NPC Alert Warning Message ("ধরা পড়ে গেলে কিন্তু শেষ!")
            AnimatedVisibility(
                visible = alertWarning != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = ChorAlertAmber.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(
                        text = "👀 ${alertWarning ?: ""}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF1F1200),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }
            }

            // Random Event Banner
            AnimatedVisibility(
                visible = activeEvent != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val ev = activeEvent
                if (ev != null) {
                    Surface(
                        color = ChorMidnightBg.copy(alpha = 0.94f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .border(2.dp, Color(ev.colorHex), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = ev.titleBn,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(ev.colorHex),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = ev.subtitleBn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Large 10-Second Final Countdown Center Badge
        val wholeSecCenter = remainingSeconds.toInt()
        if (wholeSecCenter in 1..10 && !isRoundEnded) {
            Surface(
                color = ChorDangerCrimson.copy(alpha = 0.25f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 150.dp)
                    .size(74.dp)
                    .border(2.5.dp, ChorDangerCrimson, CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$wholeSecCenter",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        color = Color.White
                    )
                }
            }
        }

        // 3. Bottom Mobile Controls: Left Virtual Joystick + Right STEAL / RUN / SPECIAL Buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Left Side: Virtual Joystick
            VirtualJoystick(
                onVectorChanged = { x, y ->
                    joyX = x
                    joyY = y
                }
            )

            // Right Side: Action Cluster (STEAL / চুরি করো, গরু নিয়ে পালাও, RUN, SPECIAL)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Secondary Buttons Row: RUN + SPECIAL
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // SPECIAL (Sneaky Cloak) Button
                    val specialReady = specialCharge >= 1f && sneakyTimeLeft <= 0f
                    Button(
                        onClick = {
                            if (engine.activateSneakySpecial()) {
                                soundManager.playComboUp(4)
                                specialCharge = engine.specialCharge
                                sneakyTimeLeft = engine.sneakyModeTimerSec
                            }
                        },
                        enabled = specialReady,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (specialReady) ChorNeonCyan else ChorSurfaceElevated,
                            contentColor = Color(0xFF00262B),
                            disabledContainerColor = ChorMidnightBg.copy(alpha = 0.8f),
                            disabledContentColor = ChorTextSecondary
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .border(
                                1.5.dp,
                                if (specialReady) ChorNeonCyan else ChorCardBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .testTag("special_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Special Sneaky Mode",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (specialReady) "SPECIAL 🛡️" else "🛡️ ${(specialCharge * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // RUN Button
                    val sprinting = engine.isSprintPressed
                    Button(
                        onClick = {
                            engine.isSprintPressed = !engine.isSprintPressed
                            soundManager.playButtonTap()
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sprinting) ChorGold else ChorMidnightBg.copy(alpha = 0.88f),
                            contentColor = if (sprinting) Color(0xFF1F1200) else Color.White
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .border(1.5.dp, ChorGold, RoundedCornerShape(16.dp))
                            .testTag("run_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                            contentDescription = "Run",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (sprinting) "RUN ON ⚡" else "RUN ${(sprintStamina * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Primary Large COLLECT / STEAL Button ("চুরি করো" or "গরু নিয়ে পালাও" or "উৎসব চাঁদা তোলো")
                val targetItem = nearbyInteractable
                val isCowTarget = targetItem?.config?.itemId == "cow" || targetItem?.config?.specialEffect == "COW_CHASE"
                val buttonLabelPrimary = when {
                    isCowTarget -> "গরু নিয়ে পালাও 🐄"
                    gameMode == "CHANDA" && targetItem != null -> "চাঁদা তোলো ${targetItem.config.emoji}"
                    gameMode == "CHANDA" -> "🎪 উৎসব চাঁদা তোলো"
                    targetItem != null -> "চুরি করো ${targetItem.config.emoji}"
                    else -> "চুরি করো / STEAL"
                }
                val buttonSubLabel = when {
                    isCowTarget -> "+${(targetItem?.config?.coinValue ?: 150L) * liveCombo} 🪙 (Follow & Collect!)"
                    targetItem != null -> "${targetItem.config.nameBn} • +${targetItem.config.coinValue * liveCombo} 🪙"
                    gameMode == "CHANDA" -> "উৎসব আইটেমের কাছে গিয়ে চাপো"
                    else -> "জিনিসের কাছে গিয়ে চাপো"
                }

                Button(
                    onClick = {
                        engine.attemptCollect(
                            settings = settings,
                            onSoundCollect = { combo -> soundManager.playCollectSuccess(combo) },
                            onSoundCowEscape = { soundManager.playCowEscapeSound() },
                            onSoundCombo = { combo -> soundManager.playComboUp(combo) }
                        )
                        liveCoins = engine.coinsEarned
                        liveItems = engine.itemsCollected
                        liveCombo = engine.currentCombo
                        if (activeCompetition != null && liveCoins > 0L) {
                            onSyncCompetitionScore(liveCoins)
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isCowTarget -> ChorGold
                            targetItem != null -> ChorNeonEmerald
                            else -> ChorSurfaceElevated.copy(alpha = 0.92f)
                        },
                        contentColor = if (targetItem != null) Color(0xFF041F10) else Color.White
                    ),
                    modifier = Modifier
                        .width(220.dp)
                        .height(74.dp)
                        .border(
                            width = 2.5.dp,
                            color = when {
                                isCowTarget -> Color.White
                                targetItem != null -> ChorGold
                                else -> ChorCardBorder
                            },
                            shape = RoundedCornerShape(24.dp)
                        )
                        .testTag("steal_collect_button")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCowTarget) Icons.Default.Bolt else Icons.Default.PanTool,
                                contentDescription = "Steal or Collect",
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = buttonLabelPrimary,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Text(
                            text = buttonSubLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 11.sp,
                            color = if (targetItem != null) Color(0xFF041F10).copy(alpha = 0.85f) else ChorTextSecondary
                        )
                    }
                }
            }
        }

        // 4. Final Result Screen Overlay when TIME'S UP!
        if (roundSummary != null) {
            ResultOverlay(
                summary = roundSummary,
                settings = settings,
                onPlayAgain = {
                    roundSeed++
                    onPlayAgain()
                },
                onOpenLeaderboard = onOpenLeaderboard,
                onClaimRewardedBonus = onClaimRewardedBonus,
                onCreateChallengeFromScore = { _ -> onSendChallengeFromResult() },
                onReturnHome = onExitToHome
            )
        }
    }
}

@Composable
private fun HudMetricBox(
    title: String,
    value: String,
    valueColor: Color,
    isLarge: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = ChorTextSecondary,
            fontSize = 10.sp
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            fontSize = if (isLarge) 20.sp else 16.sp
        )
    }
}

@Composable
private fun VirtualJoystick(
    onVectorChanged: (Float, Float) -> Unit
) {
    val maxRadiusPx = 110f
    var knobX by remember { mutableFloatStateOf(0f) }
    var knobY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .size(136.dp)
            .clip(CircleShape)
            .background(ChorMidnightBg.copy(alpha = 0.72f))
            .border(2.dp, ChorNeonCyan.copy(alpha = 0.65f), CircleShape)
            .testTag("virtual_joystick")
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { },
                    onDragEnd = {
                        knobX = 0f
                        knobY = 0f
                        onVectorChanged(0f, 0f)
                    },
                    onDragCancel = {
                        knobX = 0f
                        knobY = 0f
                        onVectorChanged(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val nextX = knobX + dragAmount.x
                        val nextY = knobY + dragAmount.y
                        val dist = hypot(nextX, nextY)
                        if (dist <= maxRadiusPx) {
                            knobX = nextX
                            knobY = nextY
                        } else {
                            knobX = (nextX / dist) * maxRadiusPx
                            knobY = (nextY / dist) * maxRadiusPx
                        }
                        onVectorChanged(
                            (knobX / maxRadiusPx).coerceIn(-1f, 1f),
                            (knobY / maxRadiusPx).coerceIn(-1f, 1f)
                        )
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Crosshair rings
        Box(
            modifier = Modifier
                .size(86.dp)
                .border(1.dp, ChorCardBorder.copy(alpha = 0.6f), CircleShape)
        )
        // Movable Thumb Knob
        Box(
            modifier = Modifier
                .offset { IntOffset(knobX.roundToInt(), knobY.roundToInt()) }
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(ChorNeonCyan, Color(0xFF007C91))
                    )
                )
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🕹️",
                fontSize = 20.sp
            )
        }
    }
}

private fun DrawScope.drawVillageWorld(
    engine: ChorVillageGameEngine,
    nearbyTarget: WorldCollectible?,
    emojiPaint: Paint,
    labelPaint: Paint,
    popupPaint: Paint
) {
    val camX = engine.playerX - size.width / 2f
    val camY = engine.playerY - size.height / 2f

    // 1. Lush Cartoon Village Grass Background & Grid
    drawRect(color = Color(0xFF1E4628))
    val gridSpacing = 140f
    val startGridX = ((camX / gridSpacing).toInt() * gridSpacing)
    val startGridY = ((camY / gridSpacing).toInt() * gridSpacing)
    var gx = startGridX
    while (gx < camX + size.width + gridSpacing) {
        var gy = startGridY
        while (gy < camY + size.height + gridSpacing) {
            val sx = gx - camX
            val sy = gy - camY
            drawCircle(
                color = Color(0xFF275833),
                radius = 12f,
                center = Offset(sx, sy)
            )
            gy += gridSpacing
        }
        gx += gridSpacing
    }

    // 2. Village Dirt Roads & Crossroads
    val roadColor = Color(0xFF8D6E48)
    val roadBorder = Color(0xFF6D5233)
    // Horizontal main village road (y = 1060..1200)
    drawRoundRect(
        color = roadBorder,
        topLeft = Offset(80f - camX, 1052f - camY),
        size = Size(engine.worldWidth - 160f, 156f),
        cornerRadius = CornerRadius(40f, 40f)
    )
    drawRoundRect(
        color = roadColor,
        topLeft = Offset(90f - camX, 1060f - camY),
        size = Size(engine.worldWidth - 180f, 140f),
        cornerRadius = CornerRadius(36f, 36f)
    )
    // Vertical main village road (x = 1030..1170)
    drawRoundRect(
        color = roadBorder,
        topLeft = Offset(1022f - camX, 80f - camY),
        size = Size(156f, engine.worldHeight - 160f),
        cornerRadius = CornerRadius(40f, 40f)
    )
    drawRoundRect(
        color = roadColor,
        topLeft = Offset(1030f - camX, 90f - camY),
        size = Size(140f, engine.worldHeight - 180f),
        cornerRadius = CornerRadius(36f, 36f)
    )

    // 3. Draw Village Structures (Fields, Gardens, Pond, Houses, Shops, Trees)
    for (st in engine.structures) {
        val sx = st.x - camX
        val sy = st.y - camY
        if (sx + st.width < -120f || sx - st.width > size.width + 120f ||
            sy + st.height < -120f || sy - st.height > size.height + 120f
        ) continue

        when (st.type) {
            StructureType.POND -> {
                drawRoundRect(
                    color = Color(0xFF4E342E),
                    topLeft = Offset(sx - st.width / 2f - 10f, sy - st.height / 2f - 10f),
                    size = Size(st.width + 20f, st.height + 20f),
                    cornerRadius = CornerRadius(48f, 48f)
                )
                drawRoundRect(
                    color = Color(0xFF0288D1),
                    topLeft = Offset(sx - st.width / 2f, sy - st.height / 2f),
                    size = Size(st.width, st.height),
                    cornerRadius = CornerRadius(42f, 42f)
                )
                val ripple = (sin(engine.animTimeSec * 2.5f) * 8f)
                drawCircle(
                    color = Color(0x66B3E5FC),
                    radius = 35f + ripple,
                    center = Offset(sx - 30f, sy - 10f),
                    style = Stroke(width = 3f)
                )
            }
            StructureType.GARDEN, StructureType.FIELD -> {
                drawRoundRect(
                    color = Color(st.roofColorHex).copy(alpha = 0.85f),
                    topLeft = Offset(sx - st.width / 2f, sy - st.height / 2f),
                    size = Size(st.width, st.height),
                    cornerRadius = CornerRadius(20f, 20f)
                )
                drawRoundRect(
                    color = Color(0xFF5D4037),
                    topLeft = Offset(sx - st.width / 2f, sy - st.height / 2f),
                    size = Size(st.width, st.height),
                    cornerRadius = CornerRadius(20f, 20f),
                    style = Stroke(width = 5f)
                )
                drawContext.canvas.nativeCanvas.drawText(
                    st.titleBn,
                    sx,
                    sy + 8f,
                    labelPaint
                )
            }
            StructureType.HOUSE, StructureType.SHOP, StructureType.COW_SHED -> {
                // 2.5D Drop shadow
                drawRoundRect(
                    color = Color(0x55000000),
                    topLeft = Offset(sx - st.width / 2f + 10f, sy - st.height / 2f + 14f),
                    size = Size(st.width, st.height),
                    cornerRadius = CornerRadius(22f, 22f)
                )
                // Building wall
                drawRoundRect(
                    color = Color(0xFFF5E6CC),
                    topLeft = Offset(sx - st.width / 2f, sy - st.height / 2f + 24f),
                    size = Size(st.width, st.height - 24f),
                    cornerRadius = CornerRadius(18f, 18f)
                )
                // 2.5D Pitched Roof
                drawRoundRect(
                    color = Color(st.roofColorHex),
                    topLeft = Offset(sx - st.width / 2f - 6f, sy - st.height / 2f - 8f),
                    size = Size(st.width + 12f, st.height * 0.62f),
                    cornerRadius = CornerRadius(20f, 20f)
                )
                drawContext.canvas.nativeCanvas.drawText(
                    st.titleBn,
                    sx,
                    sy - 4f,
                    labelPaint
                )
            }
            StructureType.MANGO_TREE, StructureType.COCONUT_TREE -> {
                // Tree shadow
                drawCircle(
                    color = Color(0x44000000),
                    radius = 48f,
                    center = Offset(sx + 8f, sy + 14f)
                )
                // Trunk
                drawRoundRect(
                    color = Color(0xFF5D4037),
                    topLeft = Offset(sx - 10f, sy),
                    size = Size(20f, 34f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                // Lush Canopy
                val canopyColor = if (st.type == StructureType.MANGO_TREE) Color(0xFF2E7D32) else Color(0xFF388E3C)
                drawCircle(
                    color = canopyColor,
                    radius = 46f,
                    center = Offset(sx, sy - 14f)
                )
                drawCircle(
                    color = Color(0xFF4CAF50),
                    radius = 32f,
                    center = Offset(sx - 8f, sy - 22f)
                )
            }
        }
    }

    // 4. Draw Collectible Objects
    val bobOffset = sin(engine.animTimeSec * 4.2f) * 5f
    for (item in engine.collectibles) {
        if (!item.isAvailable) continue
        val ix = item.x - camX
        val iy = item.y - camY
        if (ix < -80f || ix > size.width + 80f || iy < -80f || iy > size.height + 80f) continue

        val isTarget = nearbyTarget?.uid == item.uid
        val isSpecial = item.config.specialEffect != "NONE"

        // Aura circle
        drawCircle(
            color = when {
                isTarget -> ChorNeonEmerald.copy(alpha = 0.38f)
                isSpecial -> ChorGold.copy(alpha = 0.25f)
                else -> Color(0x33FFFFFF)
            },
            radius = if (isTarget) 44f else 34f,
            center = Offset(ix, iy)
        )
        if (isTarget) {
            drawCircle(
                color = ChorGold,
                radius = 46f,
                center = Offset(ix, iy),
                style = Stroke(width = 3.5f)
            )
        }

        // Emoji icon
        drawContext.canvas.nativeCanvas.drawText(
            item.config.emoji,
            ix,
            iy + 14f + bobOffset,
            emojiPaint
        )

        // Coin value pill below item
        val badgeText = if (item.isFollowingPlayer) {
            "হাম্বা! 🐄💨"
        } else {
            "+${item.config.coinValue} 🪙"
        }
        drawRoundRect(
            color = Color(0xCC0A0F1D),
            topLeft = Offset(ix - 46f, iy + 24f),
            size = Size(92f, 26f),
            cornerRadius = CornerRadius(12f, 12f)
        )
        drawContext.canvas.nativeCanvas.drawText(
            badgeText,
            ix,
            iy + 43f,
            labelPaint
        )
    }

    // 5. Draw NPC Villagers & Detection Radius
    val detectRadius = 215f * (if (engine.npcAlertEventTimerSec > 0f) 1.25f else 1f)
    for (npc in engine.npcs) {
        val nx = npc.x - camX
        val ny = npc.y - camY
        if (nx < -260f || nx > size.width + 260f || ny < -260f || ny > size.height + 260f) continue

        // Detection circle
        val zoneColor = when {
            npc.alertProgress >= 0.6f -> ChorDangerCrimson.copy(alpha = 0.2f)
            npc.alertProgress >= 0.25f -> ChorAlertAmber.copy(alpha = 0.16f)
            else -> Color(0x18FFFFFF)
        }
        drawCircle(
            color = zoneColor,
            radius = detectRadius,
            center = Offset(nx, ny)
        )
        drawCircle(
            color = if (npc.alertProgress >= 0.3f) ChorAlertAmber.copy(alpha = 0.6f) else Color(0x33FFFFFF),
            radius = detectRadius,
            center = Offset(nx, ny),
            style = Stroke(width = 2f)
        )

        // NPC Cartoon Character Body
        drawCircle(
            color = Color(0x55000000),
            radius = 24f,
            center = Offset(nx + 4f, ny + 22f)
        )
        drawRoundRect(
            color = Color(npc.shirtColorHex),
            topLeft = Offset(nx - 18f, ny - 10f),
            size = Size(36f, 32f),
            cornerRadius = CornerRadius(14f, 14f)
        )
        // NPC Head
        drawCircle(
            color = Color(0xFFFFCC80),
            radius = 16f,
            center = Offset(nx, ny - 22f)
        )
        // Name tag
        drawContext.canvas.nativeCanvas.drawText(
            npc.nameBn,
            nx,
            ny + 42f,
            labelPaint
        )

        // Speech Bubble when alerted ("এই! ওদিকে কী করছো?")
        val bubble = npc.speechBubble
        if (bubble != null) {
            val bw = 230f
            drawRoundRect(
                color = Color(0xEE131C31),
                topLeft = Offset(nx - bw / 2f, ny - 76f),
                size = Size(bw, 36f),
                cornerRadius = CornerRadius(14f, 14f)
            )
            drawRoundRect(
                color = ChorAlertAmber,
                topLeft = Offset(nx - bw / 2f, ny - 76f),
                size = Size(bw, 36f),
                cornerRadius = CornerRadius(14f, 14f),
                style = Stroke(width = 2.5f)
            )
            drawContext.canvas.nativeCanvas.drawText(
                bubble,
                nx,
                ny - 52f,
                labelPaint
            )
        }
    }

    // 6. Draw Footsteps
    for (fp in engine.footsteps) {
        val alpha = (1f - (fp.ageSec / fp.maxAgeSec)).coerceIn(0f, 1f)
        drawCircle(
            color = Color(0xFFD7CCC8).copy(alpha = alpha * 0.45f),
            radius = 7f,
            center = Offset(fp.x - camX, fp.y - camY)
        )
    }

    // 7. Draw Compass Guidance Arrow toward closest available item
    val closestAnywhere = engine.findClosestAvailableAnywhere()
    if (closestAnywhere != null) {
        val px = size.width / 2f
        val py = size.height / 2f
        val angle = atan2(closestAnywhere.y - engine.playerY, closestAnywhere.x - engine.playerX)
        val arrowDist = 68f
        val ax = px + cos(angle) * arrowDist
        val ay = py + sin(angle) * arrowDist
        drawCircle(
            color = ChorGold,
            radius = 8f,
            center = Offset(ax, ay)
        )
    }

    // 8. Draw the Playful Cartoon "CHOR – চোর" Character at Screen Center
    drawCartoonChorCharacter(
        cx = size.width / 2f,
        cy = size.height / 2f,
        animState = engine.playerAnimState,
        animTime = engine.animTimeSec,
        facingX = engine.playerFacingX,
        sneakyActive = engine.sneakyModeTimerSec > 0f,
        speedActive = engine.speedBoostTimerSec > 0f || engine.isSprintPressed
    )

    // 9. Draw Floating Coin & Event Popups ("+25 🪙", "COMBO x3 🔥")
    for (pop in engine.floatingPopups) {
        val progress = (pop.ageSec / pop.durationSec).coerceIn(0f, 1f)
        val px = pop.x - camX
        val py = (pop.y - camY) - (progress * 65f)
        popupPaint.color = pop.colorHex.toInt()
        popupPaint.alpha = ((1f - progress) * 255).toInt().coerceIn(0, 255)
        drawContext.canvas.nativeCanvas.drawText(
            pop.text,
            px,
            py,
            popupPaint
        )
    }
}

private fun DrawScope.drawCartoonChorCharacter(
    cx: Float,
    cy: Float,
    animState: PlayerAnimState,
    animTime: Float,
    facingX: Float,
    sneakyActive: Boolean,
    speedActive: Boolean
) {
    val walkBounce = when (animState) {
        PlayerAnimState.SNEAK_WALK -> sin(animTime * 11f) * 5f
        PlayerAnimState.RUN -> sin(animTime * 18f) * 8f
        PlayerAnimState.CELEBRATING -> -kotlin.math.abs(sin(animTime * 14f)) * 16f
        PlayerAnimState.SURPRISED -> -8f
        else -> sin(animTime * 3.5f) * 2f
    }

    val alpha = if (sneakyActive) 0.58f else 1f

    // Speed aura or Sneaky shield ring
    if (sneakyActive) {
        drawCircle(
            color = ChorNeonCyan.copy(alpha = 0.28f),
            radius = 48f,
            center = Offset(cx, cy)
        )
    } else if (speedActive) {
        drawCircle(
            color = ChorGold.copy(alpha = 0.25f),
            radius = 44f,
            center = Offset(cx, cy)
        )
    }

    if (animState == PlayerAnimState.COLLECTING) {
        drawCircle(
            color = ChorNeonEmerald,
            radius = 52f,
            center = Offset(cx, cy),
            style = Stroke(width = 4f)
        )
    }

    // Ground shadow
    drawCircle(
        color = Color(0x66000000),
        radius = 26f,
        center = Offset(cx, cy + 28f)
    )

    // Small Cartoon Backpack (behind torso)
    val packOffsetX = if (facingX >= 0f) -18f else 18f
    drawRoundRect(
        color = Color(0xFFFF8F00).copy(alpha = alpha),
        topLeft = Offset(cx + packOffsetX - 10f, cy - 12f + walkBounce),
        size = Size(20f, 26f),
        cornerRadius = CornerRadius(8f, 8f)
    )

    // Cartoon Feet (Sneaky Animated Stride)
    val stride = if (animState == PlayerAnimState.SNEAK_WALK || animState == PlayerAnimState.RUN) {
        sin(animTime * 15f) * 10f
    } else 0f
    drawCircle(
        color = Color(0xFFECEFF1).copy(alpha = alpha),
        radius = 8f,
        center = Offset(cx - 10f + stride, cy + 22f)
    )
    drawCircle(
        color = Color(0xFFECEFF1).copy(alpha = alpha),
        radius = 8f,
        center = Offset(cx + 10f - stride, cy + 22f)
    )

    // Dark Hoodie Torso
    drawRoundRect(
        color = Color(0xFF1A237E).copy(alpha = alpha),
        topLeft = Offset(cx - 20f, cy - 14f + walkBounce),
        size = Size(40f, 34f),
        cornerRadius = CornerRadius(14f, 14f)
    )

    // Hoodie Hood & Cartoon Head
    drawCircle(
        color = Color(0xFF0D1440).copy(alpha = alpha),
        radius = 21f,
        center = Offset(cx, cy - 26f + walkBounce)
    )
    drawCircle(
        color = Color(0xFFFFCC80).copy(alpha = alpha),
        radius = 15f,
        center = Offset(cx, cy - 25f + walkBounce)
    )

    // Playful Cartoon Eyes & Funny Expression
    val eyeShift = if (facingX >= 0f) 3f else -3f
    drawCircle(
        color = Color(0xFF0A0F1D),
        radius = 3f,
        center = Offset(cx - 5f + eyeShift, cy - 27f + walkBounce)
    )
    drawCircle(
        color = Color(0xFF0A0F1D),
        radius = 3f,
        center = Offset(cx + 5f + eyeShift, cy - 27f + walkBounce)
    )

    // Funny Grin or Surprise O-mouth
    if (animState == PlayerAnimState.SURPRISED) {
        drawCircle(
            color = ChorDangerCrimson,
            radius = 4.5f,
            center = Offset(cx + eyeShift, cy - 19f + walkBounce)
        )
    } else {
        val smilePath = Path().apply {
            moveTo(cx - 5f + eyeShift, cy - 20f + walkBounce)
            quadraticTo(
                cx + eyeShift,
                cy - 15f + walkBounce,
                cx + 6f + eyeShift,
                cy - 20f + walkBounce
            )
        }
        drawPath(
            path = smilePath,
            color = Color(0xFF0A0F1D),
            style = Stroke(width = 2.5f)
        )
    }
}
