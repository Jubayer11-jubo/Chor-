package com.example.data.model

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
    }
    val currentUser = auth?.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

fun generateReferralCodeForUid(uid: String): String {
    val clean = uid.uppercase(Locale.US).filter { it.isLetterOrDigit() }
    val suffix = if (clean.length >= 5) clean.take(5) else (clean + "12345").take(5)
    return "JR$suffix"
}

data class UserProfile(
    val userId: String = "",
    val username: String = "চোর প্লেয়ার",
    val avatarId: String = "avatar_hoodie",
    val totalCoins: Long = 0L,
    val totalRounds: Long = 0L,
    val bestScore: Long = 0L,
    val highestCombo: Long = 1L,
    val mangoCount: Long = 0L,
    val coconutCount: Long = 0L,
    val cowCount: Long = 0L,
    val unlockedAchievements: List<String> = emptyList(),
    val suspended: Boolean = false,
    val tutorialCompleted: Boolean = false,
    val referralCode: String = "",
    val referredByCode: String = "",
    val referralCount: Long = 0L,
    val teamId: String = "",
    val teamName: String = "",
    val chandaBestScore: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val code = referralCode.ifBlank { generateReferralCodeForUid(userId) }.take(32)
        return mapOf(
            "userId" to userId,
            "username" to username.take(40).ifBlank { "চোর প্লেয়ার" },
            "avatarId" to avatarId.take(32).ifBlank { "avatar_hoodie" },
            "totalCoins" to totalCoins.coerceIn(0L, 100000000L),
            "totalRounds" to totalRounds.coerceIn(0L, 1000000L),
            "bestScore" to bestScore.coerceIn(0L, 50000L),
            "highestCombo" to highestCombo.coerceIn(1L, 100L),
            "mangoCount" to mangoCount.coerceIn(0L, 1000000L),
            "coconutCount" to coconutCount.coerceIn(0L, 1000000L),
            "cowCount" to cowCount.coerceIn(0L, 1000000L),
            "unlockedAchievements" to unlockedAchievements.take(20),
            "suspended" to false,
            "tutorialCompleted" to tutorialCompleted,
            "referralCode" to code,
            "referredByCode" to referredByCode.take(32),
            "referralCount" to referralCount.coerceIn(0L, 100000L),
            "teamId" to teamId.take(64),
            "teamName" to teamName.take(40),
            "chandaBestScore" to chandaBestScore.coerceIn(0L, 50000L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

data class ScoreEntry(
    val scoreId: String = "",
    val userId: String = "",
    val username: String = "চোর প্লেয়ার",
    val avatarId: String = "avatar_hoodie",
    val coinsEarned: Long = 0L,
    val itemsCollected: Long = 0L,
    val bestCombo: Long = 1L,
    val durationSeconds: Long = 150L,
    val mostCollectedItem: String = "আম",
    val periodDay: String = currentDayKey(),
    val periodWeek: String = currentWeekKey(),
    val flaggedSuspicious: Boolean = false,
    val status: String = "valid",
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "scoreId" to scoreId,
            "userId" to userId,
            "username" to username.take(40).ifBlank { "চোর প্লেয়ার" },
            "avatarId" to avatarId.take(32).ifBlank { "avatar_hoodie" },
            "coinsEarned" to coinsEarned.coerceIn(0L, 50000L),
            "itemsCollected" to itemsCollected.coerceIn(0L, 500L),
            "bestCombo" to bestCombo.coerceIn(1L, 100L),
            "durationSeconds" to durationSeconds.coerceIn(10L, 600L),
            "mostCollectedItem" to mostCollectedItem.take(64).ifBlank { "আম" },
            "periodDay" to periodDay.take(16).ifBlank { currentDayKey() },
            "periodWeek" to periodWeek.take(16).ifBlank { currentWeekKey() },
            "flaggedSuspicious" to flaggedSuspicious,
            "status" to if (flaggedSuspicious) "flagged" else "valid",
            "createdAt" to FieldValue.serverTimestamp()
        )
    }
}

data class GameSession(
    val sessionId: String = "",
    val userId: String = "",
    val username: String = "চোর প্লেয়ার",
    val gameMode: String = "CHOR", // CHOR or CHANDA
    val coinsEarned: Long = 0L,
    val itemsCollected: Long = 0L,
    val bestCombo: Long = 1L,
    val durationSeconds: Long = 150L,
    val roomCode: String = "",
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "sessionId" to sessionId,
            "userId" to userId,
            "username" to username.take(40).ifBlank { "চোর প্লেয়ার" },
            "gameMode" to if (gameMode in listOf("CHOR", "CHANDA")) gameMode else "CHOR",
            "coinsEarned" to coinsEarned.coerceIn(0L, 50000L),
            "itemsCollected" to itemsCollected.coerceIn(0L, 500L),
            "bestCombo" to bestCombo.coerceIn(1L, 100L),
            "durationSeconds" to durationSeconds.coerceIn(10L, 600L),
            "roomCode" to roomCode.take(32),
            "createdAt" to FieldValue.serverTimestamp()
        )
    }
}

