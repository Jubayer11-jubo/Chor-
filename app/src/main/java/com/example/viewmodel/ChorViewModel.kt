package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppNotification
import com.example.data.model.ChallengeMatch
import com.example.data.model.CollectibleItemConfig
import com.example.data.model.CompetitionRoom
import com.example.data.model.DefaultGameCatalog
import com.example.data.model.FriendConnection
import com.example.data.model.GameSession
import com.example.data.model.GameSettings
import com.example.data.model.ReferralRecord
import com.example.data.model.ScoreEntry
import com.example.data.model.TeamRecord
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.currentDayKey
import com.example.data.model.currentWeekKey
import com.example.data.model.generateReferralCodeForUid
import com.example.data.repository.ChorRepository
import com.example.sound.GameSoundManager
import com.google.firebase.Timestamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class MainNavTab {
    HOME,
    PLAY,
    MULTIPLAYER,
    RANKING,
    PROFILE
}

data class RoundSummary(
    val coinsEarned: Long,
    val itemsCollected: Int,
    val bestCombo: Int,
    val durationSeconds: Int,
    val mostCollectedItem: String,
    val flaggedSuspicious: Boolean,
    val gameMode: String = "CHOR",
    val competitionTitle: String = "",
    val rewardedBonusClaimed: Boolean = false
)

