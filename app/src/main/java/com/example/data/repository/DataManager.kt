package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.ChallengeMatch
import com.example.data.model.CompetitionRoom
import com.example.data.model.FriendConnection
import com.example.data.model.GameSession
import com.example.data.model.OperationType
import com.example.data.model.ReferralRecord
import com.example.data.model.TeamRecord
import com.example.data.model.UserProfile
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
import java.util.Locale
import java.util.UUID

/**
 * DataManager interfaces with Firebase Firestore for storing and retrieving
 * game-related collections including users, game_sessions, achievements,
 * referrals, friends, teams, competitions, challenges, and notifications.
 */
class DataManager(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // =========================================================================
    // 1. USERS & ACHIEVEMENTS COLLECTION (/users/{userId})
    // =========================================================================

    suspend fun saveOrFetchUser(defaultName: String = "চোর প্লেয়ার"): Result<UserProfile> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val parsed = parseUserProfile(snapshot)
                // Ensure referralCode is populated if missing on legacy profile
                if (parsed.referralCode.isBlank()) {
                    val generatedCode = generateReferralCodeForUid(uid)
                    docRef.update(
                        mapOf(
                            "referralCode" to generatedCode,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    Result.success(parsed.copy(referralCode = generatedCode))
                } else {
                    Result.success(parsed)
                }
            } else {
                val cleanName = defaultName.trim().take(40).ifBlank { "চোর প্লেয়ার" }
                val newProfile = UserProfile(
                    userId = uid,
                    username = cleanName,
                    avatarId = "avatar_hoodie",
                    referralCode = generateReferralCodeForUid(uid)
                )
                docRef.set(newProfile.toCreateMap()).await()
                Result.success(newProfile)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun unlockAchievement(profile: UserProfile, achievementId: String): Result<List<String>> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            val updatedList = (profile.unlockedAchievements + achievementId)
                .map { it.take(64) }
                .distinct()
                .take(20)
            db.collection("users").document(uid).update(
                mapOf(
                    "unlockedAchievements" to updatedList,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(updatedList)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 2. GAME SESSIONS COLLECTION (/game_sessions/{sessionId})
    // =========================================================================

    suspend fun recordGameSession(
        username: String,
        gameMode: String,
        coinsEarned: Long,
        itemsCollected: Long,
        bestCombo: Long,
        durationSeconds: Long,
        roomCode: String = ""
    ): Result<GameSession> {
        val uid = requireUserId()
        val sessionId = "sess_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val path = "game_sessions/$sessionId"
        val session = GameSession(
            sessionId = sessionId,
            userId = uid,
            username = username.take(40).ifBlank { "চোর প্লেয়ার" },
            gameMode = if (gameMode in listOf("CHOR", "CHANDA")) gameMode else "CHOR",
            coinsEarned = coinsEarned.coerceIn(0L, 50000L),
            itemsCollected = itemsCollected.coerceIn(0L, 500L),
            bestCombo = bestCombo.coerceIn(1L, 100L),
            durationSeconds = durationSeconds.coerceIn(10L, 600L),
            roomCode = roomCode.take(32)
        )
        return try {
            db.collection("game_sessions").document(sessionId).set(session.toCreateMap()).await()
            Result.success(session)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeUserGameSessions(userId: String): Flow<List<GameSession>> {
        val path = "game_sessions"
        return db.collection("game_sessions")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { parseGameSession(it) }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    // =========================================================================
    // 3. FRIEND INVITE & REFERRAL TRACKING (/referrals/{referralId})
    // =========================================================================

    suspend fun applyReferralCode(
        profile: UserProfile,
        rawReferralCode: String,
        rewardCoins: Long
    ): Result<ReferralRecord> {
        val uid = requireUserId()
        val cleanCode = rawReferralCode.trim().uppercase(Locale.US)
            .replace(Regex("[^A-Z0-9_\\-]"), "")
            .take(32)

        if (cleanCode.length < 3) {
            return Result.failure(IllegalArgumentException("সঠিক রেফারেল কোড লিখুন (যেমন: JR12345)"))
        }
        val myCode = profile.referralCode.ifBlank { generateReferralCodeForUid(uid) }
        if (cleanCode.equals(myCode, ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("নিজের রেফারেল কোড ব্যবহার করা যাবে না! (Self-referral prohibited)"))
        }
        if (profile.referredByCode.isNotBlank()) {
            return Result.failure(IllegalArgumentException("তুমি ইতিমধ্যে একটি রেফারেল বোনাস গ্রহণ করেছো!"))
        }

        val referralId = "ref_${uid.take(16)}_${cleanCode.take(10)}"
        val referralPath = "referrals/$referralId"
        val userPath = "users/$uid"
        val safeReward = rewardCoins.coerceIn(10L, 1000L)
        val referrerSyntheticUid = "referrer_$cleanCode"

        val record = ReferralRecord(
            referralId = referralId,
            referrerUserId = referrerSyntheticUid,
            referrerCode = cleanCode,
            referredUserId = uid,
            referredUsername = profile.username.take(40).ifBlank { "চোর প্লেয়ার" },
            rewardCoins = safeReward,
            status = "completed"
        )

        return try {
            db.collection("referrals").document(referralId).set(record.toCreateMap()).await()

            val newCoins = (profile.totalCoins + safeReward).coerceAtMost(100000000L)
            val achievements = profile.unlockedAchievements.toMutableSet()
            if (newCoins >= 10000L) achievements.add("coin_hunter")

            db.collection("users").document(uid).update(
                mapOf(
                    "referredByCode" to cleanCode,
                    "totalCoins" to newCoins,
                    "unlockedAchievements" to achievements.toList().take(20),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(record)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, referralPath)
            Result.failure(e)
        }
    }

    fun observeUserReferrals(userId: String): Flow<List<ReferralRecord>> {
        val path = "referrals"
        return db.collection("referrals")
            .whereEqualTo("referredUserId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { parseReferralRecord(it) }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeAllReferralsForAdmin(): Flow<List<ReferralRecord>> {
        val path = "referrals"
        return db.collection("referrals")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { parseReferralRecord(it) }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    // =========================================================================
    // 4. FRIENDS SYSTEM (/friends/{friendshipId})
    // =========================================================================

    suspend fun addFriendConnection(
        friendUserId: String,
        friendUsername: String,
        friendAvatarId: String = "avatar_hoodie",
        friendBestScore: Long = 0L,
        friendTeamName: String = ""
    ): Result<FriendConnection> {
        val uid = requireUserId()
        val cleanFriendId = friendUserId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
        if (cleanFriendId.isBlank() || cleanFriendId == uid) {
            return Result.failure(IllegalArgumentException("নিজেকে বন্ধু তালিকায় যোগ করা যাবে না!"))
        }
        val friendshipId = "fr_${uid.take(12)}_${cleanFriendId.take(12)}"
        val path = "friends/$friendshipId"
        val connection = FriendConnection(
            friendshipId = friendshipId,
            userId = uid,
            friendUserId = cleanFriendId,
            friendUsername = friendUsername.trim().take(40).ifBlank { "বন্ধু" },
            friendAvatarId = friendAvatarId.take(32).ifBlank { "avatar_hoodie" },
            friendBestScore = friendBestScore.coerceIn(0L, 50000L),
            friendTeamName = friendTeamName.take(40),
            onlineStatus = "ONLINE"
        )
        return try {
            db.collection("friends").document(friendshipId).set(connection.toCreateMap()).await()
            Result.success(connection)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeFriends(userId: String): Flow<List<FriendConnection>> {
        val path = "friends"
        return db.collection("friends")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { parseFriendConnection(it) }
                    .sortedByDescending { it.friendBestScore }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun removeFriend(friendshipId: String): Result<Unit> {
        val path = "friends/$friendshipId"
        return try {
            db.collection("friends").document(friendshipId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 5. TEAM SYSTEM & TEAM COMPETITION (/teams/{teamId})
    // =========================================================================

    suspend fun createTeam(
        profile: UserProfile,
        teamName: String,
        teamAvatar: String
    ): Result<TeamRecord> {
        val uid = requireUserId()
        val code = "TM" + UUID.randomUUID().toString().uppercase(Locale.US).replace("-", "").take(4)
        val teamId = "team_$code"
        val path = "teams/$teamId"
        val cleanName = teamName.trim().take(40).let { if (it.length < 2) "চোর বাহিনী" else it }

        val team = TeamRecord(
            teamId = teamId,
            teamCode = code,
            teamName = cleanName,
            teamAvatar = teamAvatar.take(16).ifBlank { "🥷" },
            managerId = uid,
            managerName = profile.username.take(40).ifBlank { "ম্যানেজার" },
            memberIds = listOf(uid),
            memberNames = listOf(profile.username.take(40).ifBlank { "চোর প্লেয়ার" }),
            totalScore = profile.bestScore.coerceIn(0L, 100000000L),
            suspended = false,
            status = "active"
        )

        return try {
            db.collection("teams").document(teamId).set(team.toCreateMap()).await()
            val achievements = (profile.unlockedAchievements + "team_champion").distinct().take(20)
            db.collection("users").document(uid).update(
                mapOf(
                    "teamId" to teamId,
                    "teamName" to cleanName,
                    "unlockedAchievements" to achievements,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(team)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinTeamByCode(
        profile: UserProfile,
        rawTeamCode: String,
        maxTeamSize: Int = 5
    ): Result<TeamRecord> {
        val uid = requireUserId()
        val cleanCode = rawTeamCode.trim().uppercase(Locale.US).replace(Regex("[^A-Z0-9_\\-]"), "")
        val teamId = if (cleanCode.startsWith("TEAM_")) cleanCode.lowercase(Locale.US) else "team_$cleanCode"
        val path = "teams/$teamId"

        return try {
            val docRef = db.collection("teams").document(teamId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalArgumentException("এই টিম কোডে কোনো সক্রিয় টিম পাওয়া যায়নি!"))
            }
            val team = parseTeamRecord(snap)
            val limit = maxTeamSize.coerceIn(2, 10)
            if (!team.memberIds.contains(uid) && team.memberIds.size >= limit) {
                return Result.failure(IllegalArgumentException("টিমটি পূর্ণ হয়ে গেছে (সর্বোচ্চ $limit জন)!"))
            }

            val updatedIds = (team.memberIds + uid).distinct().take(10)
            val updatedNames = (team.memberNames + profile.username.take(40)).distinct().take(10)
            val updatedScore = (team.totalScore + profile.bestScore).coerceIn(0L, 100000000L)

            docRef.update(
                mapOf(
                    "memberIds" to updatedIds,
                    "memberNames" to updatedNames,
                    "totalScore" to updatedScore,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val achievements = (profile.unlockedAchievements + "team_champion").distinct().take(20)
            db.collection("users").document(uid).update(
                mapOf(
                    "teamId" to team.teamId,
                    "teamName" to team.teamName,
                    "unlockedAchievements" to achievements,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(
                team.copy(
                    memberIds = updatedIds,
                    memberNames = updatedNames,
                    totalScore = updatedScore
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeTeamMemberAsManager(
        team: TeamRecord,
        memberIndex: Int
    ): Result<Unit> {
        val uid = requireUserId()
        if (team.managerId != uid) {
            return Result.failure(IllegalStateException("শুধুমাত্র টিম ম্যানেজার মেম্বার রিমুভ করতে পারবেন।"))
        }
        if (memberIndex <= 0 || memberIndex >= team.memberIds.size) {
            return Result.failure(IllegalArgumentException("ম্যানেজারকে রিমুভ করা যাবে না।"))
        }
        val path = "teams/${team.teamId}"
        return try {
            val newIds = team.memberIds.toMutableList().apply { removeAt(memberIndex) }
            val newNames = team.memberNames.toMutableList().apply {
                if (memberIndex < size) removeAt(memberIndex)
            }
            db.collection("teams").document(team.teamId).update(
                mapOf(
                    "memberIds" to newIds,
                    "memberNames" to newNames,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeActiveTeams(): Flow<List<TeamRecord>> {
        val path = "teams"
        return db.collection("teams")
            .whereEqualTo("status", "active")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { parseTeamRecord(it) }
                    .sortedByDescending { it.totalScore }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun setTeamSuspendedAsAdmin(teamId: String, suspended: Boolean): Result<Unit> {
        val path = "teams/$teamId"
        return try {
            db.collection("teams").document(teamId).update(
                mapOf(
                    "suspended" to suspended,
                    "status" to if (suspended) "suspended" else "active",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 6. PRIVATE ROOMS & FRIEND COMPETITIONS (/competitions/{competitionId})
    // =========================================================================

    suspend fun createCompetitionRoom(
        profile: UserProfile,
        title: String,
        gameMode: String = "CHOR",
        durationSeconds: Long = 150L
    ): Result<CompetitionRoom> {
        val uid = requireUserId()
        val roomCode = "RM" + UUID.randomUUID().toString().uppercase(Locale.US).replace("-", "").take(4)
        val competitionId = "comp_$roomCode"
        val path = "competitions/$competitionId"
        val cleanTitle = title.trim().take(60).let { if (it.length < 2) "${profile.username} vs Friends" else it }

        val room = CompetitionRoom(
            competitionId = competitionId,
            roomCode = roomCode,
            title = cleanTitle,
            managerId = uid,
            managerName = profile.username.take(40).ifBlank { "চোর প্লেয়ার" },
            gameMode = if (gameMode in listOf("CHOR", "CHANDA", "TEAM_CHOR")) gameMode else "CHOR",
            status = "waiting",
            durationSeconds = durationSeconds.coerceIn(30L, 600L),
            participantIds = listOf(uid),
            leaderboardSummary = listOf("${profile.username.take(25)}: 0 🪙 (Ready)"),
            winnerName = "",
            winnerScore = 0L
        )

        return try {
            db.collection("competitions").document(competitionId).set(room.toCreateMap()).await()
            Result.success(room)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinCompetitionRoomByCode(
        profile: UserProfile,
        rawRoomCode: String
    ): Result<CompetitionRoom> {
        val uid = requireUserId()
        val cleanCode = rawRoomCode.trim().uppercase(Locale.US).replace(Regex("[^A-Z0-9_\\-]"), "")
        val competitionId = if (cleanCode.startsWith("COMP_")) cleanCode.lowercase(Locale.US) else "comp_$cleanCode"
        val path = "competitions/$competitionId"

        return try {
            val docRef = db.collection("competitions").document(competitionId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalArgumentException("এই রুম কোডে কোনো প্রাইভেট রুম পাওয়া যায়নি!"))
            }
            val room = parseCompetitionRoom(snap)
            if (room.status == "closed") {
                return Result.failure(IllegalStateException("এই রুমটি বন্ধ হয়ে গেছে।"))
            }
            val newParticipants = (room.participantIds + uid).distinct().take(10)
            val entryLabel = "${profile.username.take(25)}: 0 🪙 (Ready)"
            val existingWithoutMe = room.leaderboardSummary.filterNot {
                it.startsWith("${profile.username.take(25)}:")
            }
            val newSummary = (existingWithoutMe + entryLabel).take(10)

            docRef.update(
                mapOf(
                    "participantIds" to newParticipants,
                    "leaderboardSummary" to newSummary,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(room.copy(participantIds = newParticipants, leaderboardSummary = newSummary))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCompetitionStatus(
        competitionId: String,
        newStatus: String
    ): Result<Unit> {
        val path = "competitions/$competitionId"
        return try {
            val validStatus = if (newStatus in listOf("waiting", "active", "completed", "closed")) newStatus else "active"
            db.collection("competitions").document(competitionId).update(
                mapOf(
                    "status" to validStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitScoreToCompetitionRoom(
        competitionId: String,
        username: String,
        score: Long
    ): Result<CompetitionRoom?> {
        val path = "competitions/$competitionId"
        return try {
            val docRef = db.collection("competitions").document(competitionId)
            val snap = docRef.get().await()
            if (!snap.exists()) return Result.success(null)
            val room = parseCompetitionRoom(snap)
            val shortName = username.take(25).ifBlank { "চোর প্লেয়ার" }
            val updatedEntries = room.leaderboardSummary
                .filterNot { it.startsWith("$shortName:") }
                .toMutableList()
            updatedEntries.add("$shortName: $score 🪙")

            // Determine current highest scorer
            val bestScore = maxOf(room.winnerScore, score).coerceIn(0L, 500000L)
            val bestName = if (score >= room.winnerScore) shortName else room.winnerName.ifBlank { shortName }

            docRef.update(
                mapOf(
                    "leaderboardSummary" to updatedEntries.take(10),
                    "winnerName" to bestName.take(40),
                    "winnerScore" to bestScore,
                    "status" to "completed",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(
                room.copy(
                    leaderboardSummary = updatedEntries.take(10),
                    winnerName = bestName,
                    winnerScore = bestScore,
                    status = "completed"
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeActiveCompetitions(): Flow<List<CompetitionRoom>> {
        val path = "competitions"
        return db.collection("competitions")
            .whereIn("status", listOf("waiting", "active", "completed"))
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { parseCompetitionRoom(it) }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun deleteCompetitionAsAdmin(competitionId: String): Result<Unit> {
        val path = "competitions/$competitionId"
        return try {
            db.collection("competitions").document(competitionId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 7. 1v1 SHAREABLE SCORE CHALLENGES (/challenges/{challengeId})
    // =========================================================================

    suspend fun createScoreChallenge(
        profile: UserProfile,
        creatorScore: Long,
        gameMode: String = "CHOR"
    ): Result<ChallengeMatch> {
        val uid = requireUserId()
        val code = "CH" + UUID.randomUUID().toString().uppercase(Locale.US).replace("-", "").take(4)
        val challengeId = "chal_$code"
        val path = "challenges/$challengeId"

        val match = ChallengeMatch(
            challengeId = challengeId,
            challengeCode = code,
            creatorId = uid,
            creatorName = profile.username.take(40).ifBlank { "চোর প্লেয়ার" },
            creatorScore = creatorScore.coerceIn(0L, 50000L),
            gameMode = if (gameMode in listOf("CHOR", "CHANDA")) gameMode else "CHOR",
            accepterId = "",
            accepterName = "",
            accepterScore = 0L,
            winnerName = "",
            status = "open"
        )
        return try {
            db.collection("challenges").document(challengeId).set(match.toCreateMap()).await()
            Result.success(match)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun completeChallengeMatch(
        challengeId: String,
        accepterName: String,
        accepterScore: Long
    ): Result<ChallengeMatch?> {
        val uid = requireUserId()
        val path = "challenges/$challengeId"
        return try {
            val docRef = db.collection("challenges").document(challengeId)
            val snap = docRef.get().await()
            if (!snap.exists()) return Result.success(null)
            val match = parseChallengeMatch(snap)
            val cleanAccepter = accepterName.take(40).ifBlank { "চ্যালেঞ্জার" }
            val boundedScore = accepterScore.coerceIn(0L, 50000L)
            val winner = if (boundedScore > match.creatorScore) cleanAccepter else match.creatorName

            docRef.update(
                mapOf(
                    "accepterId" to uid,
                    "accepterName" to cleanAccepter,
                    "accepterScore" to boundedScore,
                    "winnerName" to winner.take(40),
                    "status" to "completed",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(
                match.copy(
                    accepterId = uid,
                    accepterName = cleanAccepter,
                    accepterScore = boundedScore,
                    winnerName = winner,
                    status = "completed"
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeChallenges(): Flow<List<ChallengeMatch>> {
        val path = "challenges"
        return db.collection("challenges")
            .whereIn("status", listOf("open", "completed"))
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { parseChallengeMatch(it) }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    // =========================================================================
    // 8. IN-APP NOTIFICATIONS (/notifications/{notificationId})
    // =========================================================================

    suspend fun createNotification(
        recipientUserId: String,
        senderName: String,
        type: String,
        title: String,
        message: String,
        actionCode: String = ""
    ): Result<AppNotification> {
        requireUserId()
        val notificationId = "notif_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val path = "notifications/$notificationId"
        val notif = AppNotification(
            notificationId = notificationId,
            recipientUserId = recipientUserId.take(128),
            senderName = senderName.take(40).ifBlank { "CHOR গেম" },
            type = type.take(32).ifBlank { "INVITE" },
            title = title.take(80).ifBlank { "নতুন আমন্ত্রণ" },
            message = message.take(200).ifBlank { "একটি নতুন আপডেট এসেছে!" },
            actionCode = actionCode.take(64),
            read = false
        )
        return try {
            db.collection("notifications").document(notificationId).set(notif.toCreateMap()).await()
            Result.success(notif)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeUserNotifications(userId: String): Flow<List<AppNotification>> {
        val path = "notifications"
        return db.collection("notifications")
            .whereEqualTo("recipientUserId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { parseNotification(it) }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        val path = "notifications/$notificationId"
        return try {
            db.collection("notifications").document(notificationId).update("read", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // =========================================================================
    // PARSERS
    // =========================================================================

    internal fun parseUserProfile(doc: DocumentSnapshot): UserProfile {
        val achievementsRaw = doc.get("unlockedAchievements") as? List<*>
        val achievements = achievementsRaw?.mapNotNull { it as? String } ?: emptyList()
        val uid = doc.getString("userId") ?: doc.id
        return UserProfile(
            userId = uid,
            username = doc.getString("username") ?: "চোর প্লেয়ার",
            avatarId = doc.getString("avatarId") ?: "avatar_hoodie",
            totalCoins = doc.getLong("totalCoins") ?: 0L,
            totalRounds = doc.getLong("totalRounds") ?: 0L,
            bestScore = doc.getLong("bestScore") ?: 0L,
            highestCombo = doc.getLong("highestCombo") ?: 1L,
            mangoCount = doc.getLong("mangoCount") ?: 0L,
            coconutCount = doc.getLong("coconutCount") ?: 0L,
            cowCount = doc.getLong("cowCount") ?: 0L,
            unlockedAchievements = achievements,
            suspended = doc.getBoolean("suspended") ?: false,
            tutorialCompleted = doc.getBoolean("tutorialCompleted") ?: false,
            referralCode = doc.getString("referralCode")?.ifBlank { generateReferralCodeForUid(uid) }
                ?: generateReferralCodeForUid(uid),
            referredByCode = doc.getString("referredByCode") ?: "",
            referralCount = doc.getLong("referralCount") ?: 0L,
            teamId = doc.getString("teamId") ?: "",
            teamName = doc.getString("teamName") ?: "",
            chandaBestScore = doc.getLong("chandaBestScore") ?: 0L,
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    internal fun parseGameSession(doc: DocumentSnapshot): GameSession {
        return GameSession(
            sessionId = doc.getString("sessionId") ?: doc.id,
            userId = doc.getString("userId") ?: "",
            username = doc.getString("username") ?: "চোর প্লেয়ার",
            gameMode = doc.getString("gameMode") ?: "CHOR",
            coinsEarned = doc.getLong("coinsEarned") ?: 0L,
            itemsCollected = doc.getLong("itemsCollected") ?: 0L,
            bestCombo = doc.getLong("bestCombo") ?: 1L,
            durationSeconds = doc.getLong("durationSeconds") ?: 150L,
            roomCode = doc.getString("roomCode") ?: "",
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    internal fun parseReferralRecord(doc: DocumentSnapshot): ReferralRecord {
        return ReferralRecord(
            referralId = doc.getString("referralId") ?: doc.id,
            referrerUserId = doc.getString("referrerUserId") ?: "",
            referrerCode = doc.getString("referrerCode") ?: "",
            referredUserId = doc.getString("referredUserId") ?: "",
            referredUsername = doc.getString("referredUsername") ?: "চোর প্লেয়ার",
            rewardCoins = doc.getLong("rewardCoins") ?: 100L,
            status = doc.getString("status") ?: "completed",
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    internal fun parseFriendConnection(doc: DocumentSnapshot): FriendConnection {
        return FriendConnection(
            friendshipId = doc.getString("friendshipId") ?: doc.id,
            userId = doc.getString("userId") ?: "",
            friendUserId = doc.getString("friendUserId") ?: "",
            friendUsername = doc.getString("friendUsername") ?: "বন্ধু",
            friendAvatarId = doc.getString("friendAvatarId") ?: "avatar_hoodie",
            friendBestScore = doc.getLong("friendBestScore") ?: 0L,
            friendTeamName = doc.getString("friendTeamName") ?: "",
            onlineStatus = doc.getString("onlineStatus") ?: "ONLINE",
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    internal fun parseTeamRecord(doc: DocumentSnapshot): TeamRecord {
        val memberIdsRaw = doc.get("memberIds") as? List<*>
        val memberNamesRaw = doc.get("memberNames") as? List<*>
        return TeamRecord(
            teamId = doc.getString("teamId") ?: doc.id,
            teamCode = doc.getString("teamCode") ?: "",
            teamName = doc.getString("teamName") ?: "চোর বাহিনী",
            teamAvatar = doc.getString("teamAvatar") ?: "🥷",
            managerId = doc.getString("managerId") ?: "",
            managerName = doc.getString("managerName") ?: "ম্যানেজার",
            memberIds = memberIdsRaw?.mapNotNull { it as? String } ?: emptyList(),
            memberNames = memberNamesRaw?.mapNotNull { it as? String } ?: emptyList(),
            totalScore = doc.getLong("totalScore") ?: 0L,
            suspended = doc.getBoolean("suspended") ?: false,
            status = doc.getString("status") ?: "active",
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    internal fun parseCompetitionRoom(doc: DocumentSnapshot): CompetitionRoom {
        val participantsRaw = doc.get("participantIds") as? List<*>
        val summaryRaw = doc.get("leaderboardSummary") as? List<*>
        return CompetitionRoom(
            competitionId = doc.getString("competitionId") ?: doc.id,
            roomCode = doc.getString("roomCode") ?: "",
            title = doc.getString("title") ?: "Friend Match",
            managerId = doc.getString("managerId") ?: "",
            managerName = doc.getString("managerName") ?: "চোর প্লেয়ার",
            gameMode = doc.getString("gameMode") ?: "CHOR",
            status = doc.getString("status") ?: "waiting",
            durationSeconds = doc.getLong("durationSeconds") ?: 150L,
            participantIds = participantsRaw?.mapNotNull { it as? String } ?: emptyList(),
            leaderboardSummary = summaryRaw?.mapNotNull { it as? String } ?: emptyList(),
            winnerName = doc.getString("winnerName") ?: "",
            winnerScore = doc.getLong("winnerScore") ?: 0L,
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    internal fun parseChallengeMatch(doc: DocumentSnapshot): ChallengeMatch {
        return ChallengeMatch(
            challengeId = doc.getString("challengeId") ?: doc.id,
            challengeCode = doc.getString("challengeCode") ?: "",
            creatorId = doc.getString("creatorId") ?: "",
            creatorName = doc.getString("creatorName") ?: "চোর প্লেয়ার",
            creatorScore = doc.getLong("creatorScore") ?: 0L,
            gameMode = doc.getString("gameMode") ?: "CHOR",
            accepterId = doc.getString("accepterId") ?: "",
            accepterName = doc.getString("accepterName") ?: "",
            accepterScore = doc.getLong("accepterScore") ?: 0L,
            winnerName = doc.getString("winnerName") ?: "",
            status = doc.getString("status") ?: "open",
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    internal fun parseNotification(doc: DocumentSnapshot): AppNotification {
        return AppNotification(
            notificationId = doc.getString("notificationId") ?: doc.id,
            recipientUserId = doc.getString("recipientUserId") ?: "",
            senderName = doc.getString("senderName") ?: "CHOR গেম",
            type = doc.getString("type") ?: "INVITE",
            title = doc.getString("title") ?: "",
            message = doc.getString("message") ?: "",
            actionCode = doc.getString("actionCode") ?: "",
            read = doc.getBoolean("read") ?: false,
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }
}