data class ReferralRecord(
    val referralId: String = "",
    val referrerUserId: String = "",
    val referrerCode: String = "",
    val referredUserId: String = "",
    val referredUsername: String = "চোর প্লেয়ার",
    val rewardCoins: Long = 100L,
    val status: String = "completed", // completed or flagged
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "referralId" to referralId,
            "referrerUserId" to referrerUserId,
            "referrerCode" to referrerCode.take(32).padEnd(3, '0'),
            "referredUserId" to referredUserId,
            "referredUsername" to referredUsername.take(40).ifBlank { "চোর প্লেয়ার" },
            "rewardCoins" to rewardCoins.coerceIn(0L, 1000L),
            "status" to if (status in listOf("completed", "flagged")) status else "completed",
            "createdAt" to FieldValue.serverTimestamp()
        )
    }
}

data class FriendConnection(
    val friendshipId: String = "",
    val userId: String = "",
    val friendUserId: String = "",
    val friendUsername: String = "বন্ধু",
    val friendAvatarId: String = "avatar_hoodie",
    val friendBestScore: Long = 0L,
    val friendTeamName: String = "",
    val onlineStatus: String = "ONLINE", // ONLINE or OFFLINE
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "friendshipId" to friendshipId,
            "userId" to userId,
            "friendUserId" to friendUserId,
            "friendUsername" to friendUsername.take(40).ifBlank { "বন্ধু" },
            "friendAvatarId" to friendAvatarId.take(32).ifBlank { "avatar_hoodie" },
            "friendBestScore" to friendBestScore.coerceIn(0L, 50000L),
            "friendTeamName" to friendTeamName.take(40),
            "onlineStatus" to if (onlineStatus in listOf("ONLINE", "OFFLINE")) onlineStatus else "ONLINE",
            "createdAt" to FieldValue.serverTimestamp()
        )
    }
}

