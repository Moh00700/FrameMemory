package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.extractFriendlyFirestoreError
import com.example.data.MemoryFirestoreRepository
import com.example.data.PosterRepository
import com.example.data.SceneSearchRepository
import com.example.model.CommunityMemory
import com.example.model.CountryFilterOption
import com.example.model.DecadeFilterOption
import com.example.model.LanguageFilterOption
import com.example.model.MediaCategory
import com.example.model.MediaFilterOption
import com.example.model.MemorySearchFilters
import com.example.model.SceneCandidate
import com.example.model.SearchUiState
import com.example.model.SolvedMemory
import com.example.model.StyleFilterOption
import com.example.model.UiState
import com.example.model.VoteChoice
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val VM_TAG = "FrameMemoryVM"
private const val STOP_TIMEOUT_MS = 5000L

enum class TrendingSortMode(val label: String) {
    MOST_CONFIRMED("Top Confirmed"),
    NEWEST("Newest Gems")
}

class FrameMemoryViewModel(
    private val sceneSearchRepository: SceneSearchRepository,
    private val posterRepository: PosterRepository,
    private val firestoreRepository: MemoryFirestoreRepository,
    val currentUserId: String,
    val currentUserName: String
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filters = MutableStateFlow(MemorySearchFilters())
    val filters: StateFlow<MemorySearchFilters> = _filters.asStateFlow()

    private val _searchState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _votingMemoryIds = MutableStateFlow<Set<String>>(emptySet())
    val votingMemoryIds: StateFlow<Set<String>> = _votingMemoryIds.asStateFlow()

    private val _trendingSort = MutableStateFlow(TrendingSortMode.MOST_CONFIRMED)
    val trendingSort: StateFlow<TrendingSortMode> = _trendingSort.asStateFlow()

    private val _trendingMediaFilter = MutableStateFlow<MediaCategory?>(null)
    val trendingMediaFilter: StateFlow<MediaCategory?> = _trendingMediaFilter.asStateFlow()

    private val _vaultSearchQuery = MutableStateFlow("")
    val vaultSearchQuery: StateFlow<String> = _vaultSearchQuery.asStateFlow()

    private val _vaultMediaFilter = MutableStateFlow<MediaCategory?>(null)
    val vaultMediaFilter: StateFlow<MediaCategory?> = _vaultMediaFilter.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var activeSearchJob: Job? = null

    val solvedMemoriesState: StateFlow<UiState<List<SolvedMemory>>> =
        firestoreRepository.observeSolvedMemories(currentUserId)
            .map<List<SolvedMemory>, UiState<List<SolvedMemory>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(VM_TAG, "Error observing solved memories", root)
                emit(UiState.Error(extractFriendlyFirestoreError(root, "Failed to load your vault.")))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    val communityMemoriesState: StateFlow<UiState<List<CommunityMemory>>> =
        firestoreRepository.observeCommunityMemories()
            .map<List<CommunityMemory>, UiState<List<CommunityMemory>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(VM_TAG, "Error observing community memories", root)
                emit(UiState.Error(extractFriendlyFirestoreError(root, "Failed to load trending gems.")))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    val userVotes: StateFlow<Map<String, VoteChoice>> =
        firestoreRepository.observeUserVotes(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(VM_TAG, "Error observing user votes", firestoreError ?: error)
                emit(emptyMap())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = emptyMap()
            )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query.take(500)
    }

    fun selectMediaFilter(option: MediaFilterOption) {
        _filters.update { it.copy(media = option) }
    }

    fun selectDecadeFilter(option: DecadeFilterOption) {
        _filters.update { it.copy(decade = option) }
    }

    fun selectCountryFilter(option: CountryFilterOption) {
        _filters.update { it.copy(country = option) }
    }

    fun selectStyleFilter(option: StyleFilterOption) {
        _filters.update { it.copy(style = option) }
    }

    fun selectLanguageFilter(option: LanguageFilterOption) {
        _filters.update { it.copy(language = option) }
    }

    fun resetFilters() {
        _filters.value = MemorySearchFilters()
    }

    fun resetSearch() {
        activeSearchJob?.cancel()
        _searchState.value = SearchUiState.Idle
    }

    fun setTrendingSort(mode: TrendingSortMode) {
        _trendingSort.value = mode
    }

    fun setTrendingMediaFilter(category: MediaCategory?) {
        _trendingMediaFilter.value = category
    }

    fun setVaultSearchQuery(query: String) {
        _vaultSearchQuery.value = query
    }

    fun setVaultMediaFilter(category: MediaCategory?) {
        _vaultMediaFilter.value = category
    }

    fun searchScene(excludeDismissed: Boolean = false) {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) {
            _events.tryEmit("Describe a scene, quote, object, or feeling first.")
            return
        }

        val currentExcluded = if (excludeDismissed) {
            val currentResults = _searchState.value as? SearchUiState.Results
            currentResults?.candidates?.filter { it.isDismissed }?.map { it.title }.orEmpty()
        } else {
            emptyList()
        }

        val activeFilters = _filters.value
        activeSearchJob?.cancel()
        activeSearchJob = viewModelScope.launch {
            _searchState.value = SearchUiState.Searching
            try {
                val candidates = sceneSearchRepository.findMatchingScenes(
                    memoryQuery = query,
                    filters = activeFilters,
                    excludedTitles = currentExcluded
                )
                _searchState.value = SearchUiState.Results(
                    queryText = query,
                    filterSummary = activeFilters.toSummary(),
                    candidates = candidates
                )
                // Resolve real posters concurrently without blocking results display
                candidates.forEach { candidate ->
                    launch {
                        val posterUrl = posterRepository.resolvePosterUrl(
                            title = candidate.title,
                            year = candidate.year,
                            mediaType = candidate.mediaType
                        )
                        _searchState.update { state ->
                            if (state is SearchUiState.Results) {
                                state.copy(
                                    candidates = state.candidates.map { item ->
                                        if (item.candidateId == candidate.candidateId) {
                                            item.copy(posterUrl = posterUrl, isPosterLoading = false)
                                        } else {
                                            item
                                        }
                                    }
                                )
                            } else {
                                state
                            }
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(VM_TAG, "Scene search timed out", e)
                _searchState.value = SearchUiState.Error(
                    "Search timed out after 60 seconds. Check your connection and try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VM_TAG, "Scene search failed", e)
                _searchState.value = SearchUiState.Error(
                    e.localizedMessage ?: "Unable to match that scene right now."
                )
            }
        }
    }

    fun toggleDismissCandidate(candidateId: String) {
        _searchState.update { state ->
            if (state is SearchUiState.Results) {
                state.copy(
                    candidates = state.candidates.map {
                        if (it.candidateId == candidateId) it.copy(isDismissed = !it.isDismissed) else it
                    }
                )
            } else {
                state
            }
        }
    }

    fun solveAndSaveCandidate(
        candidate: SceneCandidate,
        personalNote: String,
        shareToCommunity: Boolean,
        onComplete: () -> Unit
    ) {
        val currentResults = _searchState.value as? SearchUiState.Results
        val query = currentResults?.queryText ?: _searchQuery.value.trim().ifBlank { candidate.title }
        val filterSummary = currentResults?.filterSummary ?: _filters.value.toSummary()

        viewModelScope.launch {
            _isSaving.value = true
            val result = firestoreRepository.saveSolvedMemory(
                userId = currentUserId,
                authorName = currentUserName,
                queryText = query,
                filterSummary = filterSummary,
                candidate = candidate,
                personalNote = personalNote,
                shareToCommunity = shareToCommunity
            )
            _isSaving.value = false
            result.fold(
                onSuccess = {
                    val msg = if (shareToCommunity) {
                        "Mystery solved! Saved to your Vault & shared to Trending Gems."
                    } else {
                        "Mystery solved! Saved to your personal Vault."
                    }
                    _events.tryEmit(msg)
                    onComplete()
                },
                onFailure = { error ->
                    _events.tryEmit(
                        extractFriendlyFirestoreError(error, "Could not save solved mystery.")
                    )
                }
            )
        }
    }

    fun updateSolvedNote(memoryId: String, newNote: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = firestoreRepository.updatePersonalNote(currentUserId, memoryId, newNote)
            _isSaving.value = false
            result.fold(
                onSuccess = {
                    _events.tryEmit("Rewatch note updated.")
                    onDone()
                },
                onFailure = { error ->
                    _events.tryEmit(extractFriendlyFirestoreError(error, "Failed to update note."))
                }
            )
        }
    }

    fun shareExistingToCommunity(memory: SolvedMemory) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = firestoreRepository.shareSolvedMemoryToCommunity(
                userId = currentUserId,
                authorName = currentUserName,
                memory = memory
            )
            _isSaving.value = false
            result.fold(
                onSuccess = {
                    _events.tryEmit("Shared '${memory.title}' to Trending Gems!")
                },
                onFailure = { error ->
                    _events.tryEmit(extractFriendlyFirestoreError(error, "Could not share to community."))
                }
            )
        }
    }

    fun deleteSolvedMemory(memory: SolvedMemory, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = firestoreRepository.deleteSolvedMemory(currentUserId, memory)
            _isSaving.value = false
            result.fold(
                onSuccess = {
                    _events.tryEmit("Removed '${memory.title}' from your Vault.")
                    onDone()
                },
                onFailure = { error ->
                    _events.tryEmit(extractFriendlyFirestoreError(error, "Failed to remove item."))
                }
            )
        }
    }

    fun voteOnCommunityMemory(memoryId: String, vote: VoteChoice) {
        if (_votingMemoryIds.value.contains(memoryId)) return
        viewModelScope.launch {
            _votingMemoryIds.update { it + memoryId }
            val result = firestoreRepository.castCommunityVote(currentUserId, memoryId, vote)
            _votingMemoryIds.update { it - memoryId }
            result.onFailure { error ->
                _events.tryEmit(extractFriendlyFirestoreError(error, "Vote failed. Try again."))
            }
        }
    }
}
