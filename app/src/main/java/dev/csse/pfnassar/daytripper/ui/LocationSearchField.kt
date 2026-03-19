package dev.csse.pfnassar.daytripper.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapbox.search.result.SearchSuggestion
import kotlinx.coroutines.delay

/**
 * A text field that geocodes as the user types, showing a dropdown of
 * [SearchSuggestion] results the user can tap to resolve into coordinates.
 *
 * @param routeId           ID of the route this field belongs to.
 * @param locationName      Currently stored location display-name (from the Route).
 * @param hasCoordinate     Whether the route already has a resolved coordinate.
 * @param suggestions       Live list of search suggestions from the ViewModel.
 * @param activeSearchRouteId Which route is currently being searched (null = none).
 * @param onSearch          Called (debounced) when the user types a new query.
 * @param onSuggestionSelected Called when the user taps a suggestion.
 * @param onLocationCleared Called when the user edits text after having already
 *                          selected a geocoded result, to invalidate the old coordinate.
 */
@Composable
fun LocationSearchField(
    routeId: Int,
    locationName: String,
    hasCoordinate: Boolean,
    suggestions: List<SearchSuggestion>,
    activeSearchRouteId: Int?,
    onSearch: (String) -> Unit,
    onSuggestionSelected: (SearchSuggestion) -> Unit,
    onLocationCleared: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Local text for the field — initialised from the stored name.
    var queryText by remember(routeId) { mutableStateOf(locationName) }

    // Sync queryText when locationName is set externally (after suggestion selection).
    LaunchedEffect(locationName) {
        if (locationName.isNotEmpty()) {
            queryText = locationName
        }
    }

    // Debounce: wait 300 ms after the user stops typing before searching.
    LaunchedEffect(queryText) {
        if (queryText.isNotBlank() && queryText != locationName) {
            delay(300L)
            onSearch(queryText)
        }
    }

    val showSuggestions = activeSearchRouteId == routeId && suggestions.isNotEmpty()

    Column(modifier) {
        TextField(
            value = queryText,
            onValueChange = { newText ->
                queryText = newText
                // If the user edits after selecting a geocoded result, clear it
                if (hasCoordinate && newText != locationName) {
                    onLocationCleared()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search for a place…") },
            leadingIcon = {
                Icon(Icons.Filled.LocationOn, contentDescription = "Destination")
            },
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            supportingText = if (hasCoordinate) {
                {
                    Text(
                        text = "✓ Location set",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else null
        )

        // ── Suggestion dropdown ──────────────────────────────────
        if (showSuggestions) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                suggestions.forEachIndexed { index, suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSuggestionSelected(suggestion) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = suggestion.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            suggestion.descriptionText?.let { desc ->
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (index < suggestions.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
