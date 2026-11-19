package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.core.authStateFlow
import com.example.core.signOutUser
import com.example.data.MemoryFirestoreRepository
import com.example.data.PosterRepository
import com.example.data.SceneSearchRepository
import com.example.model.SceneCandidate
import com.example.model.SearchUiState
import com.example.ui.components.SolveMysterySheet
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.FindSceneScreen
import com.example.ui.screens.MyVaultScreen
import com.example.ui.screens.TrendingGemsScreen
import com.example.ui.viewmodel.FrameMemoryViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class AppTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    FIND("Find", Icons.Rounded.Search, Icons.Outlined.Search, "tab_find"),
    TRENDING("Trending", Icons.Rounded.LocalFireDepartment, Icons.Outlined.LocalFireDepartment, "tab_trending"),
    VAULT("Vault", Icons.Rounded.Bookmarks, Icons.Outlined.BookmarkBorder, "tab_vault")
}

@Composable
fun FrameMemoryApp(
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedFrameMemoryShell(
                currentUserId = user.uid,
                currentUserName = user.displayName?.takeIf { it.isNotBlank() } ?: "Cinephile"
            )
        }
    }
}

@Composable
private fun AuthenticatedFrameMemoryShell(
    currentUserId: String,
    currentUserName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: FrameMemoryViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                FrameMemoryViewModel(
                    sceneSearchRepository = SceneSearchRepository(),
                    posterRepository = PosterRepository(),
                    firestoreRepository = MemoryFirestoreRepository(db),
                    currentUserId = currentUserId,
                    currentUserName = currentUserName
                )
            }
        }
    )

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val solvedState by viewModel.solvedMemoriesState.collectAsStateWithLifecycle()
    val communityState by viewModel.communityMemoriesState.collectAsStateWithLifecycle()
    val userVotes by viewModel.userVotes.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val votingIds by viewModel.votingMemoryIds.collectAsStateWithLifecycle()
    val trendingSort by viewModel.trendingSort.collectAsStateWithLifecycle()
    val trendingMediaFilter by viewModel.trendingMediaFilter.collectAsStateWithLifecycle()
    val vaultSearchQuery by viewModel.vaultSearchQuery.collectAsStateWithLifecycle()
    val vaultMediaFilter by viewModel.vaultMediaFilter.collectAsStateWithLifecycle()

    var activeTab by rememberSaveable { mutableStateOf(AppTab.FIND) }
    var candidateToSolve by remember { mutableStateOf<SceneCandidate?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BackHandler(enabled = candidateToSolve != null || activeTab != AppTab.FIND) {
        if (candidateToSolve != null) {
            candidateToSolve = null
        } else if (activeTab != AppTab.FIND) {
            activeTab = AppTab.FIND
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 700.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = if (useNavRail) WindowInsets(0, 0, 0, 0) else WindowInsets.navigationBars,
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .safeDrawingPadding()
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .safeDrawingPadding(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxSize()) {
                        when (activeTab) {
                            AppTab.FIND -> FindSceneScreen(
                                searchQuery = searchQuery,
                                filters = filters,
                                searchState = searchState,
                                onQueryChange = viewModel::updateSearchQuery,
                                onSelectMedia = viewModel::selectMediaFilter,
                                onSelectDecade = viewModel::selectDecadeFilter,
                                onSelectCountry = viewModel::selectCountryFilter,
                                onSelectStyle = viewModel::selectStyleFilter,
                                onSelectLanguage = viewModel::selectLanguageFilter,
                                onResetFilters = viewModel::resetFilters,
                                onSearch = viewModel::searchScene,
                                onResetSearch = viewModel::resetSearch,
                                onToggleDismiss = viewModel::toggleDismissCandidate,
                                onOpenSolveSheet = { candidateToSolve = it }
                            )

                            AppTab.TRENDING -> TrendingGemsScreen(
                                communityState = communityState,
                                userVotes = userVotes,
                                votingMemoryIds = votingIds,
                                sortMode = trendingSort,
                                mediaFilter = trendingMediaFilter,
                                onSelectSortMode = viewModel::setTrendingSort,
                                onSelectMediaFilter = viewModel::setTrendingMediaFilter,
                                onVote = viewModel::voteOnCommunityMemory,
                                onGoToSearch = { activeTab = AppTab.FIND }
                            )

                            AppTab.VAULT -> MyVaultScreen(
                                solvedState = solvedState,
                                vaultSearchQuery = vaultSearchQuery,
                                vaultMediaFilter = vaultMediaFilter,
                                userName = currentUserName,
                                isSaving = isSaving,
                                onSearchQueryChange = viewModel::setVaultSearchQuery,
                                onSelectMediaFilter = viewModel::setVaultMediaFilter,
                                onUpdateNote = viewModel::updateSolvedNote,
                                onShareToCommunity = viewModel::shareExistingToCommunity,
                                onDeleteMemory = { viewModel.deleteSolvedMemory(it) },
                                onSignOut = {
                                    signOutUser(credentialManager, scope)
                                },
                                onGoToSearch = { activeTab = AppTab.FIND }
                            )
                        }
                    }
                }
            }
        }
    }

    candidateToSolve?.let { candidate ->
        val query = (searchState as? SearchUiState.Results)?.queryText ?: searchQuery
        SolveMysterySheet(
            candidate = candidate,
            queryText = query,
            isSaving = isSaving,
            onDismiss = { if (!isSaving) candidateToSolve = null },
            onConfirmSolve = { note, shareToCommunity ->
                viewModel.solveAndSaveCandidate(
                    candidate = candidate,
                    personalNote = note,
                    shareToCommunity = shareToCommunity,
                    onComplete = {
                        candidateToSolve = null
                    }
                )
            }
        )
    }
}
