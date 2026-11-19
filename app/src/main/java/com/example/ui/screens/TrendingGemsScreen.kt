package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CommunityMemory
import com.example.model.MediaCategory
import com.example.model.UiState
import com.example.model.VoteChoice
import com.example.ui.components.MetaTagPill
import com.example.ui.components.PosterArtwork
import com.example.ui.components.shareDiscoveryText
import com.example.ui.viewmodel.TrendingSortMode
import com.google.firebase.Timestamp

@Composable
fun TrendingGemsScreen(
    communityState: UiState<List<CommunityMemory>>,
    userVotes: Map<String, VoteChoice>,
    votingMemoryIds: Set<String>,
    sortMode: TrendingSortMode,
    mediaFilter: MediaCategory?,
    onSelectSortMode: (TrendingSortMode) -> Unit,
    onSelectMediaFilter: (MediaCategory?) -> Unit,
    onVote: (memoryId: String, vote: VoteChoice) -> Unit,
    onGoToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        // Header + Filters
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Trending Cinematic Gems",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "User-solved memories — vote \"Was it this?\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrendingSortMode.entries.forEach { mode ->
                    FilterChip(
                        selected = sortMode == mode,
                        onClick = { onSelectSortMode(mode) },
                        label = { Text(mode.label, style = MaterialTheme.typography.bodySmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("trending_sort_${mode.name.lowercase()}")
                    )
                }

                FilterChip(
                    selected = mediaFilter == null,
                    onClick = { onSelectMediaFilter(null) },
                    label = { Text("All", style = MaterialTheme.typography.bodySmall) }
                )

                MediaCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = mediaFilter == cat,
                        onClick = { onSelectMediaFilter(if (mediaFilter == cat) null else cat) },
                        label = { Text(cat.label, style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }
        }

        when (communityState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = communityState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            is UiState.Success -> {
                val filtered = communityState.data
                    .filter { mediaFilter == null || it.mediaEnum() == mediaFilter }
                    .let { list ->
                        when (sortMode) {
                            TrendingSortMode.MOST_CONFIRMED ->
                                list.sortedWith(
                                    compareByDescending<CommunityMemory> { it.yesCount - it.noCount }
                                        .thenByDescending { it.yesCount }
                                        .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                                )
                            TrendingSortMode.NEWEST ->
                                list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        }
                    }

                if (filtered.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_reel),
                            contentDescription = "Empty film reel illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(220.dp)
                                .height(160.dp)
                                .clip(MaterialTheme.shapes.large)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "No community gems here yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Solve a half-remembered scene and toggle \"Share to Trending Gems\" to kick off the reel.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onGoToSearch,
                            modifier = Modifier.testTag("empty_trending_search_cta")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Decode a Scene")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filtered, key = { it.id }) { gem ->
                            val currentVote = userVotes[gem.id] ?: VoteChoice.NONE
                            val isVoting = votingMemoryIds.contains(gem.id)
                            CommunityGemCard(
                                gem = gem,
                                currentVote = currentVote,
                                isVoting = isVoting,
                                onVoteYes = { onVote(gem.id, VoteChoice.YES) },
                                onVoteNo = { onVote(gem.id, VoteChoice.NO) },
                                onShare = {
                                    shareDiscoveryText(
                                        context = context,
                                        queryText = gem.queryText,
                                        title = gem.title,
                                        year = gem.year.toInt(),
                                        why = gem.whyItMatched
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityGemCard(
    gem: CommunityMemory,
    currentVote: VoteChoice,
    isVoting: Boolean,
    onVoteYes: () -> Unit,
    onVoteNo: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("community_gem_${gem.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Remembered quote header
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REMEMBERED BY ${gem.authorName.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = onShare,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Share gem",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"${gem.queryText}\"",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                PosterArtwork(
                    posterUrl = gem.posterUrl,
                    title = gem.title,
                    year = gem.year.toInt(),
                    mediaType = gem.mediaEnum(),
                    isLoading = false,
                    modifier = Modifier.width(86.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = gem.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            append(gem.year)
                            if (gem.directorOrCreator.isNotBlank()) {
                                append(" • ")
                                append(gem.directorOrCreator)
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MetaTagPill(gem.mediaEnum().label)
                        MetaTagPill(gem.country)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = gem.whyItMatched,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Community "Was it this?" Voting Bar
            Text(
                text = "WAS IT THIS?",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val yesSelected = currentVote == VoteChoice.YES
                val noSelected = currentVote == VoteChoice.NO

                OutlinedButton(
                    onClick = onVoteYes,
                    enabled = !isVoting,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (yesSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        contentColor = if (yesSelected) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    ),
                    border = BorderStroke(
                        width = if (yesSelected) 1.4.dp else 1.dp,
                        color = if (yesSelected) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("vote_yes_${gem.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ThumbUp,
                        contentDescription = "Vote Yes, nailed it",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Yes! (${gem.yesCount})",
                        fontWeight = if (yesSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }

                OutlinedButton(
                    onClick = onVoteNo,
                    enabled = !isVoting,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (noSelected) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        contentColor = if (noSelected) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ),
                    border = BorderStroke(
                        width = if (noSelected) 1.4.dp else 1.dp,
                        color = if (noSelected) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("vote_no_${gem.id}")
                ) {
                    Icon(
                        imageVector = if (noSelected) Icons.Rounded.Close else Icons.Rounded.ThumbDown,
                        contentDescription = "Vote Not quite",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Not quite (${gem.noCount})",
                        fontWeight = if (noSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
