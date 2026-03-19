package dev.csse.pfnassar.daytripper.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapbox.search.result.SearchSuggestion
import dev.csse.pfnassar.daytripper.ui.theme.AppTheme
import dev.csse.pfnassar.daytripper.Route
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


@Composable
fun TripListScreen(
    modifier: Modifier = Modifier,
    model: DTViewModel = viewModel<DTViewModel>()
) {

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(300.dp),
        modifier = modifier
    ) {
        item {
            StyledCard(
                modifier = Modifier
                    .padding(top = 16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "Day Trip Planner",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        itemsIndexed(items = model.routeList, key = { _, route -> route.id }) { index, route ->
            RouteCard(
                route = route,
                index = index,
                suggestions = model.searchSuggestions,
                activeSearchRouteId = model.activeSearchRouteId,
                onSearch = { query -> model.searchLocation(query, route.id) },
                onSuggestionSelected = { suggestion ->
                    model.selectSuggestion(suggestion, route.id)
                },
                onLocationCleared = { model.clearRouteLocation(route.id) },
                onTimeChange = { newTime ->
                    model.setTime(route, newTime) },
                onDelete = {
                    model.deleteRoute(route)
                }
            )
        }
        item {
            StyledCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { model.addRoute(Route()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Stop")
                }
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteCard(
    route: Route,
    index: Int,
    modifier: Modifier = Modifier,
    suggestions: List<SearchSuggestion> = emptyList(),
    activeSearchRouteId: Int? = null,
    onSearch: (String) -> Unit = {},
    onSuggestionSelected: (SearchSuggestion) -> Unit = {},
    onLocationCleared: () -> Unit = {},
    onTimeChange: (LocalTime) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    StyledCard (
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ){
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = (index + 1).toString(),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .weight(1f)
            ) {
                Text(
                    text = "Destination",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                LocationSearchField(
                    routeId = route.id,
                    locationName = route.location,
                    hasCoordinate = route.coordinate != null,
                    suggestions = suggestions,
                    activeSearchRouteId = activeSearchRouteId,
                    onSearch = onSearch,
                    onSuggestionSelected = onSuggestionSelected,
                    onLocationCleared = onLocationCleared
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Planned Time",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(4.dp))

                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                var timeDigits by remember(route.id) {
                    mutableStateOf(route.time?.format(timeFormatter)?.filter { it.isDigit() } ?: "")
                }
                var timeError by remember(route.id) { mutableStateOf(false) }

                LaunchedEffect(route.time) {
                    timeDigits = route.time?.format(timeFormatter)?.filter { it.isDigit() } ?: ""
                }

                TextField(
                    value = toMaskedTime(timeDigits), // always shows like --:--
                    onValueChange = { typed ->
                        val sanitized = sanitize24hDigits(typed.filter { it.isDigit() })
                        if (sanitized == null) return@TextField

                        timeDigits = sanitized
                        if (sanitized.length == 4) {
                            val hh = sanitized.take(2).toInt()
                            val mm = sanitized.substring(2, 4).toInt()
                            onTimeChange(LocalTime.of(hh, mm))
                            timeError = false
                        } else {
                            timeError = false
                        }
                    },
                    placeholder = { Text("--:--") },
                    leadingIcon = {
                        Icon(Icons.Filled.Schedule, contentDescription = "Planned Time")
                    },
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    isError = timeError,
                    supportingText = {
                        if (timeError) Text("Enter time as HH:mm")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete route",
                )
            }
        }
    }
}

@Composable
fun StyledCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        content()
    }
}


private fun toMaskedTime(digits: String): String {
    val mask = charArrayOf('-', '-', ':', '-', '-')
    for ((i, c) in digits.withIndex()) {
        when (i) {
            0 -> mask[0] = c
            1 -> mask[1] = c
            2 -> mask[3] = c
            3 -> mask[4] = c
        }
    }
    return String(mask)
}

private fun sanitize24hDigits(raw: String): String? {
    if (raw.length > 4) return null
    if (raw.isEmpty()) return ""

    if (raw.isNotEmpty()) {
        val h1 = raw[0].digitToInt()
        if (h1 !in 0..2) return null
    }
    if (raw.length >= 2) {
        val hh = raw.take(2).toIntOrNull() ?: return null
        if (hh !in 0..23) return null
    }
    if (raw.length >= 3) {
        val m1 = raw[2].digitToInt()
        if (m1 !in 0..5) return null
    }

    return raw
}



@Preview(showBackground = true)
@Composable
fun TripListScreenSmallPreview() {
    val model = DTViewModel()
    model.createTestRoutes(1)
    AppTheme {
        TripListScreen(
            modifier = Modifier
                .height(800.dp)
                .width(450.dp),
            model = model
        )
    }
    model.addRoute(Route())
}

@Preview(showBackground = true)
@Composable
fun TripListScreenPreview() {
    val model = DTViewModel()
    model.createTestRoutes(5)
    AppTheme {
        TripListScreen(
            modifier = Modifier
                .height(800.dp)
                .width(450.dp),
            model = model
        )
    }
}

@Preview(
    showBackground = true,
    heightDp = 450,
    widthDp = 800
)
@Composable
fun TripListScreenLandscapePreview() {
    val model = DTViewModel()
    model.createTestRoutes(5)
    AppTheme {
        TripListScreen(
            modifier = Modifier
                .height(450.dp)
                .width(800.dp),
            model = model
        )
    }
}