# DayTripper

DayTripper is an Android trip-planning app built with Jetpack Compose that helps users create a list of stops, assign times, and navigate each stop on an interactive map. The app uses Mapbox for place search, map rendering, and walking directions, then tracks stop completion as you move through the day. It also persists your trip state locally using Room so your plan remains available across app restarts.

## Project Links

- GitHub: https://github.com/qthenos/DayTripper
- Contact: pfnassar@calpoly.edu
- Figma Make design link: https://www.figma.com/make/QL17Gg1TF4RKy43p6NQFlU/Travel-Aid-App?t=a071wr72BNeRpBMp-1

## Design

The current design source is hosted in Figma (link above).

## Android + Jetpack Compose Features Used

- Jetpack Compose UI throughout the app
- Material 3 components and theming
- Compose Navigation (`NavHost`, typed destinations)
- State-driven UI with `remember`, `rememberSaveable`, and `mutableStateOf`
- ViewModel architecture (`DTViewModel`) with a custom `ViewModelProvider.Factory`
- Coroutines for asynchronous work (search requests, route fetching, database calls)
- Runtime location permissions with `ActivityResultContracts.RequestMultiplePermissions`
- Edge-to-edge display in `MainActivity`
- Adaptive/staggered grid layout for stop cards

## Third-Party Libraries and Key Integrations

- Mapbox Maps SDK + Compose extension
	- Interactive map, camera control, location puck, markers, and route polyline drawing
- Mapbox Search SDK
	- Debounced location autocomplete and suggestion resolution to coordinates
- Mapbox Directions API (HTTP call)
	- Walking-route geometry between current location and selected stop
- Room (AndroidX) + KSP
	- Local persistence for stops/routes with DAO, repository, and type converters

## Device/Platform Requirements and Setup Notes

- Android SDK levels:
	- `minSdk = 24`
	- `targetSdk = 36`
	- `compileSdk = 36`
- Java/Kotlin toolchain:
	- Java 11 compatibility
	- Kotlin 2.3.0
- Required device capabilities/permissions:
	- Internet access (for Mapbox APIs)
	- Location permission (`ACCESS_COARSE_LOCATION` and/or `ACCESS_FINE_LOCATION`) for current-position navigation
- Build/runtime token requirements:
	- A Mapbox Downloads token in `gradle.properties` as `MAPBOX_DOWNLOADS_TOKEN` is required to resolve Mapbox artifacts from the Mapbox Maven repository.
	- A Mapbox public access token is required by the app resource `mapbox_access_token`.

## Running the App

1. Open the project in Android Studio.
2. Ensure Gradle sync completes (Mapbox repo credentials must be valid).
3. Verify Mapbox tokens are configured for your local environment.
4. Run on an emulator/device with Android 7.0+ (API 24+) and grant location permission when prompted.

## Notable Functionality / Above and Beyond

- Debounced search suggestions per stop with explicit selection-to-coordinate mapping
- Turn-by-turn style trip progression flow (select stop, route to it, mark as arrived)
- Dynamic route-line rendering on the map as directions are fetched

## Project Structure (High Level)

- `app/src/main/java/.../ui`: Compose screens/components (`TripListScreen`, `MapScreen`, search field, app shell)
- `app/src/main/java/.../data`: Directions + persistence repositories
- `app/src/main/java/.../data/db`: Room entities, DAO, converters, and database singleton

