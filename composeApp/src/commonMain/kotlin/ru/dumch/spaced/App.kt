package ru.dumch.spaced

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appstractive.dnssd.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.kodein.di.compose.localDI
import ru.dumch.spaced.sync.SyncEvent
import ru.dumch.spaced.sync.SyncSideEffect
import ru.dumch.spaced.sync.SyncViewModel
import ru.dumch.spaced.ui.AppTheme

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Preview
fun App() {
    val di = localDI()
    val viewModel = viewModel { SyncViewModel(di) }
    AppTheme {
        val state = viewModel.uiState.collectAsState().value
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        val snackBarHostState = remember { SnackbarHostState() }

        // Handle effects
        LaunchedEffect(Unit) {
            viewModel.effects
                .flowWithLifecycle(lifecycle)
                .collect { effect ->
                    val snackMsg = when (effect) {
                        is SyncSideEffect.RegisterConnected -> "Service registered successfully"
                        is SyncSideEffect.RegisterDisconnected -> "Service unregistered gracefully"
                        is SyncSideEffect.ServiceDiscovered -> "Another service (${effect.event.service.name}) is discovered"
                    }
                    snackBarHostState.showSnackbar(message = snackMsg, withDismissAction = true)
                }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackBarHostState) },
            content = { padding ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {
                    PrimaryTabRow(
                        selectedTabIndex = state.tabIdx,
                    ) {
                        Tab(
                            selected = state.tabIdx == 0,
                            onClick = { viewModel.send(SyncEvent.TabSelected(0)) },
                            text = { Text(text = "Advertise") }
                        )
                        Tab(
                            selected = state.tabIdx == 1,
                            onClick = { viewModel.send(SyncEvent.TabSelected(1)) },
                            text = { Text(text = "Scan") }
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        when (state.tabIdx) {
                            0 -> AdvertiseView(viewModel)
                            else -> ScanView(viewModel)
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun AdvertiseView(viewModel: SyncViewModel) {
    val state = viewModel.uiState.collectAsState().value

    Column {
        Text("Service Registration")

        ElevatedButton(
            onClick = {
                val ev = when {
                    state.registered -> SyncEvent.RegisterOff
                    else -> SyncEvent.RegisterOn
                }
                viewModel.send(ev)
            },
        ) {
            Text(if (state.registered) "Unregister" else "Register")
        }
    }
}

@Composable
private fun ScanView(viewModel: SyncViewModel) {
    val state = viewModel.uiState.collectAsState().value

    Column {
        Row {
            Text("Scan NSD Services")

            ElevatedButton(
                onClick = {
                    val event = if (state.isScanning) SyncEvent.ScanOff else SyncEvent.ScanOn
                    viewModel.send(event)
                },
            ) {
                Text(if (state.isScanning) "Stop" else "Start")
            }
        }

        LazyColumn {
            items(state.scannedServices) { service: DiscoveredService ->
                ListItem(
                    headlineContent = { Text("${service.name} (${service.host})") },
                    supportingContent = {
                        Column {
                            Text("${service.type}:${service.port} (${service.addresses})")
                            Text(service.txt.toList().joinToString { "${it.first}=${it.second?.decodeToString()}" })
                        }
                    },
                )
            }
        }
    }
}