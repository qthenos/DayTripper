package dev.csse.pfnassar.daytripper.ui

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.mapbox.search.ResponseInfo
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchEngineSettings
import com.mapbox.search.SearchOptions
import com.mapbox.search.SearchSelectionCallback
import com.mapbox.search.SearchSuggestionsCallback
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import dev.csse.pfnassar.daytripper.Route
import dev.csse.pfnassar.daytripper.data.MapboxDirectionsRepository
import dev.csse.pfnassar.daytripper.data.RouteRepository
import dev.csse.pfnassar.daytripper.lastRouteId
import kotlinx.coroutines.launch
import java.time.LocalTime

class DTViewModel(
    private val routeRepository: RouteRepository? = null
) : ViewModel() {
    val routeList = mutableStateListOf<Route>()
    private val directionsRepository = MapboxDirectionsRepository()

    // ── Mapbox Search ────────────────────────────────────────────
    // Lazy so it's only created after MapboxOptions / access-token init
    private val searchEngine by lazy {
        SearchEngine.createSearchEngineWithBuiltInDataProviders(
            SearchEngineSettings()
        )
    }

    /** Suggestions for the currently active search field. */
    var searchSuggestions by mutableStateOf<List<SearchSuggestion>>(emptyList())
        private set

    /** The route ID whose location field is actively being searched. */
    var activeSearchRouteId by mutableStateOf<Int?>(null)
        private set

    var isSearching by mutableStateOf(false)
        private set

    private var searchRequestId = 0L

    /**
     * Forward-geocode [query] and populate [searchSuggestions].
     * Each call cancels any in-flight request for a previous query.
     */
    fun searchLocation(query: String, routeId: Int) {
        if (query.isBlank()) {
            clearSearch()
            return
        }

        activeSearchRouteId = routeId
        isSearching = true
        val currentRequestId = ++searchRequestId

        searchEngine.search(
            query,
            SearchOptions(limit = 5),
            object : SearchSuggestionsCallback {
                override fun onSuggestions(
                    suggestions: List<SearchSuggestion>,
                    responseInfo: ResponseInfo
                ) {
                    if (currentRequestId == searchRequestId) {
                        searchSuggestions = suggestions
                        isSearching = false
                    }
                }

                override fun onError(e: Exception) {
                    if (currentRequestId == searchRequestId) {
                        Log.e("DTViewModel", "Search error", e)
                        searchSuggestions = emptyList()
                        isSearching = false
                    }
                }
            }
        )
    }

    /**
     * Resolve a search suggestion into a full [SearchResult] with coordinates
     * and store it in the route identified by [routeId].
     */
    fun selectSuggestion(suggestion: SearchSuggestion, routeId: Int) {
        searchEngine.select(
            suggestion,
            object : SearchSelectionCallback {
                override fun onSuggestions(
                    suggestions: List<SearchSuggestion>,
                    responseInfo: ResponseInfo
                ) {
                    // Intermediate suggestions — not used during selection
                }

                override fun onResult(
                    suggestion: SearchSuggestion,
                    result: SearchResult,
                    responseInfo: ResponseInfo
                ) {
                    applySearchResult(routeId, result)
                    clearSearch()
                }

                override fun onResults(
                    suggestion: SearchSuggestion,
                    results: List<SearchResult>,
                    responseInfo: ResponseInfo
                ) {
                    results.firstOrNull()?.let { applySearchResult(routeId, it) }
                    clearSearch()
                }

                override fun onError(e: Exception) {
                    Log.e("DTViewModel", "Selection error", e)
                    clearSearch()
                }
            }
        )
    }

    private fun applySearchResult(routeId: Int, result: SearchResult) {
        val index = routeList.indexOfFirst { it.id == routeId }
        if (index >= 0) {
            val updatedRoute = routeList[index].copy(
                location = result.name,
                coordinate = result.coordinate
            )

            if (routeRepository == null) {
                routeList[index] = updatedRoute
                return
            }

            viewModelScope.launch {
                routeRepository.updateRoute(updatedRoute)
            }
        }
    }

    fun clearSearch() {
        searchSuggestions = emptyList()
        activeSearchRouteId = null
        isSearching = false
    }

    /** Clear a route's geocoded location (e.g. when the user edits the text). */
    fun clearRouteLocation(routeId: Int) {
        val index = routeList.indexOfFirst { it.id == routeId }
        if (index >= 0) {
            val updatedRoute = routeList[index].copy(location = "", coordinate = null)

            if (routeRepository == null) {
                routeList[index] = updatedRoute
                return
            }

            viewModelScope.launch {
                routeRepository.updateRoute(updatedRoute)
            }
        }
    }

    // ── Existing route helpers ───────────────────────────────────

    init {
        routeRepository?.let { repository ->
            viewModelScope.launch {
                repository.routes.collect { routes ->
                    routeList.clear()
                    routeList.addAll(routes)
                    // Sync lastRouteId with max ID in database
                    lastRouteId = (routes.maxOfOrNull { it.id } ?: -1) + 1
                }
            }
        }
    }

    fun findRouteById(id: String): Route? {
        return routeList.find { it.id.toString() == id }
    }

    fun addRoute(route: Route) {
        if (routeRepository == null) {
            routeList.add(route)
            return
        }

        viewModelScope.launch {
            routeRepository.addRoute(route)
        }
    }

    fun deleteRoute(route: Route) {
        if (routeRepository == null) {
            routeList.remove(route)
            return
        }

        viewModelScope.launch {
            routeRepository.deleteRoute(route)
        }
    }

    val routeCount: Int
        get() = routeList.count()

    val emptyRouteExists: Boolean
        get() = routeList.any {
            it.location.isEmpty() || it.coordinate == null || it.time == null
        }


    @RequiresApi(Build.VERSION_CODES.O)
    fun createTestRoutes(count: Int) {
        for (i in 1..count) {
            addRoute(
                Route(
                    location = "Test Location $i",
                    time = LocalTime.now().plusHours(i.toLong())
                )
            )
        }
    }

    val completedRoutesExist: Boolean
        get() = routeList.count { it.completed } > 0

    fun deleteCompletedRoutes() {
        if (routeRepository == null) {
            routeList.removeIf { it.completed }
            return
        }

        viewModelScope.launch {
            routeRepository.deleteCompletedRoutes()
        }
    }


    fun toggleRouteCompleted(route: Route): Route {
        val newRoute = route
            .copy(completed = !route.completed)

        if (routeRepository == null) {
            val index = routeList.indexOf(route)
            if (index >= 0) {
                routeList[index] = newRoute
            }
            return newRoute
        }

        viewModelScope.launch {
            routeRepository.updateRoute(newRoute)
        }
        return newRoute
    }

    fun setRouteCompleted(routeId: Int, completed: Boolean): Route? {
        val index = routeList.indexOfFirst { it.id == routeId }
        if (index < 0) return null

        val updatedRoute = routeList[index].copy(completed = completed)

        if (routeRepository == null) {
            routeList[index] = updatedRoute
            return updatedRoute
        }

        viewModelScope.launch {
            routeRepository.updateRoute(updatedRoute)
        }
        return updatedRoute
    }


    fun setTime(route: Route, time: LocalTime): Route {
        val newRoute = route
            .copy(time = time)

        if (routeRepository == null) {
            val index = routeList.indexOf(route)
            if (index >= 0) {
                routeList[index] = newRoute
            }
            return newRoute
        }

        viewModelScope.launch {
            routeRepository.updateRoute(newRoute)
        }
        return newRoute
    }

    fun setLocation(route: Route, location: String) {
        val updatedRoute = route.copy(location = location)

        if (routeRepository == null) {
            val index = routeList.indexOf(route)
            if (index >= 0) {
                routeList[index] = updatedRoute
            }
            return
        }

        viewModelScope.launch {
            routeRepository.updateRoute(updatedRoute)
        }
    }

    fun resetTrip() {
        clearSearch()
        lastRouteId = 0

        if (routeRepository == null) {
            routeList.clear()
            return
        }

        viewModelScope.launch {
            routeRepository.resetTrip()
        }
    }

    suspend fun fetchWalkingDirectionsRoute(
        accessToken: String,
        origin: Point,
        destination: Point
    ): List<Point>? {
        return directionsRepository.fetchWalkingRoute(
            accessToken = accessToken,
            origin = origin,
            destination = destination
        )
    }

}