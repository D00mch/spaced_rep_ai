package ru.dumch.spaced

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.kodein.di.instance
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.kodein.di.compose.localDI
import org.kodein.di.compose.withDI
import ru.dumch.spaced.ui.AppTheme
import spacedrepetitionai.composeapp.generated.resources.Res
import spacedrepetitionai.composeapp.generated.resources.compose_multiplatform

@Composable
@Preview
fun App() = withDI(mainDiModule) {
    AppTheme {
        var showContent: Boolean by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = { showContent = !showContent }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
                val di = localDI()
                val greeting: Greeting by di.instance()
                val greetTxt: String = remember { greeting.greet() }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greetTxt")
                }
            }
        }
    }
}