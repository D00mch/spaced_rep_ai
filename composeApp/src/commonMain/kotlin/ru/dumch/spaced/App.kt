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
import com.appstractive.dnssd.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.kodein.di.compose.localDI
import org.kodein.di.instance
import ru.dumch.spaced.sync.SyncCommon.SERVICE_TYPE
import ru.dumch.spaced.ui.AppTheme

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Preview
fun App() {
    val di = localDI()
    val service: NetService by di.instance()
    AppTheme {
        var selectedTab by remember { mutableStateOf(0) }

        Scaffold { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(text = "Advertise") }
                    )

                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(text = "Scan") }
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    when (selectedTab) {
                        0 -> AdvertiseView(service)
                        else -> ScanView()
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvertiseView(service: NetService) {
    val scope = rememberCoroutineScope()
    val isRegistered by service.isRegistered.collectAsState()

    Column {
        Text("Service Registration")

        ElevatedButton(
            onClick = {
                scope.launch {
                    if (isRegistered) {
                        service.unregister()
                    } else {
                        service.register()
                    }
                }
            },
        ) {
            Text(if (isRegistered) "Stop" else "Start")
        }
    }
}

@Composable
private fun ScanView() {
    val scope = rememberCoroutineScope()
    val scannedServices = remember { mutableStateMapOf<String, DiscoveredService>() }
    var scanJob: Job? by remember { mutableStateOf(null) }

    Column {
        Row {
            Text("Scan NSD Services")

            ElevatedButton(
                onClick = {
                    if (scanJob == null) {
                        scannedServices.clear()
                        scanJob = scope.launch(Dispatchers.IO) {
                            discoverServices(SERVICE_TYPE).collect {
                                when (it) {
                                    is DiscoveryEvent.Discovered -> {
                                        scannedServices[it.service.key] = it.service
                                        it.resolve()
                                    }

                                    is DiscoveryEvent.Removed -> {
                                        scannedServices.remove(it.service.key)
                                    }

                                    is DiscoveryEvent.Resolved -> {
                                        scannedServices[it.service.key] = it.service
                                    }
                                }
                            }
                        }
                    } else {
                        scanJob?.cancel()
                    }
                },
            ) {
                Text(if (scanJob != null) "Stop" else "Start")
            }
        }

        LazyColumn {
            items(scannedServices.values.toList()) { service: DiscoveredService ->
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