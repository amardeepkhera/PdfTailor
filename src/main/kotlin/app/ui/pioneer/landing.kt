package app.ui.pioneer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCard
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Plumbing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.awt.FileDialog
import java.io.File
import java.io.FilenameFilter

@Composable
fun landing() {
    var htmls by remember { mutableStateOf(emptySet<File>()) }

    when {
        htmls.isEmpty() -> {
            landing {
                htmls = it
            }
        }

        else -> {
            LaunchedEffect(htmls) {
                convert(htmls) {
                    htmls = emptySet()
                }
            }
        }
    }
}

@Composable
fun landing(
    htmls: (Set<File>) -> Unit
) {
    Row(
        modifier = Modifier.background(MaterialTheme.colors.background).fillMaxSize(),
    ) {

        Column(modifier = Modifier.padding(top = 120.dp, start = 320.dp)) {
            IconButton(
                onClick = {
                    FileDialog(ComposeWindow(), "Import", FileDialog.LOAD).apply {
                        filenameFilter = FilenameFilter { _, name -> name.lowercase().endsWith(".html") }
                        this.isMultipleMode = true
                        isVisible = true
                        files?.let { htmls(it.toSet()) }
                    }
                }
            ) {
                Icon(
                    modifier = Modifier.size(60.dp.plus(15.dp)).padding(start = 25.dp),
                    imageVector = Icons.Rounded.AddCard, contentDescription = "",
                    tint = Color(0xFF226600)
                )
                Text(
                    text = "Create Pioneer Test",
                    color = Color(0xFF226600),
                    modifier = Modifier.padding(top = 80.dp, start = 20.dp),
                    fontWeight = FontWeight.Bold,
                )
            }


        }
    }
}