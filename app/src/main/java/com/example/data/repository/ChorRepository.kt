package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.ChallengeMatch
import com.example.data.model.CollectibleItemConfig
import com.example.data.model.CompetitionRoom
import com.example.data.model.DefaultGameCatalog
import com.example.data.model.FriendConnection
import com.example.data.model.GameSession
import com.example.data.model.GameSettings
import com.example.data.model.OperationType
import com.example.data.model.ReferralRecord
import com.example.data.model.ScoreEntry
import com.example.data.model.TeamRecord
import com.example.data.model.UserProfile
import com.example.data.model.currentDayKey
import com.example.data.model.currentWeekKey
import com.example.data.model.generateReferralCodeForUid
import com.example.data.model.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChorRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    val dataManager: DataManager = DataManager(db, auth)

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun currentUserEmail(): String? = auth.currentUser?.email

    // 1. User Profile Operations (delegating to DataManager where appropriate)
    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    dataManager.parseUserProfile(snapshot)
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun ensureUserProfile(defaultName: String = "চোর প্লেয়ার"): Result<UserProfile> {
        return dataManager.saveOrFetchUser(defaultName)
    }

    suspend fun getUserProfileById(targetUserId: String): Result<UserProfile?> {
        val path = "users/$targetUserId"
        return try {
            val snapshot = db.collection("users").document(targetUserId).get().await()
            if (snapshot.exists()) {
                Result.success(dataManager.parseUserProfile(snapshot))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfileDetails(
        username: String,
        avatarId: String,
        tutorialCompleted: Boolean
    ): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            val updates = mapOf(
                "username" to username.trim().take(40).ifBlank { "চোর প্লেয়ার" },
                "avatarId" to avatarId.trim().take(32).ifBlank { "avatar_hoodie" },
                "tutorialCompleted" to tutorialCompleted,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // 2. Round Completion, Anti-Cheat Validation, Score & GameSession Submission
    suspend fun submitRoundResult(
        profile: UserProfile,
        coinsEarned: Long,
        itemsCollected: Long,
        bestCombo: Long,
        durationSeconds: Long,
        mostCollectedItem: String,
        mangoesInRound: Long,
        coconutsInRound: Long,
        cowsInRound: Long,
        reachedSpeedThief: Boolean,
        gameMode: String = "CHOR",
        activeRoomCode: String = "",
        activeCompetitionId: String = "",
        activeChallengeId: String = ""
    ): Result<ScoreEntry> {
        val uid = requireUserId()
        val scoreId = "score_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val scorePath = "scores/$scoreId"

        val safeDuration = durationSeconds.coerceIn(10L, 600L)
        val coinsPerSecond = coinsEarned.toDouble() / safeDuration.toDouble()
        val itemsPerSecond = itemsCollected.toDouble() / safeDuration.toDouble()
        val isSuspicious = coinsEarned > 15000L ||
            itemsCollected > 220L ||
            coinsPerSecond > 85.0 ||
            itemsPerSecond > 2.2 ||
            bestCombo > 25L

        val boundedCoins = coinsEarned.coerceIn(0L, 50000L)
        val boundedItems = itemsCollected.coerceIn(0L, 500L)
        val boundedCombo = bestCombo.coerceIn(1L, 100L)

        val scoreEntry = ScoreEntry(
            scoreId = scoreId,
            userId = uid,
            username = profile.username.take(40).ifBlank { "চোর প্লেয়ার" },
            avatarId = profile.avatarId.take(32).ifBlank { "avatar_hoodie" },
            coinsEarned = boundedCoins,
            itemsCollected = boundedItems,
            bestCombo = boundedCombo,
            durationSeconds = safeDuration,
            mostCollectedItem = mostCollectedItem.take(64).ifBlank { "আম" },
            periodDay = currentDayKey(),
            periodWeek = currentWeekKey(),
            flaggedSuspicious = isSuspicious,
            status = if (isSuspicious) "flagged" else "valid"
        )

        return try {
            // 1. Write ScoreEntry to /scores
            db.collection("scores").document(scoreId).set(scoreEntry.toCreateMap()).await()

            // 2. Write GameSession to /game_sessions via DataManager
            dataManager.recordGameSession(
                username = profile.username,
                gameMode = gameMode,
                coinsEarned = boundedCoins,
                itemsCollected = boundedItems,
                bestCombo = boundedCombo,
                durationSeconds = safeDuration,
                roomCode = activeRoomCode
            )

            // 3. Update UserProfile & Achievements if not suspended
            if (!profile.suspended) {
                val newTotalCoins = (profile.totalCoins + boundedCoins).coerceAtMost(100000000L)
                val newTotalRounds = (profile.totalRounds + 1L).coerceAtMost(1000000L)
                val newBestScore = maxOf(profile.bestScore, boundedCoins).coerceAtMost(50000L)
                val newChandaBest = if (gameMode == "CHANDA") {
                    maxOf(profile.chandaBestScore, boundedCoins).coerceAtMost(50000L)
                } else {
                    profile.chandaBestScore
                }
                val newHighestCombo = maxOf(profile.highestCombo, boundedCombo).coerceAtMost(100L)
                val newMangoCount = (profile.mangoCount + mangoesInRound.coerceAtLeast(0L)).coerceAtMost(1000000L)
                val newCoconutCount = (profile.coconutCount + coconutsInRound.coerceAtLeast(0L)).coerceAtMost(1000000L)
                val newCowCount = (profile.cowCount + cowsInRound.coerceAtLeast(0L)).coerceAtMost(1000000L)

                val achievements = profile.unlockedAchievements.toMutableSet()
                if (newMangoCount >= 100L) achievements.add("mango_master")
                if (newCoconutCount >= 100L) achievements.add("coconut_king")
                if (newTotalCoins >= 10000L) achievements.add("coin_hunter")
                if (newHighestCombo >= 10L) achievements.add("combo_master")
                if (reachedSpeedThief) achievements.add("speed_thief")
                if (boundedCoins >= 5000L) achievements.add("chor_boss")
                if (gameMode == "CHANDA" && boundedCoins >= 500L) achievements.add("chanda_collector")
                if (activeCompetitionId.isNotBlank() || activeChallengeId.isNotBlank()) {
                    achievements.add("team_champion")
                }

                val profileUpdates = mapOf(
                    "totalCoins" to newTotalCoins,
                    "totalRounds" to newTotalRounds,
                    "bestScore" to newBestScore,
                    "chandaBestScore" to newChandaBest,
                    "highestCombo" to newHighestCombo,
                    "mangoCount" to newMangoCount,
                    "coconutCount" to newCoconutCount,
                    "cowCount" to newCowCount,
                    "unlockedAchievements" to achievements.toList().take(20),
                    "referralCode" to profile.referralCode.ifBlank { generateReferralCodeForUid(uid) },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("users").document(uid).update(profileUpdates).await()

                // 4. Update active Competition Room if player is competing in a private room
                if (activeCompetitionId.isNotBlank()) {
                    dataManager.submitScoreToCompetitionRoom(
                        competitionId = activeCompetitionId,
                        username = profile.username,
                        score = boundedCoins
                    )
                }

                // 5. Update active 1v1 Challenge Match if player accepted a challenge
                if (activeChallengeId.isNotBlank()) {
                    dataManager.completeChallengeMatch(
                        challengeId = activeChallengeId,
                        accepterName = profile.username,
                        accepterScore = boundedCoins
                    )
                }

                // 6. Add earned coins to player's Team totalScore if member of an active team
                if (profile.teamId.isNotBlank()) {
                    try {
                        val teamDoc = db.collection("teams").document(profile.teamId).get().await()
                        if (teamDoc.exists()) {
                            val currentTeamScore = teamDoc.getLong("totalScore") ?: 0L
                            val newTeamScore = (currentTeamScore + boundedCoins).coerceIn(0L, 100000000L)
                            db.collection("teams").document(profile.teamId).update(
                                mapOf(
                                    "totalScore" to newTeamScore,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            ).await()
                        }
                    } catch (_: Exception) {
                        // Ignore team score update failure if team was deleted or suspended
                    }
                }
            }

            Result.success(scoreEntry)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, scorePath)
            Result.failure(e)
        }
    }

    suspend fun claimRewardedAdBonus(profile: UserProfile, bonusCoins: Long): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            val safeBonus = bonusCoins.coerceIn(10L, 500L)
            val newTotal = (profile.totalCoins + safeBonus).coerceAtMost(100000000L)
            val achievements = profile.unlockedAchievements.toMutableSet()
            if (newTotal >= 10000L) achievements.add("coin_hunter")
            db.collection("users").document(uid).update(
                mapOf(
                    "totalCoins" to newTotal,
                    "unlockedAchievements" to achievements.toList().take(20),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // 3. Leaderboard & Personal Score Queries
    fun observeValidScores(): Flow<List<ScoreEntry>> {
        val path = "scores"
        return db.collection("scores")
            .whereEqualTo("status", "valid")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { parseScoreEntry(it) }
                    .sortedByDescending { it.coinsEarned }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeUserScores(userId: String): Flow<List<ScoreEntry>> {
        val path = "scores"
        return db.collection("scores")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { parseScoreEntry(it) }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    // 4. Multiplayer, Social, Referral & GameSession Streams via DataManager
    fun observeUserGameSessions(userId: String): Flow<List<GameSession>> =
        dataManager.observeUserGameSessions(userId)

    fun observeUserReferrals(userId: String): Flow<List<ReferralRecord>> =
        dataManager.observeUserReferrals(userId)

    fun observeFriends(userId: String): Flow<List<FriendConnection>> =
        dataManager.observeFriends(userId)

    fun observeActiveTeams(): Flow<List<TeamRecord>> =
        dataManager.observeActiveTeams()

    fun observeActiveCompetitions(): Flow<List<CompetitionRoom>> =
        dataManager.observeActiveCompetitions()

    fun observeChallenges(): Flow<List<ChallengeMatch>> =
        dataManager.observeChallenges()

    fun observeUserNotifications(userId: String): Flow<List<AppNotification>> =
        dataManager.observeUserNotifications(userId)

    // 5. Game Settings & Collectible Items
    fun observeGameSettings(): Flow<GameSettings> {
        val path = "game_settings/global"
        return db.collection("game_settings").document("global")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) parseGameSettings(snapshot) else GameSettings()
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    fun observeActiveItems(): Flow<List<CollectibleItemConfig>> {
        val path = "items"
        return db.collection("items")
            .whereEqualTo("active", true)
            .snapshots()
            .map { snapshot ->
                val list = snapshot.documents.map { parseItemConfig(it) }
                list.ifEmpty { DefaultGameCatalog.defaultItems }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    // 6. Admin Panel Operations
    fun observeAllUsersForAdmin(): Flow<List<UserProfile>> {
        val path = "users"
        return db.collection("users")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { dataManager.parseUserProfile(it) }
                    .sortedByDescending { it.totalCoins }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeAllScoresForAdmin(): Flow<List<ScoreEntry>> {
        val path = "scores"
        return db.collection("scores")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { parseScoreEntry(it) }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeAllReferralsForAdmin(): Flow<List<ReferralRecord>> =
        dataManager.observeAllReferralsForAdmin()

    suspend fun saveGameSettingsAsAdmin(settings: GameSettings): Result<Unit> {
        val path = "game_settings/global"
        return try {
            val docRef = db.collection("game_settings").document("global")
            val exists = docRef.get().await().exists()
            if (exists) {
                val updates = settings.toSaveMap().toMutableMap().apply { remove("configId") }
                docRef.update(updates).await()
            } else {
                docRef.set(settings.toSaveMap()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveCollectibleItemAsAdmin(item: CollectibleItemConfig): Result<Unit> {
        val cleanId = item.itemId.trim().lowercase().replace(Regex("[^a-z0-9_\\-]"), "_")
            .take(64).ifBlank { "item_${System.currentTimeMillis()}" }
        val path = "items/$cleanId"
        val normalized = item.copy(itemId = cleanId)
        return try {
            val docRef = db.collection("items").document(cleanId)
            val exists = docRef.get().await().exists()
            if (exists) {
                val updates = normalized.toSaveMap().toMutableMap().apply { remove("itemId") }
                docRef.update(updates).await()
            } else {
                docRef.set(normalized.toSaveMap()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun seedDefaultItemsAsAdmin(): Result<Unit> {
        return try {
            for (item in DefaultGameCatalog.defaultItems) {
                saveCollectibleItemAsAdmin(item)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "items")
            Result.failure(e)
        }
    }

    suspend fun setUserSuspendedAsAdmin(targetUserId: String, suspended: Boolean): Result<Unit> {
        val path = "users/$targetUserId"
        return try {
            db.collection("users").document(targetUserId).update(
                mapOf(
                    "suspended" to suspended,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteScoreAsAdmin(scoreId: String): Result<Unit> {
        val path = "scores/$scoreId"
        return try {
            db.collection("scores").document(scoreId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun updateScoreStatusAsAdmin(scoreId: String, status: String, flagged: Boolean): Result<Unit> {
        val path = "scores/$scoreId"
        return try {
            db.collection("scores").document(scoreId).update(
                mapOf(
                    "status" to status,
                    "flaggedSuspicious" to flagged
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun resetPeriodScoresAsAdmin(periodDay: String? = null, periodWeek: String? = null): Result<Int> {
        val path = "scores"
        return try {
            val query = when {
                periodDay != null -> db.collection("scores").whereEqualTo("periodDay", periodDay)
                periodWeek != null -> db.collection("scores").whereEqualTo("periodWeek", periodWeek)
                else -> return Result.success(0)
            }
            val snap = query.get().await()
            var deleted = 0
            for (doc in snap.documents) {
                doc.reference.delete().await()
                deleted++
            }
            Result.success(deleted)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // Type-safe snapshot parsers
    private fun parseScoreEntry(doc: DocumentSnapshot): ScoreEntry {
        return ScoreEntry(
            scoreId = doc.getString("scoreId") ?: doc.id,
            userId = doc.getString("userId") ?: "",
            username = doc.getString("username") ?: "চোর প্লেয়ার",
            avatarId = doc.getString("avatarId") ?: "avatar_hoodie",
            coinsEarned = doc.getLong("coinsEarned") ?: 0L,
            itemsCollected = doc.getLong("itemsCollected") ?: 0L,
            bestCombo = doc.getLong("bestCombo") ?: 1L,
            durationSeconds = doc.getLong("durationSeconds") ?: 150L,
            mostCollectedItem = doc.getString("mostCollectedItem") ?: "আম",
            periodDay = doc.getString("periodDay") ?: currentDayKey(),
            periodWeek = doc.getString("periodWeek") ?: currentWeekKey(),
            flaggedSuspicious = doc.getBoolean("flaggedSuspicious") ?: false,
            status = doc.getString("status") ?: "valid",
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    private fun parseGameSettings(doc: DocumentSnapshot): GameSettings {
        return GameSettings(
            configId = doc.getString("configId") ?: doc.id,
            roundDurationSeconds = doc.getLong("roundDurationSeconds") ?: 150L,
            maxComboMultiplier = doc.getLong("maxComboMultiplier") ?: 5L,
            npcDetectionRadius = doc.getLong("npcDetectionRadius") ?: 100L,
            specialItemFrequency = doc.getLong("specialItemFrequency") ?: 50L,
            itemSpawnRate = doc.getLong("itemSpawnRate") ?: 100L,
            difficulty = doc.getString("difficulty") ?: "NORMAL",
            playerSpeedMultiplier = doc.getLong("playerSpeedMultiplier") ?: 100L,
            adsEnabled = doc.getBoolean("adsEnabled") ?: true,
            topBannerEnabled = doc.getBoolean("topBannerEnabled") ?: true,
            bottomBannerEnabled = doc.getBoolean("bottomBannerEnabled") ?: true,
            resultAdEnabled = doc.getBoolean("resultAdEnabled") ?: true,
            rewardedAdEnabled = doc.getBoolean("rewardedAdEnabled") ?: true,
            rewardedCoinBonus = doc.getLong("rewardedCoinBonus") ?: 50L,
            adBannerCode = doc.getString("adBannerCode") ?: "CHOR 🪙 মেগা টুর্নামেন্ট চলছে!",
            adBannerSize = doc.getString("adBannerSize") ?: "320x50",
            productionDomain = doc.getString("productionDomain") ?: "https://chor-game.com",
            downloadAppUrl = doc.getString("downloadAppUrl") ?: "https://chor-game.com/download",
            referralRewardCoins = doc.getLong("referralRewardCoins") ?: 100L,
            maxTeamSize = doc.getLong("maxTeamSize") ?: 5L,
            chandaDurationSeconds = doc.getLong("chandaDurationSeconds") ?: 90L,
            chandaBaseReward = doc.getLong("chandaBaseReward") ?: 35L,
            updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    private fun parseItemConfig(doc: DocumentSnapshot): CollectibleItemConfig {
        return CollectibleItemConfig(
            itemId = doc.getString("itemId") ?: doc.id,
            nameBn = doc.getString("nameBn") ?: "আম",
            nameEn = doc.getString("nameEn") ?: "Mango",
            emoji = doc.getString("emoji") ?: "🥭",
            coinValue = doc.getLong("coinValue") ?: 10L,
            spawnProbability = doc.getLong("spawnProbability") ?: 50L,
            respawnTimeSeconds = doc.getLong("respawnTimeSeconds") ?: 8L,
            specialEffect = doc.getString("specialEffect") ?: "NONE",
            active = doc.getBoolean("active") ?: true,
            updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }
}
