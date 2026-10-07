package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.DefaultGameCatalog
import com.example.data.model.GameSettings
import com.example.data.model.UiState
import com.example.data.repository.ChorRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOut
import com.example.ui.components.FirstTimeTutorialDialog
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.GameplayScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MultiplayerHubScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RankingScreen
import com.example.ui.theme.ChorDangerCrimson
import com.example.ui.theme.ChorGold
import com.example.ui.theme.ChorMidnightBg
import com.example.ui.theme.ChorSurfaceDark
import com.example.ui.theme.ChorTextSecondary
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ChorViewModel
import com.example.viewmodel.MainNavTab
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deepLinkPath = intent?.data?.path
        setContent {
            MyApplicationTheme {
                AppNavigation(initialDeepLinkPath = deepLinkPath)
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(
    auth: FirebaseAuth = Firebase.auth,
    initialDeepLinkPath: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var showSignInOverlay by remember { mutableStateOf(false) }

    // Attempt silent auto-sign-in on startup without blocking the CHOR Home Screen
    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { showSignInOverlay = false },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    val isAuthenticated = user != null
    val activeUserId = user?.uid ?: "chor_local_player"
    val activeDisplayName = user?.displayName ?: "চোর প্লেয়ার"

    if (showSignInOverlay && !isAuthenticated) {
        AuthScreen(
            onAuthSuccess = { showSignInOverlay = false },
            onContinueToHome = { showSignInOverlay = false }
        )
    } else {
        ChorMainGameApp(
            currentUserId = activeUserId,
            initialDisplayName = activeDisplayName,
            isAuthenticated = isAuthenticated,
            initialDeepLinkPath = initialDeepLinkPath,
            onOpenSignIn = { showSignInOverlay = true }
        )
    }
}

@Composable
fun ChorMainGameApp(
    currentUserId: String,
    initialDisplayName: String,
    isAuthenticated: Boolean = true,
    initialDeepLinkPath: String? = null,
    onOpenSignIn: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: ChorViewModel = viewModel(
        key = "${currentUserId}_$isAuthenticated",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ChorViewModel(
                    repository = ChorRepository(db),
                    currentUserId = currentUserId,
                    initialDisplayName = initialDisplayName,
                    isAuthenticated = isAuthenticated
                )
            }
        }
    )

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val multiplayerInitialSection by viewModel.multiplayerInitialSection.collectAsStateWithLifecycle()
    val currentGameMode by viewModel.currentGameMode.collectAsStateWithLifecycle()
    val activeCompetitionRoom by viewModel.activeCompetitionRoom.collectAsStateWithLifecycle()

    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val musicEnabled by viewModel.musicEnabled.collectAsStateWithLifecycle()
    val showTutorial by viewModel.showTutorial.collectAsStateWithLifecycle()
    val showAdminPanel by viewModel.showAdminPanel.collectAsStateWithLifecycle()
    val lastRoundSummary by viewModel.lastRoundSummary.collectAsStateWithLifecycle()
    val statusBannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val validScoresState by viewModel.validScoresState.collectAsStateWithLifecycle()
    val userScoresState by viewModel.userScoresState.collectAsStateWithLifecycle()
    val userSessionsState by viewModel.userSessionsState.collectAsStateWithLifecycle()
    val userReferralsState by viewModel.userReferralsState.collectAsStateWithLifecycle()
    val friendsState by viewModel.friendsState.collectAsStateWithLifecycle()
    val activeTeamsState by viewModel.activeTeamsState.collectAsStateWithLifecycle()
    val competitionsState by viewModel.competitionsState.collectAsStateWithLifecycle()
    val challengesState by viewModel.challengesState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val gameSettingsState by viewModel.gameSettingsState.collectAsStateWithLifecycle()
    val activeItemsState by viewModel.activeItemsState.collectAsStateWithLifecycle()

    val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
    val adminAllScores by viewModel.adminAllScores.collectAsStateWithLifecycle()
    val adminReferrals by viewModel.adminReferrals.collectAsStateWithLifecycle()

    val userProfile = (userProfileState as? UiState.Success)?.data
    val validScores = (validScoresState as? UiState.Success)?.data ?: emptyList()
    val userScores = (userScoresState as? UiState.Success)?.data ?: emptyList()
    val userSessions = (userSessionsState as? UiState.Success)?.data ?: emptyList()
    val userReferrals = (userReferralsState as? UiState.Success)?.data ?: emptyList()
    val friendsList = (friendsState as? UiState.Success)?.data ?: emptyList()
    val activeTeams = (activeTeamsState as? UiState.Success)?.data ?: emptyList()
    val competitions = (competitionsState as? UiState.Success)?.data ?: emptyList()
    val challenges = (challengesState as? UiState.Success)?.data ?: emptyList()
    val notifications = (notificationsState as? UiState.Success)?.data ?: emptyList()
    val gameSettings = (gameSettingsState as? UiState.Success)?.data ?: GameSettings()
    val activeItems = (activeItemsState as? UiState.Success)?.data ?: DefaultGameCatalog.defaultItems

    val unreadNotificationsCount = remember(notifications) {
        notifications.count { !it.read }
    }

    // Handle deep links like /invite/CODE, /match/CODE, or /challenge/CODE
    LaunchedEffect(initialDeepLinkPath) {
        val path = initialDeepLinkPath ?: return@LaunchedEffect
        when {
            path.startsWith("/invite/") -> {
                val code = path.removePrefix("/invite/").trim()
                if (code.isNotEmpty()) {
                    viewModel.openMultiplayerSection(0)
                    viewModel.applyReferralCode(code)
                }
            }
            path.startsWith("/match/") -> {
                val code = path.removePrefix("/match/").trim()
                if (code.isNotEmpty()) {
                    viewModel.openMultiplayerSection(1)
                    viewModel.joinCompetitionRoom(code)
                }
            }
            path.startsWith("/challenge/") -> {
                val code = path.removePrefix("/challenge/").trim()
                if (code.isNotEmpty()) {
                    viewModel.openMultiplayerSection(3)
                }
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBannerMessage) {
        val msg = statusBannerMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = ChorMidnightBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!showAdminPanel && selectedTab != MainNavTab.PLAY) {
                NavigationBar(
                    containerColor = ChorSurfaceDark,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == MainNavTab.HOME,
                        onClick = { viewModel.selectTab(MainNavTab.HOME) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF0A0F1D),
                            selectedTextColor = ChorGold,
                            indicatorColor = ChorGold,
                            unselectedIconColor = ChorTextSecondary,
                            unselectedTextColor = ChorTextSecondary
                        ),
                        modifier = Modifier.testTag("nav_home")
                    )
                    NavigationBarItem(
                        selected = selectedTab == MainNavTab.PLAY,
                        onClick = { viewModel.startNewRound("CHOR") },
                        icon = { Icon(Icons.Default.SportsEsports, contentDescription = "Play") },
                        label = { Text("Play", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF0A0F1D),
                            selectedTextColor = ChorGold,
                            indicatorColor = ChorGold,
                            unselectedIconColor = ChorTextSecondary,
                            unselectedTextColor = ChorTextSecondary
                        ),
                        modifier = Modifier.testTag("nav_play")
                    )
                    NavigationBarItem(
                        selected = selectedTab == MainNavTab.MULTIPLAYER,
                        onClick = { viewModel.selectTab(MainNavTab.MULTIPLAYER) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (unreadNotificationsCount > 0) {
                                        Badge(containerColor = ChorDangerCrimson) {
                                            Text("$unreadNotificationsCount", color = Color.White)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Groups, contentDescription = "Friends & Teams")
                            }
                        },
                        label = { Text("Friends", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF0A0F1D),
                            selectedTextColor = ChorGold,
                            indicatorColor = ChorGold,
                            unselectedIconColor = ChorTextSecondary,
                            unselectedTextColor = ChorTextSecondary
                        ),
                        modifier = Modifier.testTag("nav_multiplayer")
                    )
                    NavigationBarItem(
                        selected = selectedTab == MainNavTab.RANKING,
                        onClick = { viewModel.selectTab(MainNavTab.RANKING) },
                        icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Ranking") },
                        label = { Text("Ranking", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF0A0F1D),
                            selectedTextColor = ChorGold,
                            indicatorColor = ChorGold,
                            unselectedIconColor = ChorTextSecondary,
                            unselectedTextColor = ChorTextSecondary
                        ),
                        modifier = Modifier.testTag("nav_ranking")
                    )
                    NavigationBarItem(
                        selected = selectedTab == MainNavTab.PROFILE,
                        onClick = { viewModel.selectTab(MainNavTab.PROFILE) },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF0A0F1D),
                            selectedTextColor = ChorGold,
                            indicatorColor = ChorGold,
                            unselectedIconColor = ChorTextSecondary,
                            unselectedTextColor = ChorTextSecondary
                        ),
                        modifier = Modifier.testTag("nav_profile")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showAdminPanel) {
                AdminPanelScreen(
                    isAdmin = viewModel.isCurrentUserAdmin(),
                    settings = gameSettings,
                    items = activeItems,
                    allUsers = adminUsers,
                    allScores = adminAllScores,
                    validScores = validScores,
                    allReferrals = adminReferrals,
                    activeTeams = activeTeams,
                    competitions = competitions,
                    onSaveSettings = { viewModel.saveGameSettings(it) },
                    onSaveItem = { viewModel.saveCollectibleItem(it) },
                    onSeedDefaultItems = { viewModel.seedDefaultItems() },
                    onToggleUserSuspend = { viewModel.toggleUserSuspension(it) },
                    onDeleteScore = { viewModel.deleteInvalidScore(it) },
                    onApproveFlaggedScore = { viewModel.approveFlaggedScore(it) },
                    onResetDailyLeaderboard = { viewModel.resetDailyLeaderboard(it) },
                    onResetWeeklyLeaderboard = { viewModel.resetWeeklyLeaderboard(it) },
                    onClose = { viewModel.closeAdminPanel() }
                )
            } else {
                when (selectedTab) {
                    MainNavTab.HOME -> HomeScreen(
                        userProfile = userProfile,
                        validScores = validScores,
                        settings = gameSettings,
                        activeItems = activeItems,
                        soundEnabled = soundEnabled,
                        musicEnabled = musicEnabled,
                        onStartGame = { viewModel.startNewRound("CHOR") },
                        onStartChandaBonusMode = { viewModel.startNewRound("CHANDA") },
                        onOpenFriendCompetition = { viewModel.openMultiplayerSection(1) },
                        onOpenInviteHub = { viewModel.openMultiplayerSection(0) },
                        onOpenTutorial = { viewModel.openTutorial() },
                        onToggleSound = { viewModel.toggleSound() },
                        onToggleMusic = { viewModel.toggleMusic() },
                        onOpenAdminPanel = { viewModel.openAdminPanel() },
                        onShowMessage = { msg -> viewModel.showMessage(msg) },
                        isAuthenticated = isAuthenticated,
                        onOpenSignIn = onOpenSignIn
                    )
                    MainNavTab.PLAY -> GameplayScreen(
                        settings = gameSettings,
                        activeItems = activeItems,
                        personalBest = userProfile?.bestScore ?: 0L,
                        soundEnabled = soundEnabled,
                        roundSummary = lastRoundSummary,
                        soundManager = viewModel.soundManager,
                        gameMode = currentGameMode,
                        playerUsername = userProfile?.username ?: initialDisplayName,
                        activeCompetition = activeCompetitionRoom,
                        onSyncCompetitionScore = {},
                        onSendChallengeFromResult = {
                            val coins = lastRoundSummary?.coinsEarned ?: 0L
                            viewModel.createScoreChallenge(coins, currentGameMode)
                        },
                        onInviteFriendsFromResult = {
                            viewModel.openMultiplayerSection(0)
                        },
                        onToggleSound = { viewModel.toggleSound() },
                        onRoundComplete = { coins, items, combo, duration, topItem, mangoes, coconuts, cows, speedThief ->
                            viewModel.onRoundFinished(
                                coinsEarned = coins,
                                itemsCollected = items,
                                bestCombo = combo,
                                durationSeconds = duration,
                                mostCollectedItem = topItem,
                                mangoesInRound = mangoes,
                                coconutsInRound = coconuts,
                                cowsInRound = cows,
                                reachedSpeedThief = speedThief
                            )
                        },
                        onPlayAgain = {
                            viewModel.startNewRound(currentGameMode)
                        },
                        onOpenLeaderboard = { viewModel.dismissRoundSummaryToLeaderboard() },
                        onClaimRewardedBonus = { viewModel.claimRewardedAdBonus() },
                        onExitToHome = { viewModel.selectTab(MainNavTab.HOME) }
                    )
                    MainNavTab.MULTIPLAYER -> MultiplayerHubScreen(
                        currentUserId = currentUserId,
                        userProfile = userProfile,
                        settings = gameSettings,
                        referrals = userReferrals,
                        friends = friendsList,
                        teams = activeTeams,
                        competitions = competitions,
                        challenges = challenges,
                        sessions = userSessions,
                        notifications = notifications,
                        initialSectionIndex = multiplayerInitialSection,
                        onApplyReferralCode = { code -> viewModel.applyReferralCode(code) },
                        onAddFriend = { codeOrId, name -> viewModel.addFriend(codeOrId, name) },
                        onRemoveFriend = { friendshipId -> viewModel.removeFriend(friendshipId) },
                        onCreateTeam = { name, emoji -> viewModel.createTeam(name, emoji) },
                        onJoinTeam = { code -> viewModel.joinTeam(code) },
                        onRemoveTeamMember = { team, idx -> viewModel.removeTeamMember(team, idx) },
                        onCreateRoom = { title, mode -> viewModel.createCompetitionRoom(title, mode) },
                        onJoinRoom = { code -> viewModel.joinCompetitionRoom(code) },
                        onStartRoomMatch = { room -> viewModel.startCompetitionRoomMatch(room) },
                        onCloseRoom = { room -> viewModel.closeCompetitionRoom(room) },
                        onCreateChallenge = { score, mode -> viewModel.createScoreChallenge(score, mode) },
                        onAcceptChallenge = { challenge -> viewModel.acceptAndStartChallenge(challenge) },
                        onMarkNotificationRead = { notifId -> viewModel.markNotificationRead(notifId) },
                        onShowMessage = { msg -> viewModel.showMessage(msg) },
                        onBackToHome = { viewModel.selectTab(MainNavTab.HOME) }
                    )
                    MainNavTab.RANKING -> RankingScreen(
                        currentUserId = currentUserId,
                        userProfile = userProfile,
                        validScores = validScores,
                        friendsList = friendsList,
                        activeTeams = activeTeams,
                        onStartGame = { viewModel.startNewRound("CHOR") },
                        onOpenMultiplayerHub = { section -> viewModel.openMultiplayerSection(section) },
                        onBackToHome = { viewModel.selectTab(MainNavTab.HOME) }
                    )
                    MainNavTab.PROFILE -> ProfileScreen(
                        currentUserId = currentUserId,
                        userProfile = userProfile,
                        validScores = validScores,
                        userScores = userScores,
                        userSessions = userSessions,
                        settings = gameSettings,
                        soundEnabled = soundEnabled,
                        musicEnabled = musicEnabled,
                        onUpdateProfile = { name, avatar -> viewModel.updateProfile(name, avatar) },
                        onToggleSound = { viewModel.toggleSound() },
                        onToggleMusic = { viewModel.toggleMusic() },
                        onOpenAdminPanel = { viewModel.openAdminPanel() },
                        onSignOut = {
                            if (isAuthenticated) {
                                signOut(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onSignOutComplete = {},
                                    scope = scope
                                )
                            } else {
                                onOpenSignIn()
                            }
                        },
                        onBackToHome = { viewModel.selectTab(MainNavTab.HOME) }
                    )
                }
            }

            if (showTutorial) {
                FirstTimeTutorialDialog(
                    onDismissOnly = { viewModel.completeTutorial(startPlayingImmediately = false) },
                    onStartGameClick = { viewModel.completeTutorial(startPlayingImmediately = true) }
                )
            }
        }
    }
}
