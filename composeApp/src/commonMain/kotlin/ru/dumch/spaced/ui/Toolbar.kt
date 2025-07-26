package ru.dumch.spaced.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Toolbar(onClick: () -> Unit,) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(8.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Button(onClick = onClick) {
            Text("Close")
        }
        // Add minimize/fullscreen if needed
    }
}