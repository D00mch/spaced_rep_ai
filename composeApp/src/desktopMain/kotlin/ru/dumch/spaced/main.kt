package ru.dumch.spaced

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diamondedge.logging.FixedLogLevel
import com.diamondedge.logging.KmLogging
import com.diamondedge.logging.LogLevel
import com.diamondedge.logging.PrintLogger
import org.kodein.di.compose.localDI
import org.kodein.di.compose.withDI
import ru.dumch.spaced.di.diDesktopModule
import ru.dumch.spaced.sync.SyncViewModel

fun main() {
    KmLogging.setLoggers(PrintLogger(FixedLogLevel(true)))
    KmLogging.setLogLevel(LogLevel.Verbose)
    application {
        withDI(diDesktopModule) {
            Window(
                onCloseRequest = ::exitApplication,
                title = "SpacedRepetitionAI",
            ) {
                val di = localDI()
                val viewModel = viewModel { SyncViewModel(di) }
                App()
            }
        }
    }
}