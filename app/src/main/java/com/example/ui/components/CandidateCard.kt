package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.SceneCandidate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CandidateCard(
    rank: Int,
    candidate: SceneCandidate,
    onSolveClick: () -> Unit,
    onToggleDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTopMatch = rank == 1 && !candidate.isDismissed

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (candidate.isDismissed) 0.48f else 1f)
            .testTag("candidate_card_$rank"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isTopMatch) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        border = BorderStroke(
            width = if (isTopMatch) 1.2.dp else 1.dp,
            color = if (isTopMatch) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                PosterArtwork(
                    posterUrl = candidate.posterUrl,
                    title = candidate.title,
                    year = candidate.year,
                    mediaType = candidate.mediaType,
                    isLoading = candidate.isPosterLoading,
                    modifier = Modifier.width(96.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (isTopMatch) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isTopMatch) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = "#$rank • ${candidate.confidencePercent}% MATCH",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isTopMatch) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        IconButton(
                            onClick = onToggleDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("dismiss_candidate_$rank")
                        ) {
                            Icon(
                                imageVector = if (candidate.isDismissed) Icons.Rounded.Restore else Icons.Rounded.Close,
                                contentDescription = if (candidate.isDismissed) "Restore candidate" else "Rule out candidate",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = candidate.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = buildString {
                            append(candidate.year)
                            if (candidate.directorOrCreator.isNotBlank()) {
                                append(" • ")
                                append(candidate.directorOrCreator)
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MetaTagPill(candidate.mediaType.label)
                        MetaTagPill(candidate.country)
                        MetaTagPill(candidate.style.label)
                        MetaTagPill(candidate.languageMode.label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "WHY IT MATCHED",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = candidate.whyItMatched,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (candidate.iconicCue.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FormatQuote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = candidate.iconicCue,
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!candidate.isDismissed) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onSolveClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("solve_candidate_$rank"),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BookmarkAdded,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "This is it! Mystery Solved",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun MetaTagPill(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
