package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.SceneCandidate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolveMysterySheet(
    candidate: SceneCandidate,
    queryText: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirmSolve: (note: String, shareToCommunity: Boolean) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var note by remember(candidate.candidateId) { mutableStateOf("") }
    var shareToCommunity by remember(candidate.candidateId) { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PosterArtwork(
                    posterUrl = candidate.posterUrl,
                    title = candidate.title,
                    year = candidate.year,
                    mediaType = candidate.mediaType,
                    isLoading = candidate.isPosterLoading,
                    modifier = Modifier.width(72.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MYSTERY SOLVED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = candidate.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${candidate.year} • ${candidate.mediaType.label} • ${candidate.country}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 500) note = it },
                label = { Text("Personal rewatch note (optional)") },
                placeholder = { Text("e.g. Watch with friends, scene starts around minute 42") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("solve_note_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Share to Trending Gems",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Let the community vote \"Was it this?\" and browse this gem",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = shareToCommunity,
                    onCheckedChange = { shareToCommunity = it },
                    modifier = Modifier.testTag("share_community_switch")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        shareDiscoveryText(
                            context = context,
                            queryText = queryText,
                            title = candidate.title,
                            year = candidate.year,
                            why = candidate.whyItMatched
                        )
                    },
                    modifier = Modifier.testTag("share_external_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Share outside app",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Post")
                }

                Button(
                    onClick = { onConfirmSolve(note, shareToCommunity) },
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("confirm_solve_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving…")
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.BookmarkAdded,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save to Vault")
                    }
                }
            }
        }
    }
}

fun shareDiscoveryText(
    context: Context,
    queryText: String,
    title: String,
    year: Int,
    why: String
) {
    val shareText = buildString {
        appendLine("I half-remembered: \"$queryText\"")
        appendLine("Frame Memory found it: $title ($year)")
        appendLine()
        appendLine(why)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Mystery Solved: $title ($year)")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share Frame Memory discovery"))
}
