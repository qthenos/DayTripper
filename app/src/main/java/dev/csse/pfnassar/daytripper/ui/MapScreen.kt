package dev.csse.pfnassar.daytripper.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.attribution.Attribution
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.scalebar.ScaleBar
import dev.csse.pfnassar.daytripper.R
import dev.csse.pfnassar.daytripper.Route
import dev.csse.pfnassar.daytripper.ui.StyledCard
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    model: DTViewModel = viewModel<DTViewModel>(),
    onTripListClick: () -> Unit = {}
) {
    val context = LocalContext.current

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    var locationGranted by remember { mutableStateOf(hasLocationPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        locationGranted =
            (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                    (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
    }

    LaunchedEffect(Unit) {
        if (!locationGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val routePoints = model.routeList.filter { it.coordinate != null }
    val accessToken = stringResource(R.string.mapbox_access_token)
    val coroutineScope = rememberCoroutineScope()

    var selectedStopId by rememberSaveable { mutableStateOf<Int?>(null) }
    var activeNavigationStopId by rememberSaveable { mutableStateOf<Int?>(null) }
    var userLocation by remember { mutableStateOf<Point?>(null) }
    var routeGeometry by remember { mutableStateOf<List<Point>>(emptyList()) }
    var isDirectionsLoading by remember { mutableStateOf(false) }
    var directionsError by remember { mutableStateOf<String?>(null) }
    var mapViewRef by remember { mutableStateOf<com.mapbox.maps.MapView?>(null) }
    var routeLineManager by remember { mutableStateOf<PolylineAnnotationManager?>(null) }

    val selectedStop = routePoints.firstOrNull { it.id == selectedStopId }
    val selectedIndex = routePoints.indexOfFirst { it.id == selectedStopId }
    val nextStopId = routePoints.firstOrNull { !it.completed }?.id
    val remainingStops = routePoints.count { !it.completed }

    val viewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(14.0)
            center(
                routePoints.firstOrNull()?.coordinate
                    ?: Point.fromLngLat(-98.0, 39.5)
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = viewportState,
            scaleBar = {
                ScaleBar(Modifier.padding(top = 60.dp))
            },
            logo = {
                Logo(Modifier.padding(bottom = 40.dp))
            },
            attribution = {
                Attribution(Modifier.padding(bottom = 40.dp))
            }
        ) {
            MapEffect(locationGranted) { mapView ->
                mapViewRef = mapView
                mapView.location.updateSettings {
                    enabled = locationGranted
                    locationPuck = createDefault2DPuck(withBearing = true)
                    puckBearingEnabled = true
                    puckBearing = PuckBearing.HEADING
                }
                mapView.location.addOnIndicatorPositionChangedListener { point ->
                    userLocation = point
                }
                if (locationGranted && routePoints.isEmpty()) {
                    viewportState.transitionToFollowPuckState()
                }
            }

            MapEffect(routeGeometry) { mapView ->
                val manager = routeLineManager ?: mapView.annotations
                    .createPolylineAnnotationManager()
                    .also { routeLineManager = it }

                manager.deleteAll()

                if (routeGeometry.size >= 2) {
                    manager.create(
                        PolylineAnnotationOptions()
                            .withPoints(routeGeometry)
                            .withLineColor("#0B8F3A")
                            .withLineWidth(6.0)
                    )
                }
            }

            routePoints.forEach { route ->
                val point = route.coordinate ?: return@forEach
                val isSelected = route.id == selectedStopId
                CircleAnnotation(
                    point = point,
                    onClick = {
                        selectedStopId = route.id
                        true
                    }
                ) {
                    circleRadius = if (isSelected) 12.0 else 10.0
                    circleColor = if (isSelected) androidx.compose.ui.graphics.Color(0xFF0B8F3A)
                        else androidx.compose.ui.graphics.Color(0xFF4264FB)
                    circleStrokeWidth = 2.0
                    circleStrokeColor = androidx.compose.ui.graphics.Color.White
                }
            }

        }


        StyledCard(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onTripListClick) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Back to trip list"
                    )
                }

                IconButton(
                    enabled = routePoints.isNotEmpty() && selectedIndex > 0,
                    onClick = {
                        if (selectedIndex > 0) {
                            selectedStopId = routePoints[selectedIndex - 1].id
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.NavigateBefore,
                        contentDescription = "Previous stop"
                    )
                }

                IconButton(
                    enabled = routePoints.isNotEmpty() && selectedIndex in 0 until routePoints.lastIndex,
                    onClick = {
                        if (selectedIndex in 0 until routePoints.lastIndex) {
                            selectedStopId = routePoints[selectedIndex + 1].id
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.NavigateNext,
                        contentDescription = "Next stop"
                    )
                }
            }
        }


        StyledCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            if (selectedStop == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stops remaining: $remainingStops",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )

                    if (remainingStops == 0 && routePoints.isNotEmpty()) {
                        Button(
                            onClick = {
                                model.resetTrip()
                                selectedStopId = null
                                activeNavigationStopId = null
                                routeGeometry = emptyList()
                                directionsError = null
                                onTripListClick()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text("New Trip!")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Filled.Add, contentDescription = null)
                        }
                    }
                }
            } else {
                val timeText = selectedStop.time
                    ?.format(DateTimeFormatter.ofPattern("HH:mm"))
                    ?: "No time"
                val isPathingThisStop = activeNavigationStopId == selectedStop.id
                val startEnabled = !isPathingThisStop && selectedStop.id == nextStopId && userLocation != null && !isDirectionsLoading

                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Stop #: ${selectedStop.id}",
                            style = MaterialTheme.typography.titleLarge
                        )

                        IconButton(onClick = { selectedStopId = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = androidx.compose.ui.graphics.Color.Red
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        StyledCard(
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ){
                                Text(
                                    text = "Destination:",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = selectedStop.location,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        StyledCard(
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ){
                                Text(
                                    text = "Scheduled:",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = timeText,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    if (isPathingThisStop) {
                        Button(
                            onClick = {
                                model.setRouteCompleted(selectedStop.id, true)
                                activeNavigationStopId = null
                                routeGeometry = emptyList()
                                directionsError = null
                                selectedStopId = model.routeList
                                    .filter { it.coordinate != null }
                                    .firstOrNull { !it.completed }
                                    ?.id
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text("Arrived")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Filled.Celebration, contentDescription = null)
                        }
                    } else {
                        Button(
                            onClick = {
                                val origin = userLocation
                                val destination = selectedStop.coordinate
                                if (origin == null || destination == null) {
                                    directionsError = "Current location unavailable."
                                    return@Button
                                }

                                coroutineScope.launch {
                                    isDirectionsLoading = true
                                    directionsError = null

                                    val geometry = model.fetchWalkingDirectionsRoute(
                                        accessToken = accessToken,
                                        origin = origin,
                                        destination = destination
                                    )

                                    isDirectionsLoading = false

                                    if (geometry.isNullOrEmpty()) {
                                        directionsError = "Could not build a route to this stop."
                                        return@launch
                                    }

                                    routeGeometry = geometry
                                    activeNavigationStopId = selectedStop.id

                                    mapViewRef?.mapboxMap?.setCamera(
                                        CameraOptions.Builder()
                                            .center(origin)
                                            .zoom(14.5)
                                            .build()
                                    )
                                }
                            },
                            enabled = startEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(if (isDirectionsLoading) "Routing..." else "Start Navigation")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Filled.Navigation, contentDescription = null)
                        }
                    }

                    if (!selectedStop.completed && selectedStop.id != nextStopId && !isPathingThisStop ) {
                        Text(
                            text = "Finish earlier stops before starting this one.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    directionsError?.let { errorText ->
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color(0xFFB00020),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
