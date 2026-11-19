package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.CountryFilterOption
import com.example.model.DecadeFilterOption
import com.example.model.LanguageFilterOption
import com.example.model.MediaFilterOption
import com.example.model.MemorySearchFilters
import com.example.model.StyleFilterOption

@Composable
fun NarrowDownFilterSection(
    filters: MemorySearchFilters,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onSelectMedia: (MediaFilterOption) -> Unit,
    onSelectDecade: (DecadeFilterOption) -> Unit,
    onSelectCountry: (CountryFilterOption) -> Unit,
    onSelectStyle: (StyleFilterOption) -> Unit,
    onSelectLanguage: (LanguageFilterOption) -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCount = filters.activeCount()

    Column(modifier = modifier.fillMaxWidth()) {
        // Always-visible Format Row + Toggle
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
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (activeCount > 0) "Narrow it down ($activeCount active)" else "Narrow it down",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (activeCount > 0) {
                    TextButton(
                        onClick = onResetFilters,
                        modifier = Modifier.testTag("reset_filters_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear filters",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Clear")
                    }
                }
                TextButton(
                    onClick = onToggleExpanded,
                    modifier = Modifier.testTag("toggle_filters_button")
                ) {
                    Text(if (expanded) "Hide clues" else "More clues")
                }
            }
        }

        // Media Type Strip (Always reachable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MediaFilterOption.entries.forEach { option ->
                val selected = filters.media == option
                CinemaFilterChip(
                    label = option.label,
                    selected = selected,
                    onClick = { onSelectMedia(option) },
                    testTag = "filter_media_${option.name.lowercase()}"
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterRowGroup(
                        title = "Decade",
                        items = DecadeFilterOption.entries,
                        selectedItem = filters.decade,
                        labelOf = { it.label },
                        onSelect = onSelectDecade
                    )
                    FilterRowGroup(
                        title = "Country / Region",
                        items = CountryFilterOption.entries,
                        selectedItem = filters.country,
                        labelOf = { it.label },
                        onSelect = onSelectCountry
                    )
                    FilterRowGroup(
                        title = "Visual Style",
                        items = StyleFilterOption.entries,
                        selectedItem = filters.style,
                        labelOf = { it.label },
                        onSelect = onSelectStyle
                    )
                    FilterRowGroup(
                        title = "Subtitled / Dubbed",
                        items = LanguageFilterOption.entries,
                        selectedItem = filters.language,
                        labelOf = { it.label },
                        onSelect = onSelectLanguage
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> FilterRowGroup(
    title: String,
    items: List<T>,
    selectedItem: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                CinemaFilterChip(
                    label = labelOf(item),
                    selected = item == selectedItem,
                    onClick = { onSelect(item) }
                )
            }
        }
    }
}

@Composable
private fun CinemaFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        ),
        modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
    )
}
