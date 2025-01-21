package app.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.db.DBConnection
import app.isKnown

private const val UNEXPECTED_ERROR = "Hmm, that didn't work."

@Composable
fun errorScreen(exception: Throwable) {
    var startOverPressed by remember { mutableStateOf(false) }

    when {
        startOverPressed -> homeScreen()

        else -> errorScreen(
            error = exception,
            onStartOverPressed = { startOverPressed = it })
    }
}

@Composable
private fun errorScreen(error: Throwable, onStartOverPressed: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.background(MaterialTheme.colors.background).fillMaxSize()
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                modifier = Modifier.size(50.dp),
                imageVector = Icons.Rounded.SentimentDissatisfied, contentDescription = "",
                tint = Color(0xFF226600)
            )
            Text(
                modifier = Modifier.padding(top = 30.dp),
                text = error.resolveErrorMessage(),
                color = MaterialTheme.colors.primary
            )
            Button(modifier = Modifier.padding(top = 30.dp),
                onClick = { onStartOverPressed(true) }) {
                Text("Start Over")
            }
        }
    }
}

private fun Throwable.resolveErrorMessage() = when {
    isKnown() -> message ?: UNEXPECTED_ERROR
    else -> UNEXPECTED_ERROR
}

@Preview
@Composable
private fun previewErrorScreen() {
    errorScreen(error = RuntimeException("111"), onStartOverPressed = {})
}