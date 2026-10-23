package com.example.weatherapp.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatherapp.ui.theme.WeatherAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherAppTheme {
                val vm: WeatherViewModel = viewModel()
                val state by vm.state.collectAsStateWithLifecycle()

                // Permission launcher: on grant, load by location; on denial, VM already
                // fell back to the last city during onStart(), so nothing else to do.
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { result ->
                    val granted = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                        result[Manifest.permission.ACCESS_FINE_LOCATION] == true
                    if (granted) vm.loadByLocation()
                }

                // Runs once per composition entry: ask for location (if not already granted)
                // and let the VM resolve initial content (location → else last city).
                // On first composition: resolve initial content (location if already granted,
                // else last city), then request permission ONLY if not already granted — avoids
                // a redundant second location load when permission was granted previously.
                LaunchedEffect(Unit) {
                    vm.onStart()
                    if (!vm.hasLocationPermission()) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                            ),
                        )
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WeatherScreen(
                        state = state,
                        onQueryChange = vm::onQueryChange,
                        onSearch = vm::search,
                        onUseLocation = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                ),
                            )
                        },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