data class TeamRecord(
    val teamId: String = "",
    val teamCode: String = "",
    val teamName: String = "চোর বাহিনী",
    val teamAvatar: String = "🥷",
    val managerId: String = "",
    val managerName: String = "JR Jubayer",
    val memberIds: List<String> = emptyList(),
    val memberNames: List<String> = emptyList(),
    val totalScore: Long = 0L,
    val suspended: Boolean = false,
    val status: String = "active", // active or suspended
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "teamId" to teamId,
            "teamCode" to teamCode.take(32).ifBlank { "TEAM01" },
            "teamName" to teamName.trim().take(40).let { if (it.length < 2) "চোর বাহিনী" else it },
            "teamAvatar" to teamAvatar.take(16).ifBlank { "🥷" },
            "managerId" to managerId,
            "managerName" to managerName.take(40).ifBlank { "ম্যানেজার" },
            "memberIds" to memberIds.take(10),
            "memberNames" to memberNames.map { it.take(64) }.take(10),
            "totalScore" to totalScore.coerceIn(0L, 100000000L),
            "suspended" to false,
            "status" to "active",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

data class CompetitionRoom(
    val competitionId: String = "",
    val roomCode: String = "",
    val title: String = "Jubayer vs Friends",
    val managerId: String = "",
    val managerName: String = "চোর প্লেয়ার",
    val gameMode: String = "CHOR", // CHOR, CHANDA, TEAM_CHOR
    val status: String = "waiting", // waiting, active, completed, closed
    val durationSeconds: Long = 150L,
    val participantIds: List<String> = emptyList(),
    val leaderboardSummary: List<String> = emptyList(),
    val winnerName: String = "",
    val winnerScore: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "competitionId" to competitionId,
            "roomCode" to roomCode.take(32).ifBlank { "ABC123" },
            "title" to title.trim().take(60).let { if (it.length < 2) "Friend Match" else it },
            "managerId" to managerId,
            "managerName" to managerName.take(40).ifBlank { "চোর প্লেয়ার" },
            "gameMode" to if (gameMode in listOf("CHOR", "CHANDA", "TEAM_CHOR")) gameMode else "CHOR",
            "status" to if (status in listOf("waiting", "active", "completed", "closed")) status else "waiting",
            "durationSeconds" to durationSeconds.coerceIn(30L, 600L),
            "participantIds" to participantIds.take(10),
            "leaderboardSummary" to leaderboardSummary.map { it.take(80) }.take(10),
            "winnerName" to winnerName.take(40),
            "winnerScore" to winnerScore.coerceIn(0L, 500000L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

data class ChallengeMatch(
    val challengeId: String = "",
    val challengeCode: String = "",
    val creatorId: String = "",
    val creatorName: String = "চোর প্লেয়ার",
    val creatorScore: Long = 0L,
    val gameMode: String = "CHOR", // CHOR or CHANDA
    val accepterId: String = "",
    val accepterName: String = "",
    val accepterScore: Long = 0L,
    val winnerName: String = "",
    val status: String = "open", // open or completed
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "challengeId" to challengeId,
            "challengeCode" to challengeCode.take(32).ifBlank { "CHAL01" },
            "creatorId" to creatorId,
            "creatorName" to creatorName.take(40).ifBlank { "চোর প্লেয়ার" },
            "creatorScore" to creatorScore.coerceIn(0L, 50000L),
            "gameMode" to if (gameMode in listOf("CHOR", "CHANDA")) gameMode else "CHOR",
            "accepterId" to accepterId.take(128),
            "accepterName" to accepterName.take(40),
            "accepterScore" to accepterScore.coerceIn(0L, 50000L),
            "winnerName" to winnerName.take(40),
            "status" to if (status in listOf("open", "completed")) status else "open",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

data class AppNotification(
    val notificationId: String = "",
    val recipientUserId: String = "",
    val senderName: String = "CHOR গেম",
    val type: String = "INVITE", // INVITE, CHALLENGE, TEAM, ACHIEVEMENT, ROOM
    val title: String = "",
    val message: String = "",
    val actionCode: String = "",
    val read: Boolean = false,
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "notificationId" to notificationId,
            "recipientUserId" to recipientUserId,
            "senderName" to senderName.take(40).ifBlank { "CHOR গেম" },
            "type" to type.take(32).ifBlank { "INVITE" },
            "title" to title.take(80).ifBlank { "নতুন নোটিফিকেশন" },
            "message" to message.take(200).ifBlank { "গেমে নতুন আপডেট এসেছে!" },
            "actionCode" to actionCode.take(64),
            "read" to read,
            "createdAt" to FieldValue.serverTimestamp()
        )
    }
}

data class GameSettings(
    val configId: String = "global",
    val roundDurationSeconds: Long = 150L, // 2 minutes 30 seconds default
    val maxComboMultiplier: Long = 5L,
    val npcDetectionRadius: Long = 100L,
    val specialItemFrequency: Long = 50L,
    val itemSpawnRate: Long = 100L,
    val difficulty: String = "NORMAL",
    val playerSpeedMultiplier: Long = 100L,
    val adsEnabled: Boolean = true,
    val topBannerEnabled: Boolean = true,
    val bottomBannerEnabled: Boolean = true,
    val resultAdEnabled: Boolean = true,
    val rewardedAdEnabled: Boolean = true,
    val rewardedCoinBonus: Long = 50L,
    val adBannerCode: String = "CHOR 🪙 মেগা টুর্নামেন্ট চলছে! প্রতিদিন নতুন লিডারবোর্ড চ্যালেঞ্জ!",
    val adBannerSize: String = "320x50",
    val productionDomain: String = "https://chor-game.com",
    val downloadAppUrl: String = "https://chor-game.com/download",
    val referralRewardCoins: Long = 100L,
    val maxTeamSize: Long = 5L,
    val chandaDurationSeconds: Long = 90L,
    val chandaBaseReward: Long = 35L,
    val updatedAt: Timestamp? = null
) {
    fun toSaveMap(): Map<String, Any> {
        return mapOf(
            "configId" to configId,
            "roundDurationSeconds" to roundDurationSeconds.coerceIn(30L, 600L),
            "maxComboMultiplier" to maxComboMultiplier.coerceIn(2L, 10L),
            "npcDetectionRadius" to npcDetectionRadius.coerceIn(50L, 200L),
            "specialItemFrequency" to specialItemFrequency.coerceIn(10L, 100L),
            "itemSpawnRate" to itemSpawnRate.coerceIn(50L, 200L),
            "difficulty" to if (difficulty in listOf("EASY", "NORMAL", "HARD")) difficulty else "NORMAL",
            "playerSpeedMultiplier" to playerSpeedMultiplier.coerceIn(50L, 200L),
            "adsEnabled" to adsEnabled,
            "topBannerEnabled" to topBannerEnabled,
            "bottomBannerEnabled" to bottomBannerEnabled,
            "resultAdEnabled" to resultAdEnabled,
            "rewardedAdEnabled" to rewardedAdEnabled,
            "rewardedCoinBonus" to rewardedCoinBonus.coerceIn(10L, 500L),
            "adBannerCode" to adBannerCode.take(500),
            "adBannerSize" to adBannerSize.take(32).ifBlank { "320x50" },
            "productionDomain" to productionDomain.trim().removeSuffix("/").take(120).ifBlank { "https://chor-game.com" },
            "downloadAppUrl" to downloadAppUrl.trim().take(200).ifBlank { "https://chor-game.com/download" },
            "referralRewardCoins" to referralRewardCoins.coerceIn(10L, 1000L),
            "maxTeamSize" to maxTeamSize.coerceIn(2L, 10L),
            "chandaDurationSeconds" to chandaDurationSeconds.coerceIn(30L, 300L),
            "chandaBaseReward" to chandaBaseReward.coerceIn(10L, 500L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

data class CollectibleItemConfig(
    val itemId: String = "",
    val nameBn: String = "",
    val nameEn: String = "",
    val emoji: String = "🥭",
    val coinValue: Long = 10L,
    val spawnProbability: Long = 50L,
    val respawnTimeSeconds: Long = 8L,
    val specialEffect: String = "NONE", // NONE, COW_CHASE, SPEED_BOOST, TIME_BONUS, SNEAKY_MODE, JACKPOT
    val active: Boolean = true,
    val updatedAt: Timestamp? = null
) {
    fun toSaveMap(): Map<String, Any> {
        return mapOf(
            "itemId" to itemId,
            "nameBn" to nameBn.take(40).ifBlank { "আইটেম" },
            "nameEn" to nameEn.take(40).ifBlank { "Item" },
            "emoji" to emoji.take(16).ifBlank { "🥭" },
            "coinValue" to coinValue.coerceIn(5L, 1000L),
            "spawnProbability" to spawnProbability.coerceIn(1L, 100L),
            "respawnTimeSeconds" to respawnTimeSeconds.coerceIn(3L, 120L),
            "specialEffect" to if (specialEffect in listOf("NONE", "COW_CHASE", "SPEED_BOOST", "TIME_BONUS", "SNEAKY_MODE", "JACKPOT")) specialEffect else "NONE",
            "active" to active,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

data class AchievementDef(
    val id: String,
    val emoji: String,
    val titleBn: String,
    val titleEn: String,
    val descriptionBn: String,
    val targetValue: Int
)

object DefaultGameCatalog {
    val defaultItems: List<CollectibleItemConfig> = listOf(
        CollectibleItemConfig(
            itemId = "mango",
            nameBn = "আম",
            nameEn = "Mango",
            emoji = "🥭",
            coinValue = 10L,
            spawnProbability = 85L,
            respawnTimeSeconds = 5L,
            specialEffect = "NONE",
            active = true
        ),
        CollectibleItemConfig(
            itemId = "coconut",
            nameBn = "ডাব",
            nameEn = "Coconut",
            emoji = "🥥",
            coinValue = 20L,
            spawnProbability = 75L,
            respawnTimeSeconds = 6L,
            specialEffect = "NONE",
            active = true
        ),
        CollectibleItemConfig(
            itemId = "egg_basket",
            nameBn = "ডিমের ঝুড়ি",
            nameEn = "Egg Basket",
            emoji = "🥚",
            coinValue = 25L,
            spawnProbability = 65L,
            respawnTimeSeconds = 7L,
            specialEffect = "NONE",
            active = true
        ),
        CollectibleItemConfig(
            itemId = "crop_basket",
            nameBn = "ফসলের ঝুড়ি",
            nameEn = "Crop Basket",
            emoji = "🌾",
            coinValue = 30L,
            spawnProbability = 60L,
            respawnTimeSeconds = 8L,
            specialEffect = "NONE",
            active = true
        ),
        CollectibleItemConfig(
            itemId = "fruit_basket",
            nameBn = "ফলের ঝুড়ি",
            nameEn = "Fruit Basket",
            emoji = "🧺",
            coinValue = 35L,
            spawnProbability = 55L,
            respawnTimeSeconds = 8L,
            specialEffect = "NONE",
            active = true
        ),
        CollectibleItemConfig(
            itemId = "bag",
            nameBn = "স্কুল ব্যাগ",
            nameEn = "Bag",
            emoji = "🎒",
            coinValue = 40L,
            spawnProbability = 50L,
            respawnTimeSeconds = 9L,
            specialEffect = "NONE",
            active = true
        ),
        CollectibleItemConfig(
            itemId = "fictional_phone",
            nameBn = "খেলনা ফোন",
            nameEn = "Fictional Phone",
            emoji = "📱",
            coinValue = 50L,
            spawnProbability = 40L,
            respawnTimeSeconds = 11L,
            specialEffect = "NONE",
            active = true
        ),
        CollectibleItemConfig(
            itemId = "coin_bag",
            nameBn = "কয়েন ব্যাগ",
            nameEn = "Coin Bag",
            emoji = "🪙",
            coinValue = 75L,
            spawnProbability = 35L,
            respawnTimeSeconds = 12L,
            specialEffect = "NONE",
            active = true
        ),
        CollectibleItemConfig(
            itemId = "money_bag",
            nameBn = "ভার্চুয়াল মানি ব্যাগ",
            nameEn = "Virtual Money Bag",
            emoji = "💰",
            coinValue = 100L,
            spawnProbability = 28L,
            respawnTimeSeconds = 14L,
            specialEffect = "NONE",
            active = true
        ),
        CollectibleItemConfig(
            itemId = "golden_mango",
            nameBn = "সোনালী আম",
            nameEn = "Golden Mango",
            emoji = "⭐",
            coinValue = 100L,
            spawnProbability = 22L,
            respawnTimeSeconds = 16L,
            specialEffect = "SPEED_BOOST",
            active = true
        ),
        CollectibleItemConfig(
            itemId = "cow",
            nameBn = "গরু",
            nameEn = "Cow",
            emoji = "🐄",
            coinValue = 150L,
            spawnProbability = 25L,
            respawnTimeSeconds = 15L,
            specialEffect = "COW_CHASE",
            active = true
        ),
        CollectibleItemConfig(
            itemId = "mystery_box",
            nameBn = "রহস্য বক্স",
            nameEn = "Mystery Box",
            emoji = "🎁",
            coinValue = 120L,
            spawnProbability = 20L,
            respawnTimeSeconds = 18L,
            specialEffect = "TIME_BONUS",
            active = true
        ),
        CollectibleItemConfig(
            itemId = "treasure",
            nameBn = "গুপ্তধন",
            nameEn = "Fictional Treasure",
            emoji = "💎",
            coinValue = 250L,
            spawnProbability = 14L,
            respawnTimeSeconds = 22L,
            specialEffect = "JACKPOT",
            active = true
        )
    )

    val achievements: List<AchievementDef> = listOf(
        AchievementDef(
            id = "mango_master",
            emoji = "🥭",
            titleBn = "আম ওস্তাদ (Mango Master)",
            titleEn = "Mango Master",
            descriptionBn = "মোট ১০০টি আম সংগ্রহ করো",
            targetValue = 100
        ),
        AchievementDef(
            id = "coconut_king",
            emoji = "🥥",
            titleBn = "ডাবের রাজা (Coconut King)",
            titleEn = "Coconut King",
            descriptionBn = "মোট ১০০টি ডাব সংগ্রহ করো",
            targetValue = 100
        ),
        AchievementDef(
            id = "coin_hunter",
            emoji = "💰",
            titleBn = "কয়েন শিকারি (Coin Hunter)",
            titleEn = "Coin Hunter",
            descriptionBn = "মোট ১০,০০০ Virtual Coins অর্জন করো",
            targetValue = 10000
        ),
        AchievementDef(
            id = "combo_master",
            emoji = "🔥",
            titleBn = "কম্বো মাস্টার (Combo Master)",
            titleEn = "Combo Master",
            descriptionBn = "এক রাউন্ডে x10 কম্বো অর্জন করো",
            targetValue = 10
        ),
        AchievementDef(
            id = "speed_thief",
            emoji = "🏆",
            titleBn = "স্পিড চোর (Speed Thief)",
            titleEn = "Speed Thief",
            descriptionBn = "৩০ সেকেন্ডে ২০টি জিনিস সংগ্রহ করো",
            targetValue = 20
        ),
        AchievementDef(
            id = "chor_boss",
            emoji = "👑",
            titleBn = "চোর বস (Chor Boss)",
            titleEn = "Chor Boss",
            descriptionBn = "এক রাউন্ডে ৫,০০০ Virtual Coins অর্জন করো",
            targetValue = 5000
        ),
        AchievementDef(
            id = "team_champion",
            emoji = "🤝",
            titleBn = "টিম চ্যাম্পিয়ন (Team Champion)",
            titleEn = "Team Champion",
            descriptionBn = "টিম বা বন্ধুদের সাথে মাল্টিপ্লেয়ার কম্পিটিশনে খেলো",
            targetValue = 1
        ),
        AchievementDef(
            id = "chanda_collector",
            emoji = "🎪",
            titleBn = "উৎসব চাঁদা ওস্তাদ (Festival Star)",
            titleEn = "Festival Star",
            descriptionBn = "চাঁদা তোলা বোনাস রাউন্ডে ৫০০+ Virtual Coins সংগ্রহ করো",
            targetValue = 500
        )
    )
}

fun currentDayKey(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(Date())
}

fun currentWeekKey(): String {
    val cal = Calendar.getInstance(Locale.US)
    val year = cal.get(Calendar.YEAR)
    val week = cal.get(Calendar.WEEK_OF_YEAR)
    return String.format(Locale.US, "%04d-W%02d", year, week)
}
