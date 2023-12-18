package app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.awt.FileDialog
import java.io.File
import java.io.FilenameFilter

@Preview
@Composable
fun homeScreen() {
    var fileToBeSplit by remember { mutableStateOf("") }
    var filesToBeMerged by remember { mutableStateOf(emptySet<File>()) }
    var fileToBeLocked by remember { mutableStateOf("") }
    var fileToBeUnlocked by remember { mutableStateOf("") }
    var workflowCompleted by remember { mutableStateOf(false) }

    when {
        fileToBeSplit.isEmpty() && filesToBeMerged.isEmpty() && fileToBeLocked.isEmpty() && fileToBeUnlocked.isEmpty() -> homeScreen(
            workflowCompleted = workflowCompleted,
            fileToBeSplit = { fileToBeSplit = it },
            filesToBeMerged = { filesToBeMerged = it },
            fileToBeLocked = { fileToBeLocked = it },
            fileToBeUnlocked = { fileToBeUnlocked = it }
        )

        fileToBeSplit.isNotEmpty() -> splitScreen(
            file = File(fileToBeSplit),
            workflowCompleted = { workflowCompleted = it },
            updateFile = { fileToBeSplit = it }
        )

        filesToBeMerged.isNotEmpty() -> mergeScreen(
            files = filesToBeMerged,
            workflowCompleted = { workflowCompleted = it },
            updateFiles = { filesToBeMerged = it },
            images = emptyList(),
            mergeActions = mutableListOf()
        )

        fileToBeLocked.isNotEmpty() -> passwordScreenOnLock(
            file = File(fileToBeLocked),
            workflowCompleted = { workflowCompleted = it },
            updateFile = { fileToBeLocked = it },
        )

        fileToBeUnlocked.isNotEmpty() -> passwordScreenOnUnlock(
            file = File(fileToBeUnlocked),
            workflowCompleted = { workflowCompleted = it },
            updateFile = { fileToBeUnlocked = it }
        )
    }
}

private val ICON_SIZE = 60.dp

@Composable
fun homeScreen(
    workflowCompleted: Boolean,
    fileToBeSplit: (String) -> Unit,
    filesToBeMerged: (Set<File>) -> Unit,
    fileToBeLocked: (String) -> Unit,
    fileToBeUnlocked: (String) -> Unit,
) {
    Row(
        modifier = Modifier.background(MaterialTheme.colors.background).fillMaxSize(),
    ) {
        if (workflowCompleted) {
            Column { animatedCheckMark() }
        }
        Column(modifier = Modifier.padding(top = 120.dp, start = 270.dp)) {
            IconButton(
                onClick = {
                    FileDialog(ComposeWindow(), "Import", FileDialog.LOAD).apply {
                        filenameFilter = FilenameFilter { _, name -> name.lowercase().endsWith(".pdf") }
                        isVisible = true
                        file?.let { fileToBeSplit("$directory$file") }
                    }
                }
            ) {
                Icon(
                    modifier = Modifier.size(ICON_SIZE.plus(15.dp)).padding(start = 25.dp),
                    imageVector = Icons.Rounded.ContentCut, contentDescription = "",
                    tint = Color(0xFF226600)
                )
                Text(
                    text = "Split",
                    color = Color(0xFF226600),
                    modifier = Modifier.padding(top = 80.dp, start = 20.dp),
                    fontWeight = FontWeight.Bold,
                )
            }

            IconButton(onClick = {
                FileDialog(ComposeWindow(), "Import", FileDialog.LOAD).apply {
                    filenameFilter = FilenameFilter { _, name -> name.lowercase().endsWith(".pdf") }
                    this.isMultipleMode = true
                    isVisible = true
                    files?.let { filesToBeMerged(it.toSet()) }
                }
            }) {
                Icon(
                    modifier = Modifier.size(ICON_SIZE.minus(5.dp)),
                    imageVector = Icons.Rounded.Hub, contentDescription = "",
                    tint = Color(0xFF226600)
                )
                Text(
                    text = "Merge or Edit",
                    color = Color(0xFF226600),
                    modifier = Modifier.padding(top = 90.dp),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Column(
            modifier = Modifier.padding(start = 100.dp, top = 120.dp)
        ) {
            IconButton(
                onClick = {
                    FileDialog(ComposeWindow(), "Import", FileDialog.LOAD).apply {
                        filenameFilter = FilenameFilter { _, name -> name.lowercase().endsWith(".pdf") }
                        isVisible = true
                        file?.let { fileToBeLocked("$directory$file") }
                    }
                }
            ) {
                Icon(
                    modifier = Modifier.size(ICON_SIZE),
                    imageVector = Icons.Rounded.Lock, contentDescription = "",
                    tint = Color(0xFF226600)
                )
                Text(
                    text = "Lock",
                    color = Color(0xFF226600),
                    modifier = Modifier.padding(top = 80.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = {
                FileDialog(ComposeWindow(), "Import", FileDialog.LOAD).apply {
                    filenameFilter = FilenameFilter { _, name -> name.lowercase().endsWith(".pdf") }
                    isVisible = true
                    file?.let { fileToBeUnlocked("$directory$file") }
                }
            }) {
                Icon(
                    modifier = Modifier.size(ICON_SIZE),
                    imageVector = Icons.Rounded.LockOpen, contentDescription = "",
                    tint = Color(0xFF226600)
                )
                Text(
                    text = "Unlock",
                    color = Color(0xFF226600),
                    modifier = Modifier.padding(top = 90.dp),
                    fontWeight = FontWeight.Bold,
                )
            }

        }
    }
}

@Composable
private fun animatedCheckMark() {
    val a = Animatable(initialValue = 0f)

    LaunchedEffect(Unit) {
        delay(200)
        a.animateTo(
            targetValue = 1000f,
            animationSpec = SpringSpec(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessVeryLow
            )
        )
        delay(500)
        a.animateTo(
            targetValue = 2000f,
            animationSpec = SpringSpec(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessVeryLow
            )
        )
    }
    Row(
        modifier = Modifier.background(
            color = MaterialTheme.colors.background
        ).fillMaxHeight()
            .padding(top = 50.dp)
    ) {
        Icon(
            imageVector = Icons.Sharp.CheckCircle, contentDescription = "",
            modifier = Modifier
                .absoluteOffset(x = (-50).dp)
                .size(50.dp)
                .graphicsLayer {
                    translationX = a.value
                },
            tint = Color(0xFF226600)
        )
    }
}