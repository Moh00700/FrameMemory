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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.MediaCategory
import com.example.model.SolvedMemory
import com.example.model.UiState
import com.example.ui.components.MetaTagPill
import com.example.ui.components.PosterArtwork
import com.example.ui.components.shareDiscoveryText

@Composable
fun MyVaultScreen(
    solvedState: UiState<List<SolvedMemory>>,
    vaultSearchQuery: String,
    vaultMediaFilter: MediaCategory?,
    userName: String,
    isSaving: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSelectMediaFilter: (MediaCategory?) -> Unit,
    onUpdateNote: (memoryId: String, note: String, onDone: () -> Unit) -> Unit,
    onShareToCommunity: (SolvedMemory) -> Unit,
    onDeleteMemory: (SolvedMemory) -> Unit,
    onSignOut: () -> Unit,
    onGoToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var editingMemory by remember { mutableStateOf<SolvedMemory?>(null) }
    var deletingMemory by remember { mutableStateOf<SolvedMemory?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        // Header + Sign Out
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mystery Solved Vault",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Signed in as $userName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = "Sign out",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sign out")
            }
        }

        when (solvedState) {
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
                            text = solvedState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            is UiState.Success -> {
                val allItems = solvedState.data
                if (allItems.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_reel),
                            contentDescription = "Empty film reel vault",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(220.dp)
                                .height(160.dp)
                                .clip(MaterialTheme.shapes.large)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Your Mystery Vault is empty",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Whenever you tap \"This is it!\" on a match, it’s archived here with your original memory and rewatch notes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onGoToSearch,
                            modifier = Modifier.testTag("empty_vault_search_cta")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Find a Film or Show")
                        }
                    }
                } else {
                    val filtered = allItems.filter { item ->
                        val matchesCat = vaultMediaFilter == null || item.mediaEnum() == vaultMediaFilter
                        val q = vaultSearchQuery.trim()
                        val matchesQuery = q.isBlank() ||
                            item.title.contains(q, ignoreCase = true) ||
                            item.queryText.contains(q, ignoreCase = true) ||
                            item.personalNote.contains(q, ignoreCase = true)
                        matchesCat && matchesQuery
                    }

                    // Stats & Search Strip
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            VaultStatPill(
                                label = "SOLVED",
                                value = allItems.size.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            VaultStatPill(
                                label = "SHARED",
                                value = allItems.count { it.sharedToCommunity }.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            VaultStatPill(
                                label = "ANIME / TV",
                                value = allItems.count { it.mediaEnum() != MediaCategory.MOVIE }.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = vaultSearchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Filter your solved titles or clues…") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = null)
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_search_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = vaultMediaFilter == null,
                                onClick = { onSelectMediaFilter(null) },
                                label = { Text("All (${allItems.size})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            MediaCategory.entries.forEach { cat ->
                                val count = allItems.count { it.mediaEnum() == cat }
                                FilterChip(
                                    selected = vaultMediaFilter == cat,
                                    onClick = {
                                        onSelectMediaFilter(if (vaultMediaFilter == cat) null else cat)
                                    },
                                    label = { Text("${cat.label} ($count)") }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.id }) { memory ->
                            VaultMemoryCard(
                                memory = memory,
                                isSaving = isSaving,
                                onEditNote = { editingMemory = memory },
                                onShareToCommunity = { onShareToCommunity(memory) },
                                onExternalShare = {
                                    shareDiscoveryText(
                                        context = context,
                                        queryText = memory.queryText,
                                        title = memory.title,
                                        year = memory.year.toInt(),
                                        why = memory.whyItMatched
                                    )
                                },
                                onDelete = { deletingMemory = memory }
                            )
                        }
                    }
                }
            }
        }
    }

    editingMemory?.let { mem ->
        var draftNote by remember(mem.id) { mutableStateOf(mem.personalNote) }
        AlertDialog(
            onDismissRequest = { editingMemory = null },
            title = { Text("Rewatch Note") },
            text = {
                OutlinedTextField(
                    value = draftNote,
                    onValueChange = { if (it.length <= 500) draftNote = it },
                    label = { Text(mem.title) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateNote(mem.id, draftNote) { editingMemory = null }
                    },
                    enabled = !isSaving
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMemory = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    deletingMemory?.let { mem ->
        AlertDialog(
            onDismissRequest = { deletingMemory = null },
            title = { Text("Remove from Vault?") },
            text = { Text("Remove \"${mem.title}\" from your personal Mystery Solved list?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMemory(mem)
                        deletingMemory = null
                    },
                    enabled = !isSaving
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingMemory = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun VaultStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VaultMemoryCard(
    memory: SolvedMemory,
    isSaving: Boolean,
    onEditNote: () -> Unit,
    onShareToCommunity: () -> Unit,
    onExternalShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vault_card_${memory.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                PosterArtwork(
                    posterUrl = memory.posterUrl,
                    title = memory.title,
                    year = memory.year.toInt(),
                    mediaType = memory.mediaEnum(),
                    isLoading = false,
                    modifier = Modifier.width(80.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.BookmarkAdded,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SOLVED",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Row {
                            IconButton(
                                onClick = onExternalShare,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "Share",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Remove from vault",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = memory.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${memory.year} • ${memory.country}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MetaTagPill(memory.mediaEnum().label)
                        MetaTagPill(memory.languageEnum().label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Memory: \"${memory.queryText}\"",
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (memory.personalNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Note: ${memory.personalNote}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEditNote,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (memory.personalNote.isBlank()) "Add Note" else "Edit Note")
                }

                if (!memory.sharedToCommunity) {
                    OutlinedButton(
                        onClick = onShareToCommunity,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Public,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Gem")
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "In Trending",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
