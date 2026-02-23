package dev.csse.pfnassar.daytripper.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DTApp(
    model: DTViewModel = viewModel<DTViewModel>()
) {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        TripListScreen(
            model = model,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        )
    }
}