class ChorViewModel(
    private val repository: ChorRepository,
    val currentUserId: String,
    private val initialDisplayName: String,
    val isAuthenticated: Boolean = true
) : ViewModel() {

    val soundManager = GameSoundManager()

    private val _selectedTab = MutableStateFlow(MainNavTab.HOME)
    val selectedTab: StateFlow<MainNavTab> = _selectedTab.asStateFlow()

    private val _multiplayerInitialSection = MutableStateFlow(0)
    val multiplayerInitialSection: StateFlow<Int> = _multiplayerInitialSection.asStateFlow()

    private val _currentGameMode = MutableStateFlow("CHOR")
    val currentGameMode: StateFlow<String> = _currentGameMode.asStateFlow()

    private val _activeCompetitionRoom = MutableStateFlow<CompetitionRoom?>(null)
    val activeCompetitionRoom: StateFlow<CompetitionRoom?> = _activeCompetitionRoom.asStateFlow()

    private val _activeChallengeMatch = MutableStateFlow<ChallengeMatch?>(null)
    val activeChallengeMatch: StateFlow<ChallengeMatch?> = _activeChallengeMatch.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _musicEnabled = MutableStateFlow(true)
    val musicEnabled: StateFlow<Boolean> = _musicEnabled.asStateFlow()

    private val _showTutorial = MutableStateFlow(false)
    val showTutorial: StateFlow<Boolean> = _showTutorial.asStateFlow()

    private val _lastRoundSummary = MutableStateFlow<RoundSummary?>(null)
    val lastRoundSummary: StateFlow<RoundSummary?> = _lastRoundSummary.asStateFlow()

    private val _showAdminPanel = MutableStateFlow(false)
    val showAdminPanel: StateFlow<Boolean> = _showAdminPanel.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _adminUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val adminUsers: StateFlow<List<UserProfile>> = _adminUsers.asStateFlow()

    private val _adminAllScores = MutableStateFlow<List<ScoreEntry>>(emptyList())
    val adminAllScores: StateFlow<List<ScoreEntry>> = _adminAllScores.asStateFlow()

    private val _adminReferrals = MutableStateFlow<List<ReferralRecord>>(emptyList())
    val adminReferrals: StateFlow<List<ReferralRecord>> = _adminReferrals.asStateFlow()

    private var adminUsersJob: Job? = null
    private var adminScoresJob: Job? = null
    private var adminReferralsJob: Job? = null

    // Local session state used ONLY when not yet signed in with Google (strictly avoids unauthenticated Firestore calls)
    private val _localProfile = MutableStateFlow(
        UserProfile(
            userId = currentUserId,
            username = initialDisplayName,
            referralCode = generateReferralCodeForUid(currentUserId),
            tutorialCompleted = true
        )
    )
    private val _localScores = MutableStateFlow<List<ScoreEntry>>(emptyList())
    private val _localSessions = MutableStateFlow<List<GameSession>>(emptyList())

    // Two-tier Flow error handling for Firestore streams, strictly gated on isAuthenticated
    val userProfileState: StateFlow<UiState<UserProfile?>> = (
        if (isAuthenticated) {
            repository.observeUserProfile(currentUserId)
                .map<UserProfile?, UiState<UserProfile?>> { profile ->
                    if (profile != null && !profile.tutorialCompleted && !_tutorialDismissedInSession) {
                        _showTutorial.value = true
                    }
                    UiState.Success(profile)
                }
                .catch { error ->
                    Log.w(TAG, "Error observing user profile", error)
                    emit(UiState.Error(error.message ?: "প্রোফাইল লোড করতে সমস্যা হয়েছে"))
                }
        } else {
            _localProfile.map<UserProfile, UiState<UserProfile?>> { UiState.Success(it) }
        }
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = if (isAuthenticated) UiState.Loading else UiState.Success(_localProfile.value)
        )

    val validScoresState: StateFlow<UiState<List<ScoreEntry>>> = (
        if (isAuthenticated) {
            repository.observeValidScores()
                .map<List<ScoreEntry>, UiState<List<ScoreEntry>>> { UiState.Success(it) }
                .catch { error ->
                    Log.w(TAG, "Error observing valid scores", error)
                    emit(UiState.Error(error.message ?: "লিডারবোর্ড লোড করতে সমস্যা হয়েছে"))
                }
        } else {
            _localScores.map { UiState.Success(it) }
        }
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = if (isAuthenticated) UiState.Loading else UiState.Success(emptyList())
        )

    val userScoresState: StateFlow<UiState<List<ScoreEntry>>> = (
        if (isAuthenticated) {
            repository.observeUserScores(currentUserId)
                .map<List<ScoreEntry>, UiState<List<ScoreEntry>>> { UiState.Success(it) }
                .catch { error ->
                    Log.w(TAG, "Error observing user scores", error)
                    emit(UiState.Error(error.message ?: "স্কোর হিস্ট্রি লোড করতে সমস্যা হয়েছে"))
                }
        } else {
            _localScores.map { UiState.Success(it) }
        }
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = if (isAuthenticated) UiState.Loading else UiState.Success(emptyList())
        )

    val userSessionsState: StateFlow<UiState<List<GameSession>>> = (
        if (isAuthenticated) {
            repository.observeUserGameSessions(currentUserId)
                .map<List<GameSession>, UiState<List<GameSession>>> { UiState.Success(it) }
                .catch { error ->
                    Log.w(TAG, "Error observing user sessions", error)
                    emit(UiState.Success(emptyList()))
                }
        } else {
            _localSessions.map { UiState.Success(it) }
        }
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    val userReferralsState: StateFlow<UiState<List<ReferralRecord>>> = (
        if (isAuthenticated) {
            repository.observeUserReferrals(currentUserId)
                .map<List<ReferralRecord>, UiState<List<ReferralRecord>>> { UiState.Success(it) }
                .catch { error ->
                    Log.w(TAG, "Error observing user referrals", error)
                    emit(UiState.Success(emptyList()))
                }
        } else {
            MutableStateFlow<UiState<List<ReferralRecord>>>(UiState.Success(emptyList()))
        }
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    val friendsState: StateFlow<UiState<List<FriendConnection>>> = (
        if (isAuthenticated) {
            repository.observeFriends(currentUserId)
                .map<List<FriendConnection>, UiState<List<FriendConnection>>> { UiState.Success(it) }
                .catch { error ->
                    Log.w(TAG, "Error observing friends", error)
                    emit(UiState.Success(emptyList()))
                }
        } else {
            MutableStateFlow<UiState<List<FriendConnection>>>(UiState.Success(emptyList()))
        }
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    val activeTeamsState: StateFlow<UiState<List<TeamRecord>>> = (
        if (isAuthenticated) {
            repository.observeActiveTeams()
                .map<List<TeamRecord>, UiState<List<TeamRecord>>> { UiState.Success(it) }
                .catch { error ->
                    Log.w(TAG, "Error observing teams", error)
                    emit(UiState.Success(emptyList()))
                }
        } else {
            MutableStateFlow<UiState<List<TeamRecord>>>(UiState.Success(emptyList()))
        }
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    val competitionsState: StateFlow<UiState<List<CompetitionRoom>>> = (
        if (isAuthenticated) {
            repository.observeActiveCompetitions()
                .map<List<CompetitionRoom>, UiState<List<CompetitionRoom>>> { rooms ->
                    val currentRoomId = _activeCompetitionRoom.value?.competitionId
                    if (currentRoomId != null) {
                        rooms.find { it.competitionId == currentRoomId }?.let {
                            _activeCompetitionRoom.value = it
                        }
                    }
                    UiState.Success(rooms)
                }
                .catch { error ->
                    Log.w(TAG, "Error observing competitions", error)
                    emit(UiState.Success(emptyList()))
                }
        } else {
            MutableStateFlow<UiState<List<CompetitionRoom>>>(UiState.Success(emptyList()))
        }
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    val challengesState: StateFlow<UiState<List<ChallengeMatch>>> = (
        if (isAuthenticated) {
            repository.observeChallenges()
                .map<List<ChallengeMatch>, UiState<List<ChallengeMatch>>> { UiState.Success(it) }
                .catch { error ->
                    Log.w(TAG, "Error observing challenges", error)
                    emit(UiState.Success(emptyList()))
                }
        } else {
            MutableStateFlow<UiState<List<ChallengeMatch>>>(UiState.Success(emptyList()))
        }
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    val notificationsState: StateFlow<UiState<List<AppNotification>>> = (
        if (isAuthenticated) {
            repository.observeUserNotifications(currentUserId)
                .map<List<AppNotification>, UiState<List<AppNotification>>> { UiState.Success(it) }
                .catch { error ->
                    Log.w(TAG, "Error observing notifications", error)
                    emit(UiState.Success(emptyList()))
                }
        } else {
            MutableStateFlow<UiState<List<AppNotification>>>(UiState.Success(emptyList()))
        }
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    val gameSettingsState: StateFlow<UiState<GameSettings>> = (
        if (isAuthenticated) {
            repository.observeGameSettings()
                .map<GameSettings, UiState<GameSettings>> { UiState.Success(it) }
                .catch { error ->
                    Log.w(TAG, "Error observing game settings", error)
                    emit(UiState.Success(GameSettings()))
                }
        } else {
            MutableStateFlow<UiState<GameSettings>>(UiState.Success(GameSettings()))
        }
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(GameSettings())
        )

    val activeItemsState: StateFlow<UiState<List<CollectibleItemConfig>>> = (
        if (isAuthenticated) {
            repository.observeActiveItems()
                .map<List<CollectibleItemConfig>, UiState<List<CollectibleItemConfig>>> { UiState.Success(it) }
                .catch { error ->
                    Log.w(TAG, "Error observing active items", error)
                    emit(UiState.Success(DefaultGameCatalog.defaultItems))
                }
        } else {
            MutableStateFlow<UiState<List<CollectibleItemConfig>>>(UiState.Success(DefaultGameCatalog.defaultItems))
        }
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(DefaultGameCatalog.defaultItems)
        )

    private var _tutorialDismissedInSession = false

    init {
        if (isAuthenticated) {
            viewModelScope.launch {
                repository.ensureUserProfile(initialDisplayName)
            }
        }
    }

    fun isCurrentUserAdmin(): Boolean {
        if (!isAuthenticated) return false
        val email = repository.currentUserEmail() ?: return false
        return email.equals("MDJUBAYERHOSEN302@gmail.com", ignoreCase = true)
    }

    fun selectTab(tab: MainNavTab) {
        soundManager.playButtonTap()
        _showAdminPanel.value = false
        if (tab != MainNavTab.PLAY) {
            soundManager.stopBackgroundMusic()
            _lastRoundSummary.value = null
        }
        _selectedTab.value = tab
    }

    fun openMultiplayerSection(sectionIndex: Int) {
        soundManager.playButtonTap()
        _showAdminPanel.value = false
        _multiplayerInitialSection.value = sectionIndex
        _selectedTab.value = MainNavTab.MULTIPLAYER
    }

    fun startNewRound(mode: String = "CHOR") {
        soundManager.playButtonTap()
        _showAdminPanel.value = false
        _currentGameMode.value = if (mode in listOf("CHOR", "CHANDA")) mode else "CHOR"
        _lastRoundSummary.value = null
        _selectedTab.value = MainNavTab.PLAY
        if (_musicEnabled.value) {
            soundManager.startBackgroundMusic()
        }
    }

    fun startCompetitionRoomMatch(room: CompetitionRoom) {
        soundManager.playButtonTap()
        _activeCompetitionRoom.value = room
        _activeChallengeMatch.value = null
        if (isAuthenticated) {
            viewModelScope.launch {
                repository.dataManager.updateCompetitionStatus(room.competitionId, "active")
            }
        }
        val mode = if (room.gameMode == "CHANDA") "CHANDA" else "CHOR"
        startNewRound(mode = mode)
    }

    fun acceptAndStartChallenge(challenge: ChallengeMatch) {
        soundManager.playButtonTap()
        _activeChallengeMatch.value = challenge
        _activeCompetitionRoom.value = null
        startNewRound(mode = challenge.gameMode)
    }

    fun toggleSound() {
        val next = !_soundEnabled.value
        _soundEnabled.value = next
        soundManager.soundEnabled = next
        if (next) soundManager.playButtonTap()
    }

    fun toggleMusic() {
        val next = !_musicEnabled.value
        _musicEnabled.value = next
        soundManager.musicEnabled = next
        soundManager.playButtonTap()
    }

    fun openTutorial() {
        soundManager.playButtonTap()
        _showTutorial.value = true
    }

    fun completeTutorial(startPlayingImmediately: Boolean) {
        soundManager.playButtonTap()
        _tutorialDismissedInSession = true
        _showTutorial.value = false
        val profile = (userProfileState.value as? UiState.Success)?.data
        if (isAuthenticated && profile != null && !profile.tutorialCompleted) {
            viewModelScope.launch {
                repository.updateProfileDetails(
                    username = profile.username,
                    avatarId = profile.avatarId,
                    tutorialCompleted = true
                )
            }
        } else if (!isAuthenticated) {
            _localProfile.value = _localProfile.value.copy(tutorialCompleted = true)
        }
        if (startPlayingImmediately) {
            startNewRound()
        }
    }

    fun updateProfile(username: String, avatarId: String) {
        soundManager.playButtonTap()
        if (!isAuthenticated) {
            _localProfile.value = _localProfile.value.copy(
                username = username.trim().take(40).ifBlank { "চোর প্লেয়ার" },
                avatarId = avatarId.trim().take(32).ifBlank { "avatar_hoodie" }
            )
            showMessage("প্রোফাইল আপডেট হয়েছে!")
            return
        }
        viewModelScope.launch {
            val res = repository.updateProfileDetails(
                username = username,
                avatarId = avatarId,
                tutorialCompleted = true
            )
            if (res.isSuccess) {
                showMessage("প্রোফাইল সফলভাবে আপডেট হয়েছে!")
            } else {
                showMessage("প্রোফাইল আপডেট ব্যর্থ হয়েছে।")
            }
        }
    }

    fun onRoundFinished(
        coinsEarned: Long,
        itemsCollected: Int,
        bestCombo: Int,
        durationSeconds: Int,
        mostCollectedItem: String,
        mangoesInRound: Int,
        coconutsInRound: Int,
        cowsInRound: Int,
        reachedSpeedThief: Boolean
    ) {
        soundManager.stopBackgroundMusic()
        soundManager.playRoundOverVictory()

        val activeRoom = _activeCompetitionRoom.value
        val activeChallenge = _activeChallengeMatch.value
        val mode = _currentGameMode.value
        val compLabel = when {
            activeRoom != null -> "Room: ${activeRoom.title}"
            activeChallenge != null -> "1v1 vs ${activeChallenge.creatorName} (${activeChallenge.creatorScore} 🪙)"
            else -> ""
        }

        val summary = RoundSummary(
            coinsEarned = coinsEarned,
            itemsCollected = itemsCollected,
            bestCombo = bestCombo,
            durationSeconds = durationSeconds,
            mostCollectedItem = mostCollectedItem,
            flaggedSuspicious = false,
            gameMode = mode,
            competitionTitle = compLabel
        )
        _lastRoundSummary.value = summary

        val currentProfile = (userProfileState.value as? UiState.Success)?.data
            ?: UserProfile(userId = currentUserId, username = initialDisplayName)

        if (!isAuthenticated) {
            // Update local profile and score history in memory without calling Firestore
            val boundedCoins = coinsEarned.coerceIn(0L, 50000L)
            val boundedItems = itemsCollected.toLong().coerceIn(0L, 500L)
            val boundedCombo = bestCombo.toLong().coerceIn(1L, 100L)
            val safeDuration = durationSeconds.toLong().coerceIn(10L, 600L)

            val newTotalCoins = (currentProfile.totalCoins + boundedCoins).coerceAtMost(100000000L)
            val newTotalRounds = (currentProfile.totalRounds + 1L).coerceAtMost(1000000L)
            val newBestScore = maxOf(currentProfile.bestScore, boundedCoins).coerceAtMost(50000L)
            val newChandaBest = if (mode == "CHANDA") {
                maxOf(currentProfile.chandaBestScore, boundedCoins).coerceAtMost(50000L)
            } else {
                currentProfile.chandaBestScore
            }
            val newHighestCombo = maxOf(currentProfile.highestCombo, boundedCombo).coerceAtMost(100L)
            val newMangoCount = (currentProfile.mangoCount + mangoesInRound.toLong().coerceAtLeast(0L)).coerceAtMost(1000000L)
            val newCoconutCount = (currentProfile.coconutCount + coconutsInRound.toLong().coerceAtLeast(0L)).coerceAtMost(1000000L)
            val newCowCount = (currentProfile.cowCount + cowsInRound.toLong().coerceAtLeast(0L)).coerceAtMost(1000000L)

            val achievements = currentProfile.unlockedAchievements.toMutableSet()
            if (newMangoCount >= 100L) achievements.add("mango_master")
            if (newCoconutCount >= 100L) achievements.add("coconut_king")
            if (newTotalCoins >= 10000L) achievements.add("coin_hunter")
            if (newHighestCombo >= 10L) achievements.add("combo_master")
            if (reachedSpeedThief) achievements.add("speed_thief")
            if (boundedCoins >= 5000L) achievements.add("chor_boss")
            if (mode == "CHANDA" && boundedCoins >= 500L) achievements.add("chanda_collector")

            _localProfile.value = currentProfile.copy(
                totalCoins = newTotalCoins,
                totalRounds = newTotalRounds,
                bestScore = newBestScore,
                chandaBestScore = newChandaBest,
                highestCombo = newHighestCombo,
                mangoCount = newMangoCount,
                coconutCount = newCoconutCount,
                cowCount = newCowCount,
                unlockedAchievements = achievements.toList().take(20)
            )

            val localScore = ScoreEntry(
                scoreId = "local_${UUID.randomUUID().toString().take(8)}",
                userId = currentUserId,
                username = currentProfile.username,
                avatarId = currentProfile.avatarId,
                coinsEarned = boundedCoins,
                itemsCollected = boundedItems,
                bestCombo = boundedCombo,
                durationSeconds = safeDuration,
                mostCollectedItem = mostCollectedItem.take(64).ifBlank { "আম" },
                periodDay = currentDayKey(),
                periodWeek = currentWeekKey(),
                flaggedSuspicious = false,
                status = "valid",
                createdAt = Timestamp.now()
            )
            _localScores.value = (listOf(localScore) + _localScores.value)
                .sortedByDescending { it.coinsEarned }

            val localSession = GameSession(
                sessionId = "session_${UUID.randomUUID().toString().take(8)}",
                userId = currentUserId,
                username = currentProfile.username,
                gameMode = mode,
                coinsEarned = boundedCoins,
                itemsCollected = boundedItems,
                bestCombo = boundedCombo,
                durationSeconds = safeDuration,
                createdAt = Timestamp.now()
            )
            _localSessions.value = listOf(localSession) + _localSessions.value
            return
        }

        viewModelScope.launch {
            val result = repository.submitRoundResult(
                profile = currentProfile,
                coinsEarned = coinsEarned,
                itemsCollected = itemsCollected.toLong(),
                bestCombo = bestCombo.toLong(),
                durationSeconds = durationSeconds.toLong(),
                mostCollectedItem = mostCollectedItem,
                mangoesInRound = mangoesInRound.toLong(),
                coconutsInRound = coconutsInRound.toLong(),
                cowsInRound = cowsInRound.toLong(),
                reachedSpeedThief = reachedSpeedThief,
                gameMode = mode,
                activeRoomCode = activeRoom?.roomCode ?: "",
                activeCompetitionId = activeRoom?.competitionId ?: "",
                activeChallengeId = activeChallenge?.challengeId ?: ""
            )
            result.onSuccess { entry ->
                _lastRoundSummary.value = summary.copy(flaggedSuspicious = entry.flaggedSuspicious)
            }
        }
    }

    fun claimRewardedAdBonus() {
        val currentSummary = _lastRoundSummary.value ?: return
        if (currentSummary.rewardedBonusClaimed) return
        val settings = (gameSettingsState.value as? UiState.Success)?.data ?: GameSettings()
        val bonus = settings.rewardedCoinBonus.coerceIn(10L, 500L)
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return

        soundManager.playCollectSuccess(3)
        _lastRoundSummary.value = currentSummary.copy(
            coinsEarned = currentSummary.coinsEarned + bonus,
            rewardedBonusClaimed = true
        )
        if (!isAuthenticated) {
            _localProfile.value = profile.copy(
                totalCoins = (profile.totalCoins + bonus).coerceAtMost(100000000L)
            )
            showMessage("🎁 বোনাস +$bonus Virtual Coins যোগ হয়েছে!")
            return
        }
        viewModelScope.launch {
            repository.claimRewardedAdBonus(profile, bonus)
            showMessage("🎁 বোনাস +$bonus Virtual Coins যোগ হয়েছে!")
        }
    }

    fun dismissRoundSummaryToLeaderboard() {
        soundManager.playButtonTap()
        _lastRoundSummary.value = null
        _selectedTab.value = MainNavTab.RANKING
    }

    // =========================================================================
    // MULTIPLAYER, REFERRAL, TEAM & CHALLENGE ACTIONS (via DataManager)
    // =========================================================================

    private fun requireSignInMessage(): Boolean {
        if (!isAuthenticated) {
            showMessage("ক্লাউড মাল্টিপ্লেয়ার, টিম ও রেফারেল ব্যবহারের জন্য Google দিয়ে Sign In করুন!")
            return false
        }
        return true
    }

    fun applyReferralCode(rawCode: String) {
        soundManager.playButtonTap()
        if (!requireSignInMessage()) return
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        val settings = (gameSettingsState.value as? UiState.Success)?.data ?: GameSettings()
        viewModelScope.launch {
            val res = repository.dataManager.applyReferralCode(
                profile = profile,
                rawReferralCode = rawCode,
                rewardCoins = settings.referralRewardCoins
            )
            res.onSuccess { rec ->
                showMessage("🎉 রেফারেল সফল! +${rec.rewardCoins} Virtual Coins যোগ হয়েছে!")
            }.onFailure { err ->
                showMessage(err.message ?: "রেফারেল কোড যুক্ত করা যায়নি।")
            }
        }
    }

    fun addFriend(friendCodeOrId: String, friendUsername: String) {
        soundManager.playButtonTap()
        if (!requireSignInMessage()) return
        viewModelScope.launch {
            val res = repository.dataManager.addFriendConnection(
                friendUserId = friendCodeOrId,
                friendUsername = friendUsername
            )
            if (res.isSuccess) {
                showMessage("👥 $friendUsername বন্ধু তালিকায় যুক্ত হয়েছে!")
            } else {
                showMessage(res.exceptionOrNull()?.message ?: "বন্ধু যোগ করা যায়নি।")
            }
        }
    }

    fun removeFriend(friendshipId: String) {
        soundManager.playButtonTap()
        if (!requireSignInMessage()) return
        viewModelScope.launch {
            repository.dataManager.removeFriend(friendshipId)
            showMessage("বন্ধু তালিকা থেকে সরানো হয়েছে।")
        }
    }

    fun createTeam(teamName: String, teamAvatar: String) {
        soundManager.playButtonTap()
        if (!requireSignInMessage()) return
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val res = repository.dataManager.createTeam(profile, teamName, teamAvatar)
            res.onSuccess { team ->
                showMessage("🛡️ টিম '${team.teamName}' তৈরি হয়েছে! কোড: ${team.teamCode}")
            }.onFailure {
                showMessage("টিম তৈরি ব্যর্থ হয়েছে।")
            }
        }
    }

    fun joinTeam(rawTeamCode: String) {
        soundManager.playButtonTap()
        if (!requireSignInMessage()) return
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        val settings = (gameSettingsState.value as? UiState.Success)?.data ?: GameSettings()
        viewModelScope.launch {
            val res = repository.dataManager.joinTeamByCode(
                profile = profile,
                rawTeamCode = rawTeamCode,
                maxTeamSize = settings.maxTeamSize.toInt()
            )
            res.onSuccess { team ->
                showMessage("🛡️ তুমি '${team.teamName}' টিমে যোগ দিয়েছো!")
            }.onFailure { err ->
                showMessage(err.message ?: "টিমে যোগ দেওয়া যায়নি।")
            }
        }
    }

    fun removeTeamMember(team: TeamRecord, memberIndex: Int) {
        soundManager.playButtonTap()
        if (!requireSignInMessage()) return
        viewModelScope.launch {
            val res = repository.dataManager.removeTeamMemberAsManager(team, memberIndex)
            if (res.isSuccess) {
                showMessage("মেম্বার রিমুভ করা হয়েছে।")
            } else {
                showMessage(res.exceptionOrNull()?.message ?: "রিমুভ ব্যর্থ হয়েছে।")
            }
        }
    }

    fun createCompetitionRoom(title: String, gameMode: String) {
        soundManager.playButtonTap()
        if (!requireSignInMessage()) return
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        val settings = (gameSettingsState.value as? UiState.Success)?.data ?: GameSettings()
        val duration = if (gameMode == "CHANDA") settings.chandaDurationSeconds else settings.roundDurationSeconds
        viewModelScope.launch {
            val res = repository.dataManager.createCompetitionRoom(
                profile = profile,
                title = title,
                gameMode = gameMode,
                durationSeconds = duration
            )
            res.onSuccess { room ->
                _activeCompetitionRoom.value = room
                showMessage("⚔️ রুম '${room.title}' তৈরি হয়েছে! কোড: ${room.roomCode}")
            }.onFailure {
                showMessage("রুম তৈরি ব্যর্থ হয়েছে।")
            }
        }
    }

    fun joinCompetitionRoom(rawRoomCode: String) {
        soundManager.playButtonTap()
        if (!requireSignInMessage()) return
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val res = repository.dataManager.joinCompetitionRoomByCode(profile, rawRoomCode)
            res.onSuccess { room ->
                _activeCompetitionRoom.value = room
                showMessage("⚔️ '${room.title}' রুমে যুক্ত হয়েছো!")
            }.onFailure { err ->
                showMessage(err.message ?: "রুমে যোগ দেওয়া যায়নি।")
            }
        }
    }

    fun closeCompetitionRoom(room: CompetitionRoom) {
        soundManager.playButtonTap()
        if (!requireSignInMessage()) return
        viewModelScope.launch {
            repository.dataManager.updateCompetitionStatus(room.competitionId, "closed")
            if (_activeCompetitionRoom.value?.competitionId == room.competitionId) {
                _activeCompetitionRoom.value = null
            }
            showMessage("রুমটি বন্ধ করা হয়েছে।")
        }
    }

    fun createScoreChallenge(score: Long, gameMode: String = "CHOR") {
        soundManager.playButtonTap()
        if (!requireSignInMessage()) return
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val res = repository.dataManager.createScoreChallenge(profile, score, gameMode)
            res.onSuccess { ch ->
                showMessage("🔥 1v1 চ্যালেঞ্জ তৈরি হয়েছে! কোড: ${ch.challengeCode}")
            }.onFailure {
                showMessage("চ্যালেঞ্জ তৈরি করা যায়নি।")
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        if (!isAuthenticated) return
        viewModelScope.launch {
            repository.dataManager.markNotificationRead(notificationId)
        }
    }

    // =========================================================================
    // ADMIN PANEL FUNCTIONS
    // =========================================================================

    fun openAdminPanel() {
        soundManager.playButtonTap()
        _showAdminPanel.value = true
        if (isCurrentUserAdmin()) {
            startAdminStreams()
        }
    }

    fun closeAdminPanel() {
        soundManager.playButtonTap()
        _showAdminPanel.value = false
    }

    private fun startAdminStreams() {
        if (!isAuthenticated) return
        if (adminUsersJob == null) {
            adminUsersJob = viewModelScope.launch {
                repository.observeAllUsersForAdmin()
                    .catch { e -> Log.w(TAG, "Admin users stream error", e) }
                    .collect { _adminUsers.value = it }
            }
        }
        if (adminScoresJob == null) {
            adminScoresJob = viewModelScope.launch {
                repository.observeAllScoresForAdmin()
                    .catch { e -> Log.w(TAG, "Admin scores stream error", e) }
                    .collect { _adminAllScores.value = it }
            }
        }
        if (adminReferralsJob == null) {
            adminReferralsJob = viewModelScope.launch {
                repository.observeAllReferralsForAdmin()
                    .catch { e -> Log.w(TAG, "Admin referrals stream error", e) }
                    .collect { _adminReferrals.value = it }
            }
        }
    }

    fun saveGameSettings(newSettings: GameSettings) {
        soundManager.playButtonTap()
        if (!requireSignInMessage()) return
        viewModelScope.launch {
            val res = repository.saveGameSettingsAsAdmin(newSettings)
            if (res.isSuccess) {
                showMessage("গেম, ডোমেইন এবং অ্যাড সেটিংস সংরক্ষিত হয়েছে!")
            } else {
                showMessage("সেটিংস সংরক্ষণ করতে অ্যাডমিন অনুমতি প্রয়োজন।")
            }
        }
    }

    fun saveCollectibleItem(item: CollectibleItemConfig) {
        soundManager.playButtonTap()
        if (!requireSignInMessage()) return
        viewModelScope.launch {
            val res = repository.saveCollectibleItemAsAdmin(item)
            if (res.isSuccess) {
                showMessage("${item.emoji} ${item.nameBn} আপডেট হয়েছে!")
            } else {
                showMessage("আইটেম আপডেট করতে অ্যাডমিন অনুমতি প্রয়োজন।")
            }
        }
    }

    fun seedDefaultItems() {
        soundManager.playButtonTap()
        if (!requireSignInMessage()) return
        viewModelScope.launch {
            val res = repository.seedDefaultItemsAsAdmin()
            if (res.isSuccess) {
                showMessage("ডিফল্ট ১৩টি আইটেম ডাটাবেসে সিঙ্ক হয়েছে!")
            } else {
                showMessage("আইটেম সিঙ্ক ব্যর্থ হয়েছে।")
            }
        }
    }

    fun toggleUserSuspension(user: UserProfile) {
        soundManager.playButtonTap()
        if (!requireSignInMessage()) return
        viewModelScope.launch {
            val res = repository.setUserSuspendedAsAdmin(user.userId, !user.suspended)
            if (res.isSuccess) {
                showMessage(if (!user.suspended) "${user.username} সাসপেন্ড করা হয়েছে।" else "${user.username} আন-সাসপেন্ড করা হয়েছে।")
            }
        }
    }

    fun toggleTeamSuspension(team: TeamRecord) {
        soundManager.playButtonTap()
        if (!requireSignInMessage()) return
        viewModelScope.launch {
            val res = repository.dataManager.setTeamSuspendedAsAdmin(team.teamId, !team.suspended)
            if (res.isSuccess) {
                showMessage("টিম '${team.teamName}' স্ট্যাটাস আপডেট হয়েছে।")
            }
        }
    }

    fun deleteCompetitionAsAdmin(competitionId: String) {
        soundManager.playButtonTap()
        if (!requireSignInMessage()) return
        viewModelScope.launch {
            val res = repository.dataManager.deleteCompetitionAsAdmin(competitionId)
            if (res.isSuccess) {
                showMessage("কম্পিটিশন রুম মুছে ফেলা হয়েছে।")
            }
        }
    }

    fun deleteInvalidScore(scoreId: String) {
        soundManager.playButtonTap()
        if (!requireSignInMessage()) return
        viewModelScope.launch {
            val res = repository.deleteScoreAsAdmin(scoreId)
            if (res.isSuccess) {
                showMessage("স্কোরটি মুছে ফেলা হয়েছে।")
            }
        }
    }

    fun approveFlaggedScore(scoreId: String) {
        soundManager.playButtonTap()
        if (!requireSignInMessage()) return
        viewModelScope.launch {
            repository.updateScoreStatusAsAdmin(scoreId, "valid", false)
            showMessage("স্কোরটি ভ্যালিড হিসেবে অনুমোদিত হয়েছে।")
        }
    }

    fun resetDailyLeaderboard(dayKey: String) {
        soundManager.playButtonTap()
        if (!requireSignInMessage()) return
        viewModelScope.launch {
            val res = repository.resetPeriodScoresAsAdmin(periodDay = dayKey)
            res.onSuccess { count ->
                showMessage("আজকের লিডারবোর্ড রিসেট হয়েছে ($count টি স্কোর মুছেছে)।")
            }
        }
    }

    fun resetWeeklyLeaderboard(weekKey: String) {
        soundManager.playButtonTap()
        if (!requireSignInMessage()) return
        viewModelScope.launch {
            val res = repository.resetPeriodScoresAsAdmin(periodWeek = weekKey)
            res.onSuccess { count ->
                showMessage("সাপ্তাহিক লিডারবোর্ড রিসেট হয়েছে ($count টি স্কোর মুছেছে)।")
            }
        }
    }

    fun clearStatusMessage() {
        _statusBannerMessage.value = null
    }

    fun showMessage(msg: String) {
        _statusBannerMessage.value = msg
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.stopBackgroundMusic()
    }

    private companion object {
        const val TAG = "ChorViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
